package dev.mrlemoos.kingdom.war.roster;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.model.war.OnDutyState;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Crown-maintained standing roster: a permanent military core on an explicit per-kingdom roster.
 * Noble titles (including Knight) never imply roster membership on their own, and only current
 * kingdom members may be rostered — until a dedicated sworn-outsider model exists, this membership
 * guard also keeps sworn outsiders off the roster. On war bill enactment, rostered members are
 * auto-mobilised to on-duty at {@link MoraleTier#STEADFAST} with hardened service.
 */
public final class StandingRosterService {

    private final KingdomService kingdomService;
    private final StandingRosterStore store;
    private final StandingRosterConfig config;

    public StandingRosterService(KingdomService kingdomService, StandingRosterStore store, StandingRosterConfig config) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
    }

    public StandingRosterConfig config() {
        return config;
    }

    public WarResult appoint(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return WarResult.fail("Unknown kingdom.");
        }
        if (!StandingRosterAuthority.isCrown(actorRank)) {
            return WarResult.fail("Only the King or Queen may appoint the standing roster.");
        }
        String normalisedKingdomId = kingdom.get().getId();
        if (!isMemberOf(normalisedKingdomId, playerId)) {
            return WarResult.fail(
                    "Only kingdom members may be appointed to the standing roster. Sworn outsiders are never rostered.");
        }
        Set<UUID> roster = new LinkedHashSet<>(store.findRoster(normalisedKingdomId));
        if (roster.contains(playerId)) {
            return WarResult.fail("That player is already on the standing roster.");
        }
        if (roster.size() >= config.rosterCap()) {
            return WarResult.fail("The standing roster is full (" + config.rosterCap() + ").");
        }
        roster.add(playerId);
        store.putRoster(normalisedKingdomId, roster);
        return WarResult.ok("Appointed to the standing roster.");
    }

    public WarResult remove(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return WarResult.fail("Unknown kingdom.");
        }
        if (!StandingRosterAuthority.isCrown(actorRank)) {
            return WarResult.fail("Only the King or Queen may remove members from the standing roster.");
        }
        String normalisedKingdomId = kingdom.get().getId();
        Set<UUID> roster = new LinkedHashSet<>(store.findRoster(normalisedKingdomId));
        if (!roster.contains(playerId)) {
            return WarResult.fail("That player is not on the standing roster.");
        }
        roster.remove(playerId);
        store.putRoster(normalisedKingdomId, roster);
        store.clearOnDutyState(playerId);
        return WarResult.ok("Removed from the standing roster.");
    }

    public Set<UUID> rosterView(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Set.of();
        }
        return store.findRoster(kingdom.get().getId());
    }

    /**
     * Called on war bill enactment (typically once per belligerent kingdom) to auto-mobilise that
     * kingdom's rostered members to on-duty Steadfast with hardened service. A non-rostered member
     * — including a Knight without explicit roster membership — is left untouched.
     */
    public void mobiliseOnWarEnactment(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        for (UUID playerId : store.findRoster(kingdom.get().getId())) {
            store.putOnDutyState(playerId, OnDutyState.steadfastHardened());
        }
    }

    public boolean isOnDuty(UUID playerId) {
        return store.findOnDutyState(playerId).isPresent();
    }

    public boolean hasHardenedService(UUID playerId) {
        Optional<OnDutyState> state = store.findOnDutyState(playerId);
        return state.isPresent() && state.get().hardenedService();
    }

    public Optional<MoraleTier> moraleTier(UUID playerId) {
        Optional<OnDutyState> state = store.findOnDutyState(playerId);
        return state.isPresent() ? Optional.of(state.get().moraleTier()) : Optional.empty();
    }

    private boolean isMemberOf(String normalisedKingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent() && normalisedKingdomId.equals(membership.get().getKingdomId());
    }
}
