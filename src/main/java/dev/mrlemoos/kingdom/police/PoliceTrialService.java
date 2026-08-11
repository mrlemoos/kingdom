package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.police.PoliceCase;
import dev.mrlemoos.kingdom.model.police.PoliceCaseStatus;
import dev.mrlemoos.kingdom.model.police.SavedSpawn;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.SuspendedAppointment;
import dev.mrlemoos.kingdom.model.police.SwornRole;
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
 * Warning records the offence only; loyalty was already dropped on Act breach (slice 1.3).
 * Prison sentences harden confinement: spawn save/restore, teleport ban, elected vacate,
 * appointed/sworn suspend-restore. Never touches the server whitelist.
 */
public final class PoliceTrialService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final MechanicalJusticeService justiceService;
    private final EconomyService economyService;
    private final ArrestRewardService arrestRewardService;
    private ElectedOfficeVacator electedOfficeVacator;
    private PrisonSpawnPort prisonSpawnPort;
    private TrialJuryService trialJuryService;
    private final AtomicLong caseSequence = new AtomicLong(1);
    private final List<PoliceCase> cases = new ArrayList<>();
    private final Map<UUID, SentenceType> lastClosedSentences = new HashMap<>();
    private final Set<UUID> teleportBlocked = new HashSet<>();
    private final Map<UUID, Integer> prisonCellByAccused = new HashMap<>();
    private final Map<UUID, PrisonConfinement> confinements = new HashMap<>();
    private final Map<UUID, SwornRole> lastRestoredSworn = new HashMap<>();

    public PoliceTrialService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            EconomyService economyService) {
        this(
                kingdomService,
                policeService,
                justiceService,
                economyService,
                new ArrestRewardService(kingdomService, justiceService, economyService),
                (kingdomId, convictId, vacatedRank) -> {},
                new NoOpPrisonSpawnPort());
    }

    public PoliceTrialService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            EconomyService economyService,
            ArrestRewardService arrestRewardService) {
        this(
                kingdomService,
                policeService,
                justiceService,
                economyService,
                arrestRewardService,
                (kingdomId, convictId, vacatedRank) -> {},
                new NoOpPrisonSpawnPort());
    }

    public PoliceTrialService(
            KingdomService kingdomService,
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            EconomyService economyService,
            ArrestRewardService arrestRewardService,
            ElectedOfficeVacator electedOfficeVacator,
            PrisonSpawnPort prisonSpawnPort) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.arrestRewardService = Objects.requireNonNull(arrestRewardService, "arrestRewardService");
        this.electedOfficeVacator = Objects.requireNonNull(electedOfficeVacator, "electedOfficeVacator");
        this.prisonSpawnPort = Objects.requireNonNull(prisonSpawnPort, "prisonSpawnPort");
    }

    /** Late-wire after {@link ElectionService} is constructed. */
    public void setElectedOfficeVacator(ElectedOfficeVacator electedOfficeVacator) {
        this.electedOfficeVacator = Objects.requireNonNull(electedOfficeVacator, "electedOfficeVacator");
    }

    public void setPrisonSpawnPort(PrisonSpawnPort prisonSpawnPort) {
        this.prisonSpawnPort = Objects.requireNonNull(prisonSpawnPort, "prisonSpawnPort");
    }

    public void setTrialJuryService(TrialJuryService trialJuryService) {
        this.trialJuryService = trialJuryService;
    }

    public ArrestRewardService arrestRewardService() {
        return arrestRewardService;
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

        arrestRewardService.payOnConstableArrest(warrant.get(), constableId);

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

    /**
     * Patrol-golem detain: same pending-trial flow, but any arrest reward refunds to the poster.
     */
    public PoliceResult arrestByPatrolGolem(String kingdomId, UUID suspectId) {
        if (kingdomService.getKingdom(kingdomId).isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
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

        arrestRewardService.refundPoster(warrant.get());

        PoliceCase policeCase = new PoliceCase(
                nextCaseId(kingdomId),
                kingdomId,
                suspectId,
                Optional.empty(),
                warrant.get().id(),
                warrant.get().actBillId(),
                System.currentTimeMillis());
        cases.add(policeCase);
        return PoliceResult.ok("Suspect detained by patrol. Pending trial opened.");
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
        if (trialJuryService != null && trialJuryService.findSession(kingdomId, accusedId).isPresent()) {
            return PoliceResult.fail("A trial jury is seated for that case. The jury must finish.");
        }
        PoliceCase policeCase = open.get();

        return switch (sentenceType) {
            case WARNING -> applyWarning(policeCase);
            case FINE -> applyFine(policeCase, fineAmount);
            case PRISON -> applyPrison(policeCase, prisonMinutes);
            case ACQUITTAL -> applyAcquittal(policeCase);
        };
    }

    /**
     * Applies a sentence without a player judge (realm-handled or jury-guilty path).
     */
    public PoliceResult sentenceAsRealm(
            String kingdomId,
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

    public boolean isUnderPrisonSentence(UUID playerId) {
        return confinements.containsKey(playerId);
    }

    /** Anyone under an active prison sentence is ineligible for Parliament. */
    public boolean isParliamentEligible(UUID playerId) {
        return !isUnderPrisonSentence(playerId);
    }

    public Optional<SavedSpawn> priorSpawn(UUID playerId) {
        PrisonConfinement confinement = confinements.get(playerId);
        if (confinement == null) {
            return Optional.empty();
        }
        return confinement.priorSpawn();
    }

    public Optional<SwornRole> lastRestoredSworn(UUID playerId) {
        return Optional.ofNullable(lastRestoredSworn.get(playerId));
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
        confinements.remove(playerId);
    }

    public PoliceResult releaseFromPrison(UUID convictId) {
        Objects.requireNonNull(convictId, "convictId");
        PrisonConfinement confinement = confinements.get(convictId);
        if (confinement == null) {
            return PoliceResult.fail("That person is not under a prison sentence.");
        }

        Optional<SavedSpawn> prior = confinement.priorSpawn();
        prisonSpawnPort.restore(convictId, prior);

        Optional<SuspendedAppointment> suspended = confinement.suspendedAppointment();
        if (suspended.isPresent() && !suspended.get().isEmpty()) {
            restoreAppointment(confinement.kingdomId(), convictId, suspended.get());
        }

        teleportBlocked.remove(convictId);
        prisonCellByAccused.remove(convictId);
        confinements.remove(convictId);
        return PoliceResult.ok("Prison sentence completed. Release effected.");
    }

    /** Releases any confinements whose real-time window has elapsed. */
    public int releaseDueSentences(long nowMs) {
        List<UUID> due = new ArrayList<>();
        for (Map.Entry<UUID, PrisonConfinement> entry : confinements.entrySet()) {
            if (entry.getValue().endsAtMs() <= nowMs) {
                due.add(entry.getKey());
            }
        }
        int released = 0;
        for (UUID convictId : due) {
            if (releaseFromPrison(convictId) instanceof PoliceResult.Success) {
                released++;
            }
        }
        return released;
    }

    private PoliceResult applyWarning(PoliceCase policeCase) {
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
        if (PrisonOfficePolicy.mayRemoveFromWhitelist()) {
            return PoliceResult.fail("Prison sentences must not remove players from the whitelist.");
        }
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
        String kingdomId = policeCase.kingdomId();

        Optional<SavedSpawn> priorSpawn = prisonSpawnPort.capture(accusedId);
        SuspendedAppointment suspended = suspendAppointedAndSworn(kingdomId, accusedId);
        vacateElectedIfNeeded(kingdomId, accusedId);

        policeCase.applySentence(SentenceType.PRISON, 0, prisonMinutes, slot);
        teleportBlocked.add(accusedId);
        prisonCellByAccused.put(accusedId, slot);
        prisonSpawnPort.confineToCell(accusedId, kingdomId, slot);
        long endsAtMs = System.currentTimeMillis() + prisonMinutes * 60_000L;
        confinements.put(
                accusedId,
                new PrisonConfinement(
                        kingdomId,
                        accusedId,
                        slot,
                        endsAtMs,
                        priorSpawn,
                        suspended.isEmpty() ? Optional.empty() : Optional.of(suspended),
                        false,
                        false));
        lastClosedSentences.put(accusedId, SentenceType.PRISON);
        return PoliceResult.ok(
                "Prison sentence of " + prisonMinutes + " minutes in cell " + slot + ".");
    }

    /**
     * Domain path for villager convicts: confine to cell, freeze economy activity, vacate office
     * seats. Wanted nametag remains players-only.
     */
    public PoliceResult applyVillagerPrison(
            PoliceCase policeCase, int prisonMinutes, boolean officeHolder) {
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
        String kingdomId = policeCase.kingdomId();

        if (officeHolder) {
            electedOfficeVacator.vacateOnPrison(kingdomId, accusedId, NobleRank.MP);
        }

        policeCase.applySentence(SentenceType.PRISON, 0, prisonMinutes, slot);
        prisonCellByAccused.put(accusedId, slot);
        long endsAtMs = System.currentTimeMillis() + prisonMinutes * 60_000L;
        confinements.put(
                accusedId,
                new PrisonConfinement(
                        kingdomId,
                        accusedId,
                        slot,
                        endsAtMs,
                        Optional.empty(),
                        Optional.empty(),
                        true,
                        true));
        lastClosedSentences.put(accusedId, SentenceType.PRISON);
        return PoliceResult.ok(
                "Villager prison sentence of " + prisonMinutes + " minutes in cell " + slot + ".");
    }

    public boolean isVillagerEconomyFrozen(UUID villagerId) {
        PrisonConfinement confinement = confinements.get(villagerId);
        return confinement != null && confinement.economyFrozen();
    }

    private void vacateElectedIfNeeded(String kingdomId, UUID accusedId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(accusedId);
        if (membership.isEmpty() || !membership.get().hasNobleTitle()) {
            return;
        }
        NobleRank rank = membership.get().getRank();
        if (!PrisonOfficePolicy.isElectedOffice(rank)) {
            return;
        }
        kingdomService.clearTitle(accusedId);
        electedOfficeVacator.vacateOnPrison(kingdomId, accusedId, rank);
    }

    private SuspendedAppointment suspendAppointedAndSworn(String kingdomId, UUID accusedId) {
        Optional<NobleRank> suspendedRank = Optional.empty();
        Optional<TitleStyle> suspendedStyle = Optional.empty();
        Optional<PlayerMembership> membership = kingdomService.getMembership(accusedId);
        if (membership.isPresent() && membership.get().hasNobleTitle()) {
            NobleRank rank = membership.get().getRank();
            if (PrisonOfficePolicy.isAppointedSuspendable(rank)) {
                suspendedRank = Optional.of(rank);
                suspendedStyle = Optional.ofNullable(membership.get().getTitleStyle());
                kingdomService.clearTitle(accusedId);
            }
        }

        Optional<SwornRole> sworn = Optional.empty();
        if (policeService.isConstable(kingdomId, accusedId)) {
            sworn = Optional.of(SwornRole.CONSTABLE);
            policeService.dismissConstable(kingdomId, NobleRank.KING, accusedId);
        } else if (policeService.isJudge(kingdomId, accusedId)) {
            sworn = Optional.of(SwornRole.JUDGE);
            policeService.dismissJudge(kingdomId, NobleRank.KING, accusedId);
        }

        return SuspendedAppointment.of(suspendedRank, suspendedStyle, sworn);
    }

    private void restoreAppointment(String kingdomId, UUID convictId, SuspendedAppointment suspended) {
        if (suspended.nobleRank().isPresent()) {
            kingdomService.assignTitle(
                    convictId,
                    suspended.nobleRank().get(),
                    suspended.titleStyle().orElse(TitleStyle.MASCULINE));
        }
        if (suspended.swornRole().isPresent()) {
            SwornRole role = suspended.swornRole().get();
            if (role == SwornRole.CONSTABLE) {
                policeService.appointConstable(kingdomId, NobleRank.KING, convictId);
            } else if (role == SwornRole.JUDGE) {
                policeService.appointJudge(kingdomId, NobleRank.KING, convictId);
            }
            lastRestoredSworn.put(convictId, role);
        }
    }

    private OptionalInt lowestUnoccupiedConfiguredCell(
            dev.mrlemoos.kingdom.model.police.KingdomPoliceState police) {
        Set<Integer> occupied = new HashSet<>(prisonCellByAccused.values());
        OptionalInt lowest = OptionalInt.empty();
        for (Integer slot : police.cellsView().keySet()) {
            if (slot == null || occupied.contains(slot)) {
                continue;
            }
            if (lowest.isEmpty() || slot.intValue() < lowest.getAsInt()) {
                lowest = OptionalInt.of(slot.intValue());
            }
        }
        return lowest;
    }

    private PoliceResult applyAcquittal(PoliceCase policeCase) {
        policeCase.applySentence(SentenceType.ACQUITTAL, 0, 0, null);
        lastClosedSentences.put(policeCase.accusedId(), SentenceType.ACQUITTAL);
        return PoliceResult.ok("Not guilty. Case closed.");
    }

    private String nextCaseId(String kingdomId) {
        return kingdomId + "-case-" + caseSequence.getAndIncrement();
    }

    /** Active prison confinement state for a convict. */
    public record PrisonConfinement(
            String kingdomId,
            UUID convictId,
            int cellSlot,
            long endsAtMs,
            Optional<SavedSpawn> priorSpawn,
            Optional<SuspendedAppointment> suspendedAppointment,
            boolean villagerConvict,
            boolean economyFrozen) {

        public PrisonConfinement {
            Objects.requireNonNull(kingdomId, "kingdomId");
            Objects.requireNonNull(convictId, "convictId");
            priorSpawn = priorSpawn == null ? Optional.empty() : priorSpawn;
            suspendedAppointment =
                    suspendedAppointment == null ? Optional.empty() : suspendedAppointment;
        }
    }

    private static final class NoOpPrisonSpawnPort implements PrisonSpawnPort {
        @Override
        public Optional<SavedSpawn> capture(UUID playerId) {
            return Optional.empty();
        }

        @Override
        public void restore(UUID playerId, Optional<SavedSpawn> prior) {
            // no-op until Bukkit port is wired
        }
    }
}
