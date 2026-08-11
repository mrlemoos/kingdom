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
import java.util.function.Function;

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
    private Function<String, Optional<UUID>> speakerVillagerResolver = kingdomId -> Optional.empty();

    public MechanicalJusticeService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeConfig config) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.config = Objects.requireNonNull(config, "config");
    }

    /** Supplies the seated villager Speaker entity id for warrant immunity checks. */
    public void setSpeakerVillagerResolver(Function<String, Optional<UUID>> speakerVillagerResolver) {
        this.speakerVillagerResolver =
                speakerVillagerResolver == null ? kingdomId -> Optional.empty() : speakerVillagerResolver;
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
        if (hasWarrantImmunity(kingdomId, suspectId)) {
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
        warrant.setApprovedBy(crownId);
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
        return findActiveForSuspect(kingdomId, suspectId).isPresent();
    }

    public Optional<Warrant> findActiveForSuspect(String kingdomId, UUID suspectId) {
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId)
                    && warrant.suspectId().equals(suspectId)
                    && warrant.status() == WarrantStatus.ACTIVE) {
                return Optional.of(warrant);
            }
        }
        return Optional.empty();
    }

    public PoliceResult markWarrantServed(String kingdomId, String warrantId) {
        Optional<Warrant> found = findById(kingdomId, warrantId);
        if (found.isEmpty()) {
            return PoliceResult.fail("Unknown warrant.");
        }
        Warrant warrant = found.get();
        if (warrant.status() != WarrantStatus.ACTIVE) {
            return PoliceResult.fail("That warrant is not active.");
        }
        warrant.setStatus(WarrantStatus.SERVED);
        return PoliceResult.ok("Warrant served.");
    }

    /**
     * Withdraws an active warrant without arrest. Caller refunds any arrest reward.
     */
    public PoliceResult cancelActiveWarrant(String kingdomId, UUID crownId, String warrantId) {
        Optional<Warrant> found = findById(kingdomId, warrantId);
        if (found.isEmpty()) {
            return PoliceResult.fail("Unknown warrant.");
        }
        Warrant warrant = found.get();
        if (warrant.status() != WarrantStatus.ACTIVE) {
            return PoliceResult.fail("That warrant is not active.");
        }
        if (!isCrownApprover(kingdomId, crownId)) {
            return PoliceResult.fail("Only the King or Queen may cancel an active warrant.");
        }
        warrant.setStatus(WarrantStatus.CANCELLED);
        return PoliceResult.ok("Warrant cancelled.");
    }

    public List<Warrant> warrantsView() {
        return List.copyOf(warrants);
    }

    public void replaceWarrants(List<Warrant> loaded) {
        warrants.clear();
        if (loaded != null) {
            warrants.addAll(loaded);
            long maxSequence = 0;
            for (Warrant warrant : loaded) {
                maxSequence = Math.max(maxSequence, parseSequence(warrant.id()));
            }
            warrantSequence.set(maxSequence + 1);
        }
    }

    public MechanicalJusticeConfig config() {
        return config;
    }

    public Optional<Warrant> findById(String kingdomId, String warrantId) {
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId) && warrant.id().equals(warrantId)) {
                return Optional.of(warrant);
            }
        }
        return Optional.empty();
    }

    private long parseSequence(String warrantId) {
        int dash = warrantId.lastIndexOf('-');
        if (dash < 0 || dash + 1 >= warrantId.length()) {
            return 0;
        }
        try {
            return Long.parseLong(warrantId.substring(dash + 1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean hasWarrantImmunity(String kingdomId, UUID suspectId) {
        if (VillagerWarrantPolicy.isImmune(suspectId, speakerVillagerResolver.apply(kingdomId))) {
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(suspectId);
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
