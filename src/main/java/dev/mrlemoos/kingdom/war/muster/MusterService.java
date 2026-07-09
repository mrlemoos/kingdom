package dev.mrlemoos.kingdom.war.muster;

import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Muster call to the levy. Opening a muster for an active war snapshots every current member of
 * both belligerent kingdoms as eligible to respond. A member who answers opens/refreshes their levy
 * morale at {@link MoraleTier#STEADFAST}; a member who explicitly refuses drops straight to
 * {@link MoraleTier#SHAKEN}. A member already on-duty on the Crown's standing roster (see
 * {@link StandingRosterService}) is auto-counted as answered and need not respond separately.
 *
 * <p>Neither answer nor refusal is accepted once the war's muster deadline has passed. A dedicated
 * {@link #sweep(long)} — run once the deadline has passed, reading active wars live from
 * {@link WarService} — marks any still-unanswered eligible member as an ignored muster: Shaken on
 * the military track, plus a political Act breach (Faithful towards Doubtful, or worse on repeat
 * offence) via the optional {@link LoyaltyService} hook. This is the dual-track consequence of an
 * ignored muster. Domain-only — no Bukkit notifications.
 */
public final class MusterService {

    private final WarService warService;
    private final KingdomService kingdomService;
    private final Supplier<Long> clockMs;
    private MusterConfig config = MusterConfig.on();
    private StandingRosterService standingRosterService;
    private LoyaltyService loyaltyService;

    private final Map<String, Set<UUID>> eligibleByWar = new HashMap<>();
    private final Map<String, Map<UUID, MusterAnswer>> answersByWar = new HashMap<>();
    private final Map<UUID, MoraleTier> levyMoraleByPlayer = new HashMap<>();

    public MusterService(WarService warService, KingdomService kingdomService) {
        this(warService, kingdomService, System::currentTimeMillis);
    }

    public MusterService(WarService warService, KingdomService kingdomService, Supplier<Long> clockMs) {
        this.warService = Objects.requireNonNull(warService, "warService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    public void setConfig(MusterConfig config) {
        this.config = config != null ? config : MusterConfig.off();
    }

    public MusterConfig config() {
        return config;
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) so a member already on-duty on the Crown's standing roster is exempted from
     * re-answering the muster.
     */
    public void setStandingRosterService(StandingRosterService standingRosterService) {
        this.standingRosterService = standingRosterService;
    }

    /**
     * Optional hook so an ignored muster also records a political Act breach. Without this set,
     * an ignored muster still drops levy morale to Shaken, but no loyalty consequence is recorded.
     */
    public void setLoyaltyService(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    /**
     * Opens the muster call for an active war: every current member of both belligerent kingdoms
     * becomes eligible to answer or refuse. A member already on-duty on the Crown's standing roster
     * is auto-counted as answered.
     */
    public WarResult openMuster(String warId) {
        if (!config.enabled()) {
            return WarResult.fail("Muster is disabled.");
        }
        Optional<ActiveWar> war = findActiveWar(warId);
        if (war.isEmpty()) {
            return WarResult.fail("No such active war.");
        }
        Set<UUID> eligible = new LinkedHashSet<>();
        eligible.addAll(membersOf(war.get().attackerKingdomId()));
        eligible.addAll(membersOf(war.get().defenderKingdomId()));
        eligibleByWar.put(warId, eligible);
        Map<UUID, MusterAnswer> answers = answersByWar.computeIfAbsent(warId, id -> new LinkedHashMap<>());
        for (UUID playerId : eligible) {
            if (isOnDutyOnStandingRoster(playerId)) {
                answers.put(playerId, MusterAnswer.ANSWERED);
            }
        }
        return WarResult.ok("Muster called for war " + warId + ".");
    }

    public boolean isEligible(String warId, UUID playerId) {
        return eligibleByWar.getOrDefault(warId, Set.of()).contains(playerId);
    }

    public Optional<MusterAnswer> answerOf(String warId, UUID playerId) {
        Map<UUID, MusterAnswer> answers = answersByWar.get(warId);
        if (answers == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(answers.get(playerId));
    }

    /**
     * Answers the muster call: opens/refreshes the player's levy morale at Steadfast. Fails when
     * muster is disabled, the war is unknown, the player is not an eligible member, or the war's
     * muster deadline has already passed.
     */
    public WarResult answer(String warId, UUID playerId) {
        WarResult gate = validateCanRespond(warId, playerId);
        if (gate instanceof WarResult.Failure failure) {
            return failure;
        }
        recordAnswer(warId, playerId, MusterAnswer.ANSWERED, MoraleTier.STEADFAST);
        return WarResult.ok("Muster answered. Levy morale: Steadfast.");
    }

    /**
     * Refuses the muster call: drops the player's levy morale straight to Shaken. Fails under the
     * same gates as {@link #answer}.
     */
    public WarResult refuse(String warId, UUID playerId) {
        WarResult gate = validateCanRespond(warId, playerId);
        if (gate instanceof WarResult.Failure failure) {
            return failure;
        }
        recordAnswer(warId, playerId, MusterAnswer.REFUSED, MoraleTier.SHAKEN);
        return WarResult.ok("Muster refused. Levy morale: Shaken.");
    }

    /**
     * Sweeps every active war (read live from {@link WarService}) whose muster deadline has passed
     * at {@code nowMs}, marking any still-unanswered eligible member an ignored muster: Shaken on the
     * military track, plus a political Act breach via the optional {@link LoyaltyService} hook.
     * Idempotent — a player already answered, refused, or previously swept as ignored is left alone.
     * Returns the number of members newly marked as an ignored muster.
     */
    public int sweep(long nowMs) {
        int ignoredCount = 0;
        for (ActiveWar war : warService.activeWarsView()) {
            if (nowMs < war.musterDeadlineAtMs()) {
                continue;
            }
            Set<UUID> eligible = eligibleByWar.get(war.id());
            if (eligible == null || eligible.isEmpty()) {
                continue;
            }
            Map<UUID, MusterAnswer> answers = answersByWar.computeIfAbsent(war.id(), id -> new LinkedHashMap<>());
            for (UUID playerId : eligible) {
                if (answers.containsKey(playerId)) {
                    continue;
                }
                answers.put(playerId, MusterAnswer.IGNORED);
                levyMoraleByPlayer.put(playerId, MoraleTier.SHAKEN);
                if (loyaltyService != null) {
                    loyaltyService.recordActBreach(playerId);
                }
                ignoredCount++;
            }
        }
        return ignoredCount;
    }

    public Optional<MoraleTier> levyMoraleTier(UUID playerId) {
        return Optional.ofNullable(levyMoraleByPlayer.get(playerId));
    }

    private WarResult validateCanRespond(String warId, UUID playerId) {
        if (!config.enabled()) {
            return WarResult.fail("Muster is disabled.");
        }
        Optional<ActiveWar> war = findActiveWar(warId);
        if (war.isEmpty()) {
            return WarResult.fail("No such active war.");
        }
        if (!isEligible(warId, playerId)) {
            return WarResult.fail("Muster has not been called for that war, or you are not eligible.");
        }
        if (clockMs.get() >= war.get().musterDeadlineAtMs()) {
            return WarResult.fail("The muster deadline has passed.");
        }
        return WarResult.ok("Eligible to respond.");
    }

    private void recordAnswer(String warId, UUID playerId, MusterAnswer answer, MoraleTier moraleTier) {
        answersByWar.computeIfAbsent(warId, id -> new LinkedHashMap<>()).put(playerId, answer);
        levyMoraleByPlayer.put(playerId, moraleTier);
    }

    private boolean isOnDutyOnStandingRoster(UUID playerId) {
        return standingRosterService != null && standingRosterService.isOnDuty(playerId);
    }

    private Optional<ActiveWar> findActiveWar(String warId) {
        if (warId == null || warId.isBlank()) {
            return Optional.empty();
        }
        for (ActiveWar war : warService.activeWarsView()) {
            if (war.id().equals(warId)) {
                return Optional.of(war);
            }
        }
        return Optional.empty();
    }

    private Set<UUID> membersOf(String kingdomId) {
        Set<UUID> members = new LinkedHashSet<>();
        for (Map.Entry<UUID, PlayerMembership> entry : kingdomService.getMembershipsView().entrySet()) {
            if (kingdomId.equals(entry.getValue().getKingdomId())) {
                members.add(entry.getKey());
            }
        }
        return members;
    }
}
