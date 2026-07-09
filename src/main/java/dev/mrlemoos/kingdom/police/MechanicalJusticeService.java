package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mechanical Act-breach → warrant draft → crown approval pipeline (Police hop 3).
 * Bukkit paper/GUI delivery is a later thin layer; domain owns case state.
 */
public final class MechanicalJusticeService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final MechanicalJusticeConfig config;
    private final AtomicLong warrantSequence = new AtomicLong(1);
    private final List<Warrant> warrants = new ArrayList<>();

    public MechanicalJusticeService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeConfig config) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.config = Objects.requireNonNull(config, "config");
    }

    public PoliceResult openFromActBreach(ActBreach breach, UUID suspectId) {
        if (!config.actBreachEnabled()) {
            return PoliceResult.fail("Mechanical Act-breach warrants are disabled.");
        }
        if (breach == null || suspectId == null) {
            return PoliceResult.fail("A breach and suspect are required.");
        }
        String kingdomId = breach.jurisdictionKingdomId();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!policeService.isPoliceReady(kingdomId)) {
            return PoliceResult.fail(
                    "Police infrastructure is not ready. Configure at least one cell and a court.");
        }
        if (hasWarrantImmunity(suspectId)) {
            return PoliceResult.fail("That person has warrant immunity under kingdom police law.");
        }
        if (findPendingForSuspect(kingdomId, suspectId).isPresent()
                || hasActiveWarrant(kingdomId, suspectId)) {
            return PoliceResult.fail("A warrant for that suspect is already open.");
        }

        Warrant warrant = new Warrant(
                nextWarrantId(kingdomId),
                kingdomId,
                suspectId,
                breach.actBillId(),
                breach.provisionKind(),
                WarrantStatus.PENDING_CROWN,
                System.currentTimeMillis());
        warrants.add(warrant);
        return PoliceResult.ok("Warrant application filed pending royal approval.");
    }

    public PoliceResult approveWarrant(String kingdomId, UUID crownId, String warrantId) {
        Optional<Warrant> found = findById(kingdomId, warrantId);
        if (found.isEmpty()) {
            return PoliceResult.fail("Unknown warrant.");
        }
        Warrant warrant = found.get();
        if (warrant.status() != WarrantStatus.PENDING_CROWN) {
            return PoliceResult.fail("That warrant is not awaiting royal approval.");
        }
        if (!isCrownApprover(kingdomId, crownId)) {
            return PoliceResult.fail("Only the King or Queen may approve a warrant.");
        }
        warrant.setStatus(WarrantStatus.ACTIVE);
        return PoliceResult.ok("Warrant approved and now active.");
    }

    public PoliceResult rejectWarrant(String kingdomId, UUID crownId, String warrantId) {
        Optional<Warrant> found = findById(kingdomId, warrantId);
        if (found.isEmpty()) {
            return PoliceResult.fail("Unknown warrant.");
        }
        Warrant warrant = found.get();
        if (warrant.status() != WarrantStatus.PENDING_CROWN) {
            return PoliceResult.fail("That warrant is not awaiting royal approval.");
        }
        if (!isCrownApprover(kingdomId, crownId)) {
            return PoliceResult.fail("Only the King or Queen may reject a warrant.");
        }
        warrant.setStatus(WarrantStatus.REJECTED);
        return PoliceResult.ok("Warrant rejected.");
    }

    public Optional<Warrant> findPendingForSuspect(String kingdomId, UUID suspectId) {
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId)
                    && warrant.suspectId().equals(suspectId)
                    && warrant.status() == WarrantStatus.PENDING_CROWN) {
                return Optional.of(warrant);
            }
        }
        return Optional.empty();
    }

    public boolean hasActiveWarrant(String kingdomId, UUID suspectId) {
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId)
                    && warrant.suspectId().equals(suspectId)
                    && warrant.status() == WarrantStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }

    public List<Warrant> warrantsView() {
        return List.copyOf(warrants);
    }

    public MechanicalJusticeConfig config() {
        return config;
    }

    private Optional<Warrant> findById(String kingdomId, String warrantId) {
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId) && warrant.id().equals(warrantId)) {
                return Optional.of(warrant);
            }
        }
        return Optional.empty();
    }

    private boolean hasWarrantImmunity(UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !membership.get().hasNobleTitle()) {
            return false;
        }
        NobleRank rank = membership.get().getRank();
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }

    private boolean isCrownApprover(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty() || !membership.get().hasNobleTitle()) {
            return false;
        }
        if (!kingdomId.equals(membership.get().getKingdomId())) {
            return false;
        }
        NobleRank rank = membership.get().getRank();
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    private String nextWarrantId(String kingdomId) {
        return kingdomId + "-warrant-" + warrantSequence.getAndIncrement();
    }
}
