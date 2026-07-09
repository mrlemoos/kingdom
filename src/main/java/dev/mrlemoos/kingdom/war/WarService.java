package dev.mrlemoos.kingdom.war;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Validates and enacts war bills, and tracks active wars. At most one active war per kingdom pair;
 * an attacker already a belligerent in any active war cannot table a second war bill.
 */
public final class WarService {

    private final KingdomService kingdomService;
    private final Supplier<Long> clockMs;
    private final Map<String, ActiveWar> activeWars = new HashMap<>();
    private final List<ActiveWar> endedWars = new ArrayList<>();
    private final AtomicLong warSequence = new AtomicLong(1);
    private WarConfig config = WarConfig.off();
    private StandingRosterService standingRosterService;

    public WarService(KingdomService kingdomService) {
        this(kingdomService, System::currentTimeMillis);
    }

    public WarService(KingdomService kingdomService, Supplier<Long> clockMs) {
        this.kingdomService = kingdomService;
        this.clockMs = clockMs;
    }

    public void setConfig(WarConfig config) {
        this.config = config != null ? config : WarConfig.off();
    }

    public WarConfig config() {
        return config;
    }

    /**
     * Optional hook (nullable setter, mirrors the loyaltyStore pattern) so {@link #enactWarBill}
     * can auto-mobilise each belligerent's standing roster without a hard compile-time coupling.
     */
    public void setStandingRosterService(StandingRosterService standingRosterService) {
        this.standingRosterService = standingRosterService;
    }

    public Collection<ActiveWar> activeWarsView() {
        return List.copyOf(activeWars.values());
    }

    public Optional<ActiveWar> activeWarFor(String kingdomId) {
        if (kingdomId == null) {
            return Optional.empty();
        }
        String normalised = Kingdom.normaliseId(kingdomId);
        for (ActiveWar war : activeWars.values()) {
            if (war.involves(normalised)) {
                return Optional.of(war);
            }
        }
        return Optional.empty();
    }

    public boolean isAtWar(String kingdomId) {
        return activeWarFor(kingdomId).isPresent();
    }

    /**
     * Battlefield treason is only possible while the kingdom is a belligerent in an active war. Query
     * only — under open PvP no damage is cancelled or gated by this flag.
     */
    public boolean battlefieldTreasonPossible(String kingdomId) {
        return isAtWar(kingdomId);
    }

    /**
     * Cross-kingdom validation for a war bill: master flag, target sanity, self-target, and no second
     * active war for the attacker. Coalitions are rejected structurally — {@code targetKingdomId} names
     * exactly one defender, so a comma-joined or otherwise multi-target string simply fails as an
     * unknown kingdom.
     */
    public WarResult validateWarBill(String attackerKingdomId, String targetKingdomId) {
        if (!config.enabled()) {
            return WarResult.fail("War is disabled.");
        }
        if (targetKingdomId == null || targetKingdomId.isBlank()) {
            return WarResult.fail("A target kingdom is required.");
        }
        String normalisedAttacker = Kingdom.normaliseId(attackerKingdomId);
        String normalisedTarget = Kingdom.normaliseId(targetKingdomId);
        if (normalisedTarget.equals(normalisedAttacker)) {
            return WarResult.fail("A kingdom cannot declare war on itself.");
        }
        if (kingdomService.getKingdom(normalisedTarget).isEmpty()) {
            return WarResult.fail("Unknown target kingdom.");
        }
        if (isAtWar(normalisedAttacker)) {
            return WarResult.fail("Your kingdom is already at war.");
        }
        return WarResult.ok("War bill valid.");
    }

    public WarResult enactWarBill(String attackerKingdomId, BillPayload.War payload) {
        if (payload == null) {
            return WarResult.fail("War bill payload is required.");
        }
        WarResult validation = validateWarBill(attackerKingdomId, payload.targetKingdomId());
        if (validation instanceof WarResult.Failure failure) {
            return failure;
        }
        String normalisedAttacker = Kingdom.normaliseId(attackerKingdomId);
        String normalisedTarget = Kingdom.normaliseId(payload.targetKingdomId());
        long startedAt = clockMs.get();
        long musterDeadlineAt = startedAt + (payload.musterDeadlineMcDays() * config.msPerMcDay());
        ActiveWar war = new ActiveWar(
                nextWarId(),
                normalisedAttacker,
                normalisedTarget,
                payload.aim(),
                payload.outcome(),
                startedAt,
                musterDeadlineAt);
        activeWars.put(war.id(), war);
        if (standingRosterService != null) {
            standingRosterService.mobiliseOnWarEnactment(normalisedAttacker);
            standingRosterService.mobiliseOnWarEnactment(normalisedTarget);
        }
        return WarResult.ok("War declared: " + normalisedAttacker + " against " + normalisedTarget + ".");
    }

    /**
     * Ends an active war (the peace path domain), clearing the at-war state for both belligerents and
     * recording the war in history for future counter-war eligibility. Fails when no active war exists
     * with the given id.
     */
    public WarResult endWar(String warId) {
        if (warId == null || warId.isBlank()) {
            return WarResult.fail("No such active war.");
        }
        ActiveWar war = activeWars.remove(warId);
        if (war == null) {
            return WarResult.fail("No such active war.");
        }
        endedWars.add(war);
        return WarResult.ok(
                "Peace declared between " + war.attackerKingdomId() + " and " + war.defenderKingdomId() + ".");
    }

    /**
     * Validates a counter-war bill: a former defender may table a new war bill against the former
     * attacker's homeland, per the counter-war rule. Applies the same base checks as a war bill (master
     * flag, target sanity, no second concurrent war for the proposer) plus a prior-defender-role check
     * against the named target.
     */
    public WarResult validateCounterWarBill(String proposerKingdomId, String targetKingdomId) {
        WarResult baseValidation = validateWarBill(proposerKingdomId, targetKingdomId);
        if (baseValidation instanceof WarResult.Failure failure) {
            return failure;
        }
        String normalisedProposer = Kingdom.normaliseId(proposerKingdomId);
        String normalisedTarget = Kingdom.normaliseId(targetKingdomId);
        if (!wasDefenderAgainst(normalisedProposer, normalisedTarget)) {
            return WarResult.fail(
                    "A counter-war may only be tabled by a former defender against that former attacker.");
        }
        return WarResult.ok("Counter-war bill valid.");
    }

    /**
     * A declaration-of-war message suitable for broadcast (British spelling), naming both belligerents
     * and the war's aim and outcome.
     */
    public String declarationMessage(ActiveWar war) {
        return war.attackerKingdomId()
                + " has declared war on "
                + war.defenderKingdomId()
                + ". Aim: "
                + war.aim().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + ". Outcome: "
                + war.outcome().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + ".";
    }

    private boolean wasDefenderAgainst(String defenderKingdomId, String attackerKingdomId) {
        for (ActiveWar war : endedWars) {
            if (war.defenderKingdomId().equals(defenderKingdomId)
                    && war.attackerKingdomId().equals(attackerKingdomId)) {
                return true;
            }
        }
        return false;
    }

    public void replaceActiveWars(Collection<ActiveWar> wars) {
        activeWars.clear();
        if (wars != null) {
            for (ActiveWar war : wars) {
                activeWars.put(war.id(), war);
            }
        }
    }

    private String nextWarId() {
        return "war-" + warSequence.getAndIncrement();
    }
}
