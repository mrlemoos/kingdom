package dev.mrlemoos.kingdom.war.squad;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Squad assignment and officer command (Phase 5, Slice 5.3): an officer who is a military
 * participant (see {@link OfficerEligibility}) may be assigned a squad of rank-and-file — pressed
 * villagers and/or crown units (see {@link SquadMember}) — capped by {@link SquadConfig}, then
 * commanded idle/follow/attack. Domain-only state machine; no Bukkit pathfinding goal is ever
 * touched here (see {@code docs/build-order.md} Slice 5.3's Bukkit row for that follow-up work).
 *
 * <p>When the officer's morale reaches {@link MoraleTier#ROUT} — checked fresh on every {@link
 * #command} call and via the explicit {@link #tickOfficerMorale} sweep — every squad that officer
 * commands routs per the war glossary's Squad rout entry: pressed villagers are released back to
 * the villager economy via the optional {@link ConscriptionService} hook, crown units are
 * destroyed via the optional {@link CrownSquadService} hook, and the squad is removed from the
 * registry outright (it does not linger in a {@link SquadState#ROUTED} state to be reassigned).
 *
 * <p>Phase 5, Slice 5.4 adds {@link #tickMoralePolicies} (and its single-squad counterpart {@link
 * #applyMoralePolicy}): a periodic sweep that consults {@link SquadMoralePolicy} for every
 * assigned squad so Shaken hesitation and Breaking scatter force a squad's state on the next tick
 * even when its officer never re-issued a command — {@link #command} and {@link
 * #tickOfficerMorale} alone only re-check morale at command time or explicitly for Rout.
 */
public final class SquadService {

    private final SquadConfig config;
    private final Predicate<UUID> officerEligibility;
    private final Function<UUID, MoraleTier> officerMoraleTrack;
    private final Supplier<UUID> idGenerator;
    private final Map<UUID, Squad> squadsById = new LinkedHashMap<>();
    private ConscriptionService conscriptionService;
    private CrownSquadService crownSquadService;

    public SquadService(
            SquadConfig config, Predicate<UUID> officerEligibility, Function<UUID, MoraleTier> officerMoraleTrack) {
        this(config, officerEligibility, officerMoraleTrack, UUID::randomUUID);
    }

    public SquadService(
            SquadConfig config,
            Predicate<UUID> officerEligibility,
            Function<UUID, MoraleTier> officerMoraleTrack,
            Supplier<UUID> idGenerator) {
        this.config = Objects.requireNonNull(config, "config");
        this.officerEligibility = Objects.requireNonNull(officerEligibility, "officerEligibility");
        this.officerMoraleTrack = Objects.requireNonNull(officerMoraleTrack, "officerMoraleTrack");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public SquadConfig config() {
        return config;
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) so a squad rout releases that squad's pressed villagers back to the villager
     * economy. Without this set, rout still removes the squad, but pressed villagers stay pressed.
     */
    public void setConscriptionService(ConscriptionService conscriptionService) {
        this.conscriptionService = conscriptionService;
    }

    /**
     * Optional hook so a squad rout destroys that squad's crown units. Without this set, rout
     * still removes the squad, but crown units stay ledgered until demobilisation.
     */
    public void setCrownSquadService(CrownSquadService crownSquadService) {
        this.crownSquadService = crownSquadService;
    }

    /**
     * Assigns a new squad of {@code members} to {@code officerId} on behalf of {@code kingdomId},
     * starting at {@link SquadState#IDLE}. Rejects when squads are disabled, {@code members} is
     * empty or over {@link SquadConfig#maxMembersPerSquad()}, the officer already commands the
     * maximum squads allowed ({@link SquadConfig#maxSquadsPerOfficer()}), the kingdom has already
     * reached its total squad cap ({@link SquadConfig#maxSquadsPerKingdom()}), the officer is not
     * currently a military participant (see {@link OfficerEligibility}), or the officer is
     * already at {@link MoraleTier#ROUT} and therefore unfit to command anything new.
     */
    public WarResult assign(String kingdomId, UUID officerId, Set<SquadMember> members) {
        Objects.requireNonNull(officerId, "officerId");
        Objects.requireNonNull(members, "members");
        if (!config.enabled()) {
            return WarResult.fail("Squads are disabled.");
        }
        if (kingdomId == null || kingdomId.isBlank()) {
            return WarResult.fail("Kingdom id is required.");
        }
        if (members.isEmpty()) {
            return WarResult.fail("A squad must have at least one member.");
        }
        if (members.size() > config.maxMembersPerSquad()) {
            return WarResult.fail("Squad size cap reached (" + config.maxMembersPerSquad() + ").");
        }
        if (!officerEligibility.test(officerId)) {
            return WarResult.fail("Only a military participant may be assigned a squad to command.");
        }
        if (officerMoraleTrack.apply(officerId) == MoraleTier.ROUT) {
            return WarResult.fail("An officer at Rout is unfit to command a squad.");
        }
        if (squadsForOfficer(officerId).size() >= config.maxSquadsPerOfficer()) {
            return WarResult.fail(
                    "That officer already commands the maximum number of squads (" + config.maxSquadsPerOfficer() + ").");
        }
        if (squadsForKingdom(kingdomId).size() >= config.maxSquadsPerKingdom()) {
            return WarResult.fail("The kingdom squad cap is full (" + config.maxSquadsPerKingdom() + ").");
        }
        UUID squadId = idGenerator.get();
        Squad squad = new Squad(squadId, kingdomId, officerId, members, SquadState.IDLE);
        squadsById.put(squadId, squad);
        return WarResult.ok("Squad assigned. Squad " + squadId + '.');
    }

    /**
     * Orders {@code squadId} to {@code desiredState} — one of {@link SquadState#IDLE}, {@link
     * SquadState#FOLLOW}, or {@link SquadState#ATTACK}. {@link SquadState#ROUTED} is never a valid
     * command target; it is only ever reached via {@link #tickOfficerMorale}. Re-checks the
     * commanding officer's morale before applying the order: if the officer has since reached
     * Rout, the squad routs on the spot (per {@link #tickOfficerMorale}) and the command fails.
     */
    public WarResult command(UUID squadId, SquadState desiredState) {
        Objects.requireNonNull(squadId, "squadId");
        Objects.requireNonNull(desiredState, "desiredState");
        if (!config.enabled()) {
            return WarResult.fail("Squads are disabled.");
        }
        if (desiredState == SquadState.ROUTED) {
            return WarResult.fail("A squad cannot be ordered into rout directly.");
        }
        Squad squad = squadsById.get(squadId);
        if (squad == null) {
            return WarResult.fail("Unknown squad.");
        }
        if (officerMoraleTrack.apply(squad.officerId()) == MoraleTier.ROUT) {
            tickOfficerMorale(squad.officerId());
            return WarResult.fail("The officer has routed; that squad no longer exists.");
        }
        squadsById.put(squadId, squad.withState(desiredState));
        return WarResult.ok("Squad ordered to " + display(desiredState) + '.');
    }

    /**
     * Explicit morale sweep for {@code officerId} (mirrors {@code MusterService#sweep}'s
     * periodic-check shape): reads their current tier from the injected morale track and, if it
     * is {@link MoraleTier#ROUT}, routs every squad they still command. Returns the number of
     * squads routed — {@code 0} if the officer is not at Rout or commands no squads.
     */
    public int tickOfficerMorale(UUID officerId) {
        Objects.requireNonNull(officerId, "officerId");
        if (officerMoraleTrack.apply(officerId) != MoraleTier.ROUT) {
            return 0;
        }
        List<Squad> toRout = new ArrayList<>();
        for (Squad squad : squadsById.values()) {
            if (squad.officerId().equals(officerId) && squad.state() != SquadState.ROUTED) {
                toRout.add(squad);
            }
        }
        for (Squad squad : toRout) {
            rout(squad);
        }
        return toRout.size();
    }

    /**
     * Phase 5, Slice 5.4 morale-policy sweep: applies {@link #applyMoralePolicy} to every
     * currently assigned squad, forcing a state change — or a rout — even for squads their
     * officer last commanded to FOLLOW or ATTACK. No-op returning {@code 0} when squads are
     * disabled. Returns how many squads changed state or routed.
     */
    public int tickMoralePolicies() {
        if (!config.enabled()) {
            return 0;
        }
        int changed = 0;
        for (Squad squad : List.copyOf(squadsById.values())) {
            if (applyMoralePolicy(squad.id())) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Applies {@link SquadMoralePolicy} to the single squad {@code squadId}: an officer at
     * {@link MoraleTier#ROUT} routs the squad exactly as {@link #tickOfficerMorale} does; an
     * officer at {@link MoraleTier#SHAKEN} or {@link MoraleTier#BREAKING} forces the squad's
     * state to {@link SquadMoralePolicy#forcedState}'s result, overriding a standing
     * FOLLOW/ATTACK order; {@link MoraleTier#STEADFAST} leaves the squad's state untouched.
     * Returns {@code false} when squads are disabled, {@code squadId} is unknown, or the
     * officer's tier leaves the squad's state as it already was.
     */
    public boolean applyMoralePolicy(UUID squadId) {
        Objects.requireNonNull(squadId, "squadId");
        if (!config.enabled()) {
            return false;
        }
        Squad squad = squadsById.get(squadId);
        if (squad == null) {
            return false;
        }
        MoraleTier officerTier = officerMoraleTrack.apply(squad.officerId());
        if (officerTier == MoraleTier.ROUT) {
            rout(squad);
            return true;
        }
        Optional<SquadState> forced = SquadMoralePolicy.forcedState(officerTier);
        if (forced.isPresent() && forced.get() != squad.state()) {
            squadsById.put(squadId, squad.withState(forced.get()));
            return true;
        }
        return false;
    }

    public Optional<Squad> find(UUID squadId) {
        return Optional.ofNullable(squadsById.get(squadId));
    }

    public List<Squad> squadsForOfficer(UUID officerId) {
        List<Squad> matches = new ArrayList<>();
        for (Squad squad : squadsById.values()) {
            if (squad.officerId().equals(officerId)) {
                matches.add(squad);
            }
        }
        return List.copyOf(matches);
    }

    public List<Squad> squadsForKingdom(String kingdomId) {
        List<Squad> matches = new ArrayList<>();
        for (Squad squad : squadsById.values()) {
            if (squad.kingdomId().equals(kingdomId)) {
                matches.add(squad);
            }
        }
        return List.copyOf(matches);
    }

    /**
     * Squad rout: pressed villagers are released back to the villager economy, crown units are
     * destroyed, and the squad is removed from the registry outright rather than left parked in
     * {@link SquadState#ROUTED}. The optional hooks are best-effort — a missing hook simply skips
     * that half of the release, matching the nullable-setter pattern used across the war domain.
     */
    private void rout(Squad squad) {
        for (SquadMember member : squad.members()) {
            if (member instanceof SquadMember.PressedVillager pressed && conscriptionService != null) {
                conscriptionService.release(pressed.id());
            } else if (member instanceof SquadMember.CrownUnit crownUnit && crownSquadService != null) {
                crownSquadService.destroyUnit(squad.kingdomId(), crownUnit.id());
            }
        }
        squadsById.remove(squad.id());
    }

    private static String display(SquadState state) {
        String lower = state.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
