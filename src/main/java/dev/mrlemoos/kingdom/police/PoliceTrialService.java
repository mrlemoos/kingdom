package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.police.PoliceCase;
import dev.mrlemoos.kingdom.model.police.PoliceCaseStatus;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Arrest → pending trial → sentence pipeline for Police hop 3.
 * Warning does not drop loyalty beyond the Act-breach drop already applied in slice 1.3.
 */
public final class PoliceTrialService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final MechanicalJusticeService justiceService;
    private final LoyaltyService loyaltyService;
    private final EconomyService economyService;
    private final AtomicLong caseSequence = new AtomicLong(1);
    private final List<PoliceCase> cases = new ArrayList<>();
    private final Map<UUID, SentenceType> lastClosedSentences = new HashMap<>();
    private final Set<UUID> teleportBlocked = new HashSet<>();
    private final Map<UUID, Integer> prisonCellByAccused = new HashMap<>();

    public PoliceTrialService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            LoyaltyService loyaltyService,
            EconomyService economyService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
    }

    public PoliceResult arrest(String kingdomId, UUID constableId, UUID suspectId) {
        if (kingdomService.getKingdom(kingdomId).isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        if (!policeService.isConstable(kingdomId, constableId)) {
            return PoliceResult.fail("Only a constable may arrest.");
        }
        if (!policeService.isPoliceReady(kingdomId)) {
            return PoliceResult.fail(
                    "Police infrastructure is not ready. Configure at least one cell and a court.");
        }
        if (findOpenCase(kingdomId, suspectId).isPresent()) {
            return PoliceResult.fail("That suspect already has a pending trial.");
        }
        Optional<Warrant> warrant = justiceService.findActiveForSuspect(kingdomId, suspectId);
        if (warrant.isEmpty()) {
            return PoliceResult.fail("No active warrant for that suspect.");
        }

        PoliceResult served = justiceService.markWarrantServed(kingdomId, warrant.get().id());
        if (served instanceof PoliceResult.Failure) {
            return served;
        }

        PoliceCase policeCase = new PoliceCase(
                nextCaseId(kingdomId),
                kingdomId,
                suspectId,
                constableId,
                warrant.get().id(),
                warrant.get().actBillId(),
                System.currentTimeMillis());
        cases.add(policeCase);
        return PoliceResult.ok("Suspect arrested. Pending trial opened.");
    }

    public PoliceResult sentence(
            String kingdomId,
            UUID judgeId,
            UUID accusedId,
            SentenceType sentenceType,
            double fineAmount,
            int prisonMinutes) {
        if (sentenceType == null) {
            return PoliceResult.fail("A sentence type is required.");
        }
        Optional<PoliceCase> open = findOpenCase(kingdomId, accusedId);
        if (open.isEmpty()) {
            return PoliceResult.fail("No pending trial for that accused.");
        }
        if (!policeService.isJudge(kingdomId, judgeId)) {
            return PoliceResult.fail("Only a judge may pass sentence.");
        }
        PoliceCase policeCase = open.get();

        return switch (sentenceType) {
            case WARNING -> applyWarning(policeCase);
            case FINE -> applyFine(policeCase, fineAmount);
            case PRISON -> applyPrison(policeCase, prisonMinutes);
            case ACQUITTAL -> applyAcquittal(policeCase);
        };
    }

    public Optional<PoliceCase> findOpenCase(String kingdomId, UUID accusedId) {
        for (PoliceCase policeCase : cases) {
            if (policeCase.kingdomId().equals(kingdomId)
                    && policeCase.accusedId().equals(accusedId)
                    && policeCase.status() == PoliceCaseStatus.PENDING_TRIAL) {
                return Optional.of(policeCase);
            }
        }
        return Optional.empty();
    }

    public Optional<SentenceType> lastClosedSentence(String kingdomId, UUID accusedId) {
        // Kingdom scoped by last closed case for that accused in this kingdom.
        for (int index = cases.size() - 1; index >= 0; index--) {
            PoliceCase policeCase = cases.get(index);
            if (policeCase.kingdomId().equals(kingdomId)
                    && policeCase.accusedId().equals(accusedId)
                    && policeCase.status() != PoliceCaseStatus.PENDING_TRIAL) {
                return policeCase.sentenceType();
            }
        }
        return Optional.ofNullable(lastClosedSentences.get(accusedId));
    }

    public boolean isKingdomTeleportBlocked(UUID playerId) {
        return teleportBlocked.contains(playerId);
    }

    public OptionalInt assignedCellSlot(String kingdomId, UUID accusedId) {
        Integer slot = prisonCellByAccused.get(accusedId);
        if (slot == null) {
            return OptionalInt.empty();
        }
        Optional<PoliceCase> open = findOpenCase(kingdomId, accusedId);
        if (open.isPresent()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(slot);
    }

    public void clearPrisonBlock(UUID playerId) {
        teleportBlocked.remove(playerId);
        prisonCellByAccused.remove(playerId);
    }

    private PoliceResult applyWarning(PoliceCase policeCase) {
        // Loyalty already dropped on Act breach (slice 1.3); warning records only.
        policeCase.applySentence(SentenceType.WARNING, 0, 0, null);
        lastClosedSentences.put(policeCase.accusedId(), SentenceType.WARNING);
        return PoliceResult.ok("Warning recorded. Offence noted without further penalty.");
    }

    private PoliceResult applyFine(PoliceCase policeCase, double fineAmount) {
        if (fineAmount <= 0) {
            return PoliceResult.fail("Fine amount must be positive.");
        }
        UUID accusedId = policeCase.accusedId();
        if (!economyService.debitWallet(accusedId, fineAmount)) {
            return PoliceResult.fail("Accused has insufficient Corona to pay the fine.");
        }
        economyService.creditTreasury(policeCase.kingdomId(), fineAmount);
        policeCase.applySentence(SentenceType.FINE, fineAmount, 0, null);
        lastClosedSentences.put(accusedId, SentenceType.FINE);
        return PoliceResult.ok("Fine of " + fineAmount + " Corona levied to the treasury.");
    }

    private PoliceResult applyPrison(PoliceCase policeCase, int prisonMinutes) {
        if (prisonMinutes <= 0) {
            return PoliceResult.fail("Prison sentence requires a positive duration in minutes.");
        }
        Optional<dev.mrlemoos.kingdom.model.Kingdom> kingdom =
                kingdomService.getKingdom(policeCase.kingdomId());
        if (kingdom.isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        OptionalInt freeSlot = lowestUnoccupiedConfiguredCell(kingdom.get().getPoliceState());
        if (freeSlot.isEmpty()) {
            return PoliceResult.fail("No free prison cell is available.");
        }
        int slot = freeSlot.getAsInt();
        UUID accusedId = policeCase.accusedId();
        policeCase.applySentence(SentenceType.PRISON, 0, prisonMinutes, slot);
        teleportBlocked.add(accusedId);
        prisonCellByAccused.put(accusedId, slot);
        lastClosedSentences.put(accusedId, SentenceType.PRISON);
        return PoliceResult.ok(
                "Prison sentence of " + prisonMinutes + " minutes in cell " + slot + ".");
    }

    private OptionalInt lowestUnoccupiedConfiguredCell(
            dev.mrlemoos.kingdom.model.police.KingdomPoliceState police) {
        Set<Integer> occupied = new HashSet<>(prisonCellByAccused.values());
        int max = policeService.config().maxCells();
        for (int slot = 1; slot <= max; slot++) {
            if (police.cell(slot).isPresent() && !occupied.contains(slot)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    private PoliceResult applyAcquittal(PoliceCase policeCase) {
        policeCase.applySentence(SentenceType.ACQUITTAL, 0, 0, null);
        lastClosedSentences.put(policeCase.accusedId(), SentenceType.ACQUITTAL);
        return PoliceResult.ok("Not guilty. Case closed.");
    }

    private String nextCaseId(String kingdomId) {
        return kingdomId + "-case-" + caseSequence.getAndIncrement();
    }
}
