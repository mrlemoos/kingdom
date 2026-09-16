package dev.mrlemoos.kingdom.service;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.election.ProfessionVoteBias;
import dev.mrlemoos.kingdom.election.StableSeatUuid;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.model.parliament.PendingMotionSecond;
import dev.mrlemoos.kingdom.model.parliament.PreparedPublicWork;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.parliament.DivisionBloc;
import dev.mrlemoos.kingdom.parliament.DivisionTally;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import dev.mrlemoos.kingdom.parliament.SittingCalendar;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class ParliamentService {

    public static final int DEFAULT_DIVISION_WINDOW_MC_DAYS = 1;
    public static final int DEFAULT_PREMIER_QUESTIONS_INTERVAL_MC_DAYS = 7;
    public static final int DEFAULT_CONFIDENCE_COOLDOWN_MC_DAYS = 7;
    /** In-game days a referendum's polling window runs for unless closed early. */
    public static final int DEFAULT_POLLING_WINDOW_MC_DAYS = 2;
    /** The longest question the realm may be asked, in characters. */
    public static final int MAX_REFERENDUM_QUESTION_LENGTH = 120;

    private final KingdomService kingdomService;
    private final java.util.function.Supplier<Long> clockMs;
    private final AtomicLong billSequence = new AtomicLong(1);
    private ProfessionVoteBias professionVoteBias = ProfessionVoteBias.defaults();
    private WarService warService;
    private TreatyService treatyService;
    private int divisionWindowMcDays = DEFAULT_DIVISION_WINDOW_MC_DAYS;
    private int premierQuestionsIntervalMcDays = DEFAULT_PREMIER_QUESTIONS_INTERVAL_MC_DAYS;
    private int confidenceCooldownMcDays = DEFAULT_CONFIDENCE_COOLDOWN_MC_DAYS;
    private int pollingWindowMcDays = DEFAULT_POLLING_WINDOW_MC_DAYS;
    private ElectionService electionService;
    private java.util.function.ObjIntConsumer<String> villagerSeatReleaser = (kingdomId, seatIndex) -> {};
    private final Map<String, List<DivisionBloc>> lastDivisionBlocs = new java.util.HashMap<>();
    private java.util.function.LongSupplier mcDayClock = () -> 0L;
    private TerritoryResolver territoryResolver;

    public ParliamentService(KingdomService kingdomService) {
        this(kingdomService, System::currentTimeMillis);
    }

    ParliamentService(KingdomService kingdomService, java.util.function.Supplier<Long> clockMs) {
        this.kingdomService = kingdomService;
        this.clockMs = clockMs;
    }

    public void setProfessionVoteBias(ProfessionVoteBias professionVoteBias) {
        this.professionVoteBias = professionVoteBias != null ? professionVoteBias : ProfessionVoteBias.defaults();
    }

    public void setWarService(WarService warService) {
        this.warService = warService;
    }

    public void setTreatyService(TreatyService treatyService) {
        this.treatyService = treatyService;
    }

    /** Used when preparing a public-work site so the Premier cannot site it outside linked territory. */
    public void setTerritoryResolver(TerritoryResolver territoryResolver) {
        this.territoryResolver = territoryResolver;
    }

    /** Who calls the Premier election a carried motion of no confidence forces. */
    public void setElectionService(ElectionService electionService) {
        this.electionService = electionService;
    }

    /** How a dismissed villager Premier is released back to the territory it was claimed from. */
    public void setVillagerSeatReleaser(java.util.function.ObjIntConsumer<String> villagerSeatReleaser) {
        this.villagerSeatReleaser = villagerSeatReleaser != null ? villagerSeatReleaser : (kingdomId, seatIndex) -> {};
    }

    public void setConfidenceCooldownMcDays(int confidenceCooldownMcDays) {
        this.confidenceCooldownMcDays = Math.max(confidenceCooldownMcDays, 0);
    }

    /** Where the in-game day comes from when Hansard needs to date a result. */
    public void setMcDayClock(java.util.function.LongSupplier mcDayClock) {
        this.mcDayClock = mcDayClock != null ? mcDayClock : () -> 0L;
    }

    public ParliamentResult setCommons(String kingdomId, ChamberSite site) {
        return setChamber(kingdomId, site, ChamberTarget.COMMONS);
    }

    public ParliamentResult setLords(String kingdomId, ChamberSite site) {
        return setChamber(kingdomId, site, ChamberTarget.LORDS);
    }

    public ParliamentResult setSpeakerChair(String kingdomId, ChamberSite site) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Speaker's Chair site is required.");
        }
        kingdom.get().getParliamentSites().setSpeakerChair(site);
        return ParliamentResult.ok("Speaker's Chair set.");
    }

    public ParliamentResult setBar(String kingdomId, ChamberSite site) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Bar of the House site is required.");
        }
        kingdom.get().getParliamentSites().setBar(site);
        return ParliamentResult.ok("Bar of the House set.");
    }

    public ParliamentResult setRegistrar(String kingdomId, RegistrarSite site) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Registrar site is required.");
        }
        kingdom.get().getParliamentSites().setRegistrar(site);
        return ParliamentResult.ok("Registrar site set.");
    }

    public ParliamentResult setMpSeat(String kingdomId, int seatIndex, dev.mrlemoos.kingdom.model.election.MpSeatLocation location) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (seatIndex < 1 || seatIndex > 8) {
            return ParliamentResult.fail("MP seat index must be 1–8.");
        }
        if (location == null) {
            return ParliamentResult.fail("MP seat location is required.");
        }
        kingdom.get().getElectionState().setSeatLocation(seatIndex, location);
        return ParliamentResult.ok("MP seat " + seatIndex + " location set.");
    }

    public ParliamentResult prepareMint(String kingdomId, NobleRank rank, MintLocation location) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may prepare a mint location.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (location == null) {
            return ParliamentResult.fail("Mint location is required.");
        }
        kingdom.get().getParliamentState().setPreparedMint(location);
        return ParliamentResult.ok("Mint location prepared for a supply bill.");
    }

    public ParliamentResult preparePublicWork(String kingdomId, NobleRank rank, PreparedPublicWork work) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may prepare a public work.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (work == null) {
            return ParliamentResult.fail("Public work site is required.");
        }
        if (!work.estateType().isEstate()) {
            return ParliamentResult.fail("Public work type must be an estate block (beacon, conduit, or lodestone).");
        }
        if (territoryResolver != null) {
            TerritoryLocation territory = territoryResolver.resolve(
                    work.worldName(), work.x(), work.y(), work.z(), kingdomId);
            if (territory.type() != TerritoryLocation.IncomeLocation.OWN_KINGDOM) {
                return ParliamentResult.fail("Public work must be inside your kingdom's linked territory.");
            }
        }
        kingdom.get().getParliamentState().setPreparedPublicWork(work);
        return ParliamentResult.ok("Public work prepared for a supply bill.");
    }

    public ParliamentResult tableFiscal(
            String kingdomId, NobleRank rank, UUID proposerId, FiscalRates rates, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a fiscal bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (rates == null) {
            return ParliamentResult.fail("Fiscal rates are required.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.FISCAL,
                optionalTitle,
                new BillPayload.Fiscal(rates));
    }

    public ParliamentResult tableBudget(String kingdomId, NobleRank rank, UUID proposerId, double amount, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a budget bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (amount < 0) {
            return ParliamentResult.fail("Budget amount cannot be negative.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.BUDGET,
                optionalTitle,
                new BillPayload.Budget(amount));
    }

    public ParliamentResult tableFiscalForVillagerPremier(
            String kingdomId, int premierSeatIndex, FiscalRates rates, String optionalTitle) {
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (!isPremierVillagerSeat(kingdomId, premierSeatIndex)) {
            return ParliamentResult.fail("That seat is not the Premier villager.");
        }
        if (rates == null) {
            return ParliamentResult.fail("Fiscal rates are required.");
        }
        return tableBill(
                kingdomId,
                StableSeatUuid.forSeat(kingdomId, premierSeatIndex),
                BillType.FISCAL,
                optionalTitle,
                new BillPayload.Fiscal(rates));
    }

    public ParliamentResult tableBudgetForVillagerPremier(
            String kingdomId, int premierSeatIndex, double amount, String optionalTitle) {
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (!isPremierVillagerSeat(kingdomId, premierSeatIndex)) {
            return ParliamentResult.fail("That seat is not the Premier villager.");
        }
        if (amount < 0) {
            return ParliamentResult.fail("Budget amount cannot be negative.");
        }
        return tableBill(
                kingdomId,
                StableSeatUuid.forSeat(kingdomId, premierSeatIndex),
                BillType.BUDGET,
                optionalTitle,
                new BillPayload.Budget(amount));
    }

    /**
     * Puts the confidence question to the House. Tabled by a seated player MP and held on the order
     * paper until a <b>seconder</b> confirms it; the Premier may neither table nor second, and a
     * House of one seated player MP cannot put the question at all.
     */
    public ParliamentResult tableNoConfidence(
            String kingdomId, NobleRank rank, UUID proposerId, String optionalTitle) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (rank == NobleRank.PREMIER) {
            return ParliamentResult.fail("The Premier may not table a motion of no confidence.");
        }
        if (!isSeatedPlayerMp(kingdom.get(), proposerId)) {
            return ParliamentResult.fail("Only a seated Member of Parliament may table a motion of no confidence.");
        }
        if (!hasSeatedPremier(kingdomId, kingdom.get())) {
            return ParliamentResult.fail("There is no Premier for the House to withdraw its confidence from.");
        }
        if (countSeatedPlayerMps(kingdom.get()) < 2) {
            return ParliamentResult.fail(
                    "A motion of no confidence needs a seconder, and no other Member is seated.");
        }
        long today = mcDayClock.getAsLong();
        java.util.OptionalLong cooldownUntil = kingdom.get().getParliamentState().confidenceCooldownUntilMcDay();
        if (cooldownUntil.isPresent() && today < cooldownUntil.getAsLong()) {
            return ParliamentResult.fail("The confidence cooldown runs for another "
                    + (cooldownUntil.getAsLong() - today)
                    + " in-game day(s).");
        }

        ParliamentResult tabled = tableBill(
                kingdomId,
                proposerId,
                BillType.NO_CONFIDENCE,
                optionalTitle,
                new BillPayload.NoConfidence(proposerId));
        if (tabled instanceof ParliamentResult.Failure) {
            return tabled;
        }

        ParliamentState state = kingdom.get().getParliamentState();
        Bill motion = state.currentBill().orElseThrow();
        motion.setState(BillState.AWAITING_SECOND);
        state.setPendingMotionSecond(new PendingMotionSecond(motion.id(), proposerId, clockMs.get()));
        return ParliamentResult.ok("Motion of no confidence tabled. It awaits a seconder.");
    }

    /** Confirms a tabled motion so it may go to division. The seconder is never the proposer. */
    public ParliamentResult secondNoConfidence(String kingdomId, NobleRank rank, UUID seconderId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        ParliamentState state = kingdom.get().getParliamentState();
        Optional<PendingMotionSecond> pending = state.pendingMotionSecond();
        Optional<Bill> motion = state.currentBill();
        if (pending.isEmpty() || motion.isEmpty() || motion.get().state() != BillState.AWAITING_SECOND) {
            return ParliamentResult.fail("No motion of no confidence awaits a seconder.");
        }
        if (rank == NobleRank.PREMIER) {
            return ParliamentResult.fail("The Premier may not second a motion of no confidence.");
        }
        if (pending.get().proposedBy().equals(seconderId)) {
            return ParliamentResult.fail("A Member may not second their own motion.");
        }
        if (!isSeatedPlayerMp(kingdom.get(), seconderId)) {
            return ParliamentResult.fail("Only a seated Member of Parliament may second a motion.");
        }

        motion.get().setState(BillState.TABLED);
        state.clearPendingMotionSecond();
        return ParliamentResult.ok("The motion of no confidence has been seconded.");
    }

    /** The motion awaiting a seconder, for the hub to offer the House the chance to rise. */
    public Optional<PendingMotionSecond> pendingMotionSecond(String kingdomId) {
        return kingdomService.getKingdom(kingdomId)
                .map(k -> k.getParliamentState().pendingMotionSecond())
                .orElse(Optional.empty());
    }

    /** Whether this Member could put the confidence question right now. */
    public boolean canTableNoConfidence(String kingdomId, NobleRank rank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty() || rank == NobleRank.PREMIER || !isSessionOpen(kingdomId)) {
            return false;
        }
        if (kingdom.get().getParliamentState().currentBill().isPresent()) {
            return false;
        }
        if (!isSeatedPlayerMp(kingdom.get(), playerId) || !hasSeatedPremier(kingdomId, kingdom.get())) {
            return false;
        }
        if (countSeatedPlayerMps(kingdom.get()) < 2) {
            return false;
        }
        java.util.OptionalLong cooldownUntil = kingdom.get().getParliamentState().confidenceCooldownUntilMcDay();
        return cooldownUntil.isEmpty() || mcDayClock.getAsLong() >= cooldownUntil.getAsLong();
    }

    /** Whether this Member could second the motion now before the House. */
    public boolean canSecondNoConfidence(String kingdomId, NobleRank rank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty() || rank == NobleRank.PREMIER || !isSessionOpen(kingdomId)) {
            return false;
        }
        Optional<PendingMotionSecond> pending = kingdom.get().getParliamentState().pendingMotionSecond();
        if (pending.isEmpty() || pending.get().proposedBy().equals(playerId)) {
            return false;
        }
        return isSeatedPlayerMp(kingdom.get(), playerId);
    }

    public void setPollingWindowMcDays(int pollingWindowMcDays) {
        this.pollingWindowMcDays = Math.max(pollingWindowMcDays, 0);
    }

    /**
     * Puts a question to every member of the realm. The Premier or the Crown may call it; polling
     * opens at once and the referendum holds the order paper until it closes, so no other business
     * may be tabled meanwhile. It is advisory: nothing is enacted and no royal assent is sought.
     */
    public ParliamentResult callReferendum(String kingdomId, NobleRank rank, UUID callerId, String question) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        boolean crown = dev.mrlemoos.kingdom.resignation.ResignationAuthority.canResolveResignation(
                kingdomId, kingdomService, rank);
        if (rank != NobleRank.PREMIER && !crown) {
            return ParliamentResult.fail("Only the Premier or the Crown may put a question to the realm.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (question == null || question.isBlank()) {
            return ParliamentResult.fail("A referendum needs a question.");
        }
        String tidied = question.trim();
        if (tidied.length() > MAX_REFERENDUM_QUESTION_LENGTH) {
            return ParliamentResult.fail(
                    "The question may be at most " + MAX_REFERENDUM_QUESTION_LENGTH + " characters.");
        }

        ParliamentResult tabled = tableBill(
                kingdomId,
                callerId,
                BillType.REFERENDUM,
                tidied,
                new BillPayload.Referendum(tidied, callerId));
        if (tabled instanceof ParliamentResult.Failure) {
            return tabled;
        }

        Bill referendum = kingdom.get().getParliamentState().currentBill().orElseThrow();
        referendum.setState(BillState.DIVISION_OPEN);
        referendum.setDivisionClosesOnMcDay(mcDayClock.getAsLong() + pollingWindowMcDays);
        return ParliamentResult.ok("Referendum called: " + tidied + " Polling is open to every member.");
    }

    /** Whether the realm is being polled on a question right now. */
    public boolean isPollingOpen(String kingdomId) {
        Optional<Bill> referendum = currentReferendum(kingdomId);
        return referendum.isPresent() && referendum.get().state() == BillState.DIVISION_OPEN;
    }

    /** The referendum before the realm, if the order paper holds one. */
    public Optional<Bill> currentReferendum(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isPresent() && bill.get().type() == BillType.REFERENDUM) {
            return bill;
        }
        return Optional.empty();
    }

    /** The question before the realm, for the ballot and the login prompt. */
    public Optional<String> referendumQuestion(String kingdomId) {
        Optional<Bill> referendum = currentReferendum(kingdomId);
        if (referendum.isEmpty() || !(referendum.get().payload() instanceof BillPayload.Referendum payload)) {
            return Optional.empty();
        }
        return Optional.of(payload.question());
    }

    /** Everyone entitled to a ballot: the whole membership, not the eight seats. */
    public int electorate(String kingdomId) {
        return (int) kingdomService.getMembershipsView().values().stream()
                .filter(membership -> kingdomId.equals(membership.getKingdomId()))
                .count();
    }

    /**
     * Records one member's ballot. Every member weighs the same—a seat in the Commons buys no extra
     * say—and a member who votes again replaces their earlier ballot.
     */
    public ParliamentResult castBallot(String kingdomId, UUID voterId, VoteChoice choice) {
        if (choice == null) {
            return ParliamentResult.fail("Vote choice is required.");
        }
        Optional<Bill> referendum = currentReferendum(kingdomId);
        if (referendum.isEmpty() || referendum.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No referendum is open to the realm.");
        }
        if (voterId == null
                || kingdomService.getMembership(voterId)
                        .filter(membership -> kingdomId.equals(membership.getKingdomId()))
                        .isEmpty()) {
            return ParliamentResult.fail("Only members of the realm may vote in its referendum.");
        }
        referendum.get().recordVote(voterId, choice);
        return ParliamentResult.ok("Ballot recorded.");
    }

    /** Closes polling early. Only the Premier who governs may cut the realm's answer short. */
    public ParliamentResult closePolling(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may close polling early.");
        }
        Optional<Bill> referendum = currentReferendum(kingdomId);
        if (referendum.isEmpty() || referendum.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No referendum is open to the realm.");
        }
        return concludeReferendum(kingdomId, referendum.get(), mcDayClock.getAsLong());
    }

    /** Closes polling once the window has run. Empty while the realm still has time to answer. */
    public Optional<ParliamentResult> closePollingIfDue(String kingdomId, long currentMcDay) {
        Optional<Bill> referendum = currentReferendum(kingdomId);
        if (referendum.isEmpty() || referendum.get().state() != BillState.DIVISION_OPEN) {
            return Optional.empty();
        }
        if (currentMcDay < referendum.get().divisionClosesOnMcDay().orElse(currentMcDay)) {
            return Optional.empty();
        }
        return Optional.of(concludeReferendum(kingdomId, referendum.get(), currentMcDay));
    }

    /**
     * Declares the realm's answer. There is no quorum—a referendum is never void for want of
     * voters—and no Act follows: the result is proclaimed with the turnout, entered in Hansard, and
     * the order paper is freed.
     */
    private ParliamentResult concludeReferendum(String kingdomId, Bill referendum, long mcDay) {
        VoteTally tally = VoteTally.from(referendum.votesView());
        int abstained = (int) referendum.votesView().values().stream()
                .filter(choice -> choice == VoteChoice.ABSTAIN)
                .count();
        int entitled = electorate(kingdomId);
        boolean carried = tally.aye() > tally.nay();

        HansardRecord record = new HansardRecord(
                referendum.title(),
                BillType.REFERENDUM.name().toLowerCase(Locale.ROOT),
                carried,
                tally.aye(),
                tally.nay(),
                abstained,
                entitled,
                List.of(),
                mcDay);
        kingdomService.getKingdom(kingdomId)
                .ifPresent(kingdom -> kingdom.getParliamentState().addHansardRecord(record));

        referendum.setState(carried ? BillState.PASSED : BillState.FAILED);
        clearBill(kingdomId);
        return ParliamentResult.ok(describeReferendumResult(record));
    }

    /** The proclamation the realm hears: the answer, then the turnout it was given on. */
    public static String describeReferendumResult(HansardRecord record) {
        return String.format(
                Locale.UK,
                "Referendum result — %s: %s. Ayes %d, noes %d, abstentions %d. "
                        + "Turnout %.1f%% (%d of %d members entitled). The result is advisory.",
                record.title(),
                record.carried() ? "the realm answers aye" : "the realm does not answer aye",
                record.aye(),
                record.nay(),
                record.abstain(),
                record.turnout() * 100,
                record.votesCast(),
                record.electorate());
    }

    private static boolean isSeatedPlayerMp(Kingdom kingdom, UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return kingdom.getElectionState().seatIndexForPlayer(playerId).isPresent();
    }

    private static int countSeatedPlayerMps(Kingdom kingdom) {
        return (int) kingdom.getElectionState().seatsView().values().stream()
                .filter(seat -> seat.kind() == MpSeatKind.PLAYER && seat.isOccupied())
                .count();
    }

    /**
     * Decides a motion in the Commons: it never travels to the Lords and never becomes an Act. A
     * carried motion removes the Premier and calls a Premier election without proroguing Parliament;
     * a failed one starts the confidence cooldown binding the whole House.
     */
    private ParliamentResult concludeMotion(String kingdomId, Bill motion, boolean carried, long mcDay) {
        recordInHansard(kingdomId, motion, carried, mcDay);
        motion.setState(carried ? BillState.PASSED : BillState.FAILED);
        clearBill(kingdomId);

        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.ok("The motion of no confidence has been decided.");
        }
        if (!carried) {
            kingdom.get().getParliamentState().startConfidenceCooldown(mcDay + confidenceCooldownMcDays);
            return ParliamentResult.ok("The motion of no confidence has failed. The Premier remains in office.");
        }
        return ParliamentResult.ok("The motion of no confidence is carried. " + removePremier(kingdomId, kingdom.get()));
    }

    /** Strips the Premier—player or villager—and puts the office back to the House. */
    private String removePremier(String kingdomId, Kingdom kingdom) {
        var electionState = kingdom.getElectionState();
        java.util.OptionalInt villagerSeat = electionState.premierVillagerSeatIndex();
        if (villagerSeat.isPresent()) {
            int seatIndex = villagerSeat.getAsInt();
            villagerSeatReleaser.accept(kingdomId, seatIndex);
            electionState.clearPremierVillager();
            Optional<MpSeat> seat = electionState.seat(seatIndex);
            if (seat.isPresent()) {
                seat.get().clear();
            }
        }
        clearPremierTitles(kingdomId);

        if (electionService == null) {
            return "The Premier has left office.";
        }
        ElectionResult election = electionService.startPremierElection(kingdomId);
        if (election instanceof ElectionResult.Success) {
            return "The Premier has left office and a Premier election is called.";
        }
        return "The Premier has left office.";
    }

    private void clearPremierTitles(String kingdomId) {
        for (var membership : kingdomService.getMembershipsView().values()) {
            if (kingdomId.equals(membership.getKingdomId()) && membership.getRank() == NobleRank.PREMIER) {
                kingdomService.clearTitle(membership.getPlayerId());
            }
        }
    }

    /** How many in-game days a division stays open under a villager Speaker. */
    public int divisionWindowMcDays() {
        return divisionWindowMcDays;
    }

    public void setDivisionWindowMcDays(int divisionWindowMcDays) {
        this.divisionWindowMcDays = Math.max(divisionWindowMcDays, 0);
    }

    public void setPremierQuestionsIntervalMcDays(int premierQuestionsIntervalMcDays) {
        this.premierQuestionsIntervalMcDays = Math.max(premierQuestionsIntervalMcDays, 0);
    }

    /**
     * Calls Questions to the Premier when the interval has run: the session must be open and a
     * Premier—player or villager—seated. Ceremony alone; nothing is queued, tallied or recorded.
     * Empty when no window is due.
     */
    public Optional<ParliamentResult> callPremierQuestions(String kingdomId, long currentMcDay) {
        if (!needsVillagerSpeaker(kingdomId) || !isSessionOpen(kingdomId)) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty() || !hasSeatedPremier(kingdomId, kingdom.get())) {
            return Optional.empty();
        }
        ParliamentState state = kingdom.get().getParliamentState();
        java.util.OptionalLong last = state.lastPremierQuestionsMcDay();
        if (last.isPresent() && currentMcDay - last.getAsLong() < premierQuestionsIntervalMcDays) {
            return Optional.empty();
        }
        state.recordPremierQuestions(currentMcDay);
        return Optional.of(ParliamentResult.ok("The Speaker calls Questions to the Premier."));
    }

    private boolean hasSeatedPremier(String kingdomId, Kingdom kingdom) {
        return kingdomService.hasPlayerWithRank(kingdomId, NobleRank.PREMIER)
                || kingdom.getElectionState().premierVillagerSeatIndex().isPresent();
    }

    /** True when no player holds the Speakership, so a villager Speaker takes the Chair. */
    public boolean needsVillagerSpeaker(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).isPresent()
                && !kingdomService.hasPlayerWithRank(kingdomId, NobleRank.SPEAKER);
    }

    /**
     * Moves Commons business along under a villager Speaker: opens the division on the bill before the
     * House on a sitting day, then closes it once the division window has run—or at once when neither
     * player nor villager MPs are seated to vote. Empty when nothing was due.
     */
    public Optional<ParliamentResult> conductVillagerSpeakerDivision(String kingdomId, long currentMcDay) {
        return conductVillagerSpeakerDivision(kingdomId, currentMcDay, currentMcDay);
    }

    public Optional<ParliamentResult> conductVillagerSpeakerDivision(
            String kingdomId, long currentMcDay, long realmDay) {
        if (!needsVillagerSpeaker(kingdomId) || !isSessionOpen(kingdomId)) {
            return Optional.empty();
        }
        Optional<Bill> current = currentBill(kingdomId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        Bill bill = current.get();
        if (bill.type() == BillType.REFERENDUM) {
            // The realm is polling; the Chair has no business in it.
            return Optional.empty();
        }
        boolean playerMpsSeated = kingdomService.getKingdom(kingdomId)
                .map(k -> hasSeatedPlayerMps(k.getElectionState()))
                .orElse(false);
        boolean villagerMpsSeated = kingdomService.getKingdom(kingdomId)
                .map(k -> hasSeatedVillagerMps(k.getElectionState()))
                .orElse(false);

        if (bill.state() == BillState.TABLED) {
            if (!SittingCalendar.allowsDivision(realmDay, false)) {
                return Optional.empty();
            }
            bill.setState(BillState.DIVISION_OPEN);
            bill.setDivisionClosesOnMcDay(currentMcDay + divisionWindowMcDays);
            if (stillSitting(bill, playerMpsSeated, villagerMpsSeated, currentMcDay)) {
                return Optional.of(ParliamentResult.ok("The Speaker has opened a division on " + bill.title() + "."));
            }
            return Optional.of(closeVillagerSpeakerDivision(kingdomId, bill, currentMcDay));
        }

        if (bill.state() != BillState.DIVISION_OPEN
                || stillSitting(bill, playerMpsSeated, villagerMpsSeated, currentMcDay)) {
            return Optional.empty();
        }
        boolean emptyHouse = !playerMpsSeated && !villagerMpsSeated;
        if (emptyHouse && !SittingCalendar.allowsDivision(realmDay, false)) {
            // Recess with the benches away: do not decide the bill on the Speaker's nay alone.
            return Optional.empty();
        }
        return Optional.of(closeVillagerSpeakerDivision(kingdomId, bill, currentMcDay));
    }

    /** A division stays open while anyone seated has time left to vote in it. */
    private static boolean stillSitting(
            Bill bill, boolean playerMpsSeated, boolean villagerMpsSeated, long currentMcDay) {
        boolean anyoneSeated = playerMpsSeated || villagerMpsSeated;
        return anyoneSeated && currentMcDay < bill.divisionClosesOnMcDay().orElse(currentMcDay);
    }

    private static boolean hasSeatedVillagerMps(
            dev.mrlemoos.kingdom.model.election.KingdomElectionState electionState) {
        for (dev.mrlemoos.kingdom.model.election.MpSeat seat : electionState.seatsView().values()) {
            if (seat.kind() == dev.mrlemoos.kingdom.model.election.MpSeatKind.VILLAGER
                    && seat.entityId().isPresent()
                    && !seat.isRecessed()) {
                return true;
            }
        }
        return false;
    }

    private ParliamentResult closeVillagerSpeakerDivision(String kingdomId, Bill bill, long currentMcDay) {
        castVillagerMpVotes(kingdomId, bill);

        recordDivisionBlocs(kingdomId);
        VoteTally tally = VoteTally.from(bill.votesView());
        int aye = tally.aye();
        int nay = tally.nay();
        if (aye == nay) {
            // Denison's rule: an unelected Chair leaves the standing position undisturbed.
            bill.setSpeakerCastingVote(VoteChoice.NAY);
            nay++;
        }

        if (bill.type() == BillType.NO_CONFIDENCE) {
            return concludeMotion(kingdomId, bill, aye > nay, currentMcDay);
        }

        if (aye > nay) {
            recordInHansard(kingdomId, bill, true, currentMcDay);
            bill.setState(BillState.AWAITING_ASSENT);
            return ParliamentResult.ok("Bill passed the Commons and awaits royal assent.");
        }

        recordInHansard(kingdomId, bill, false, currentMcDay);
        bill.setState(BillState.FAILED);
        clearBill(kingdomId);
        return ParliamentResult.ok("Bill failed the division.");
    }

    public ParliamentResult tableSpendMint(
            String kingdomId, NobleRank rank, UUID proposerId, double cost, String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a mint supply bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        Optional<MintLocation> prepared = kingdom.get().getParliamentState().preparedMint();
        if (prepared.isEmpty()) {
            return ParliamentResult.fail("No mint location prepared. Use /kingdom parliament prepare mint at a lectern.");
        }
        if (cost < 0) {
            return ParliamentResult.fail("Mint cost cannot be negative.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.SPEND_MINT,
                optionalTitle,
                new BillPayload.SpendMint(prepared.get(), cost));
    }

    public ParliamentResult tableSpendPublicWork(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            RealmWealthRates rates,
            String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a public-work supply bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        Optional<PreparedPublicWork> prepared = kingdom.get().getParliamentState().preparedPublicWork();
        if (prepared.isEmpty()) {
            return ParliamentResult.fail(
                    "No public work prepared. Prepare a site and estate type first.");
        }
        RealmWealthRates wealthRates = rates != null ? rates : RealmWealthRates.defaults();
        PreparedPublicWork site = prepared.get();
        double cost = wealthRates.coronaValue(site.estateType());
        if (cost < 0) {
            return ParliamentResult.fail("Public work cost cannot be negative.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.SPEND_PUBLIC_WORK,
                optionalTitle,
                new BillPayload.SpendPublicWork(
                        site.estateType(), site.worldName(), site.x(), site.y(), site.z(), cost));
    }

    public ParliamentResult tableSpendStipend(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            UUID recipientId,
            double amount,
            String reason,
            String optionalTitle) {
        if (rank != NobleRank.PREMIER) {
            return ParliamentResult.fail("Only the Premier may table a supply bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (recipientId == null) {
            return ParliamentResult.fail("Recipient is required.");
        }
        if (amount <= 0) {
            return ParliamentResult.fail("Spend amount must be positive.");
        }
        String normalisedReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        return tableBill(
                kingdomId,
                proposerId,
                BillType.SPEND_STIPEND,
                optionalTitle,
                new BillPayload.SpendStipend(recipientId, amount, normalisedReason));
    }

    public ParliamentResult tableWar(
            String kingdomId,
            NobleRank rank,
            UUID proposerId,
            String targetKingdomId,
            WarAim aim,
            WarOutcome outcome,
            int musterDeadlineMcDays,
            String optionalTitle) {
        if (!RankAuthority.canTableWarAndPeace(rank)) {
            return ParliamentResult.fail("Only the King or Queen may table a war bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (warService == null) {
            return ParliamentResult.fail("War is disabled.");
        }
        if (treatyService != null && treatyService.isActive(kingdomId, targetKingdomId, TreatyKind.NON_AGGRESSION)) {
            return ParliamentResult.fail("A non-aggression treaty with that kingdom is active.");
        }
        if (aim == null || outcome == null) {
            return ParliamentResult.fail("War aim and outcome are required.");
        }
        if (musterDeadlineMcDays <= 0) {
            return ParliamentResult.fail("Muster deadline must be a positive number of days.");
        }
        boolean counterWar = warService.wasFormerDefenderAgainst(kingdomId, targetKingdomId);
        WarResult validation = counterWar
                ? warService.validateCounterWarBill(kingdomId, targetKingdomId)
                : warService.validateWarBill(kingdomId, targetKingdomId);
        if (validation instanceof WarResult.Failure failure) {
            return ParliamentResult.fail(failure.message());
        }
        String title = optionalTitle;
        if ((title == null || title.isBlank()) && counterWar) {
            title = BillTitles.defaultCounterWarTitle(Kingdom.normaliseId(kingdomId), clockMs.get());
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.WAR,
                title,
                new BillPayload.War(Kingdom.normaliseId(targetKingdomId), aim, outcome, musterDeadlineMcDays));
    }

    public boolean isCounterWarEligible(String kingdomId, String targetKingdomId) {
        return warService != null && warService.wasFormerDefenderAgainst(kingdomId, targetKingdomId);
    }

    public boolean canTableWar(String kingdomId, NobleRank rank) {
        return RankAuthority.canTableWarAndPeace(rank) && warService != null && warService.config().enabled();
    }

    /**
     * Tables a peace bill ending the kingdom's active war: hostilities cease, the levy demobilises,
     * captured chunks revert (no-op until Phase 6), and no region merge occurs — peace without
     * decisive victory carries no annexation or tribute side effect. Only tableable while war is
     * enabled and the kingdom is actually at war.
     */
    public ParliamentResult tablePeace(String kingdomId, NobleRank rank, UUID proposerId, String optionalTitle) {
        if (!RankAuthority.canTableWarAndPeace(rank)) {
            return ParliamentResult.fail("Only the King or Queen may table a peace bill.");
        }
        ParliamentResult blocked = premierActionBlocked(kingdomId);
        if (blocked != null) {
            return blocked;
        }
        if (warService == null || !warService.config().enabled()) {
            return ParliamentResult.fail("War is disabled.");
        }
        Optional<ActiveWar> activeWar = warService.activeWarFor(kingdomId);
        if (activeWar.isEmpty()) {
            return ParliamentResult.fail("Your kingdom is not at war.");
        }
        return tableBill(
                kingdomId,
                proposerId,
                BillType.PEACE,
                optionalTitle,
                new BillPayload.Peace(activeWar.get().id()));
    }

    public boolean canTablePeace(String kingdomId, NobleRank rank) {
        return canTableWar(kingdomId, rank) && warService.activeWarFor(kingdomId).isPresent();
    }

    public ParliamentResult tableTreaty(
            String kingdomId, NobleRank rank, UUID proposerId, String counterpartKingdomId, TreatyKind kind,
            boolean repeal, String optionalTitle) {
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ParliamentResult.fail("Only the King or Queen may table a treaty bill.");
        }
        if (treatyService == null) {
            return ParliamentResult.fail("Treaties are not available.");
        }
        if (kind == null) {
            return ParliamentResult.fail("Treaty kind is required.");
        }
        var validation = treatyService.validate(kingdomId, counterpartKingdomId, kind);
        if (validation instanceof dev.mrlemoos.kingdom.treaty.TreatyResult.Failure failure) {
            return ParliamentResult.fail(failure.message());
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        if (currentBill(kingdomId).isPresent()) {
            return ParliamentResult.fail("A bill is already before Parliament.");
        }
        return tableBill(
                kingdomId, proposerId, BillType.TREATY, optionalTitle,
                new BillPayload.Treaty(Kingdom.normaliseId(counterpartKingdomId), kind, repeal));
    }

    public ParliamentResult openDivision(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may open a division.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty()) {
            return ParliamentResult.fail("No bill is before the House.");
        }
        if (bill.get().state() != BillState.TABLED) {
            return ParliamentResult.fail("A division is not ready to open.");
        }
        bill.get().setState(BillState.DIVISION_OPEN);
        return ParliamentResult.ok("Division open. MPs may vote.");
    }

    public ParliamentResult castVote(String kingdomId, NobleRank rank, UUID voterId, VoteChoice choice) {
        if (rank != NobleRank.MP) {
            return ParliamentResult.fail("Only Members of Parliament may vote in a division.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        if (choice == null) {
            return ParliamentResult.fail("Vote choice is required.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }
        if (bill.get().type() == BillType.REFERENDUM) {
            return ParliamentResult.fail("A referendum is decided by ballot, not by division.");
        }
        bill.get().recordVote(voterId, choice);
        return ParliamentResult.ok("Vote recorded.");
    }

    public ParliamentResult castSpeakerVote(String kingdomId, NobleRank rank, VoteChoice choice) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may cast a casting vote.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        if (choice != VoteChoice.AYE && choice != VoteChoice.NAY) {
            return ParliamentResult.fail("Casting vote must be aye or nay.");
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }
        if (bill.get().type() == BillType.REFERENDUM) {
            return ParliamentResult.fail("A referendum is decided by ballot, not by division.");
        }
        VoteTally tally = VoteTally.from(bill.get().votesView());
        if (tally.aye() != tally.nay()) {
            return ParliamentResult.fail("Casting vote is only required when aye and nay are tied.");
        }
        bill.get().setSpeakerCastingVote(choice);
        return ParliamentResult.ok("Casting vote recorded.");
    }

    public ParliamentResult closeDivision(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.SPEAKER) {
            return ParliamentResult.fail("Only the Speaker may close a division.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.DIVISION_OPEN) {
            return ParliamentResult.fail("No division is open.");
        }
        if (bill.get().type() == BillType.REFERENDUM) {
            return ParliamentResult.fail("A referendum is decided by ballot, not by division.");
        }

        castVillagerMpVotes(kingdomId, bill.get());

        recordDivisionBlocs(kingdomId);
        VoteTally tally = VoteTally.from(bill.get().votesView());
        int aye = tally.aye();
        int nay = tally.nay();

        if (aye == nay) {
            Optional<VoteChoice> casting = bill.get().speakerCastingVote();
            if (casting.isEmpty()) {
                return ParliamentResult.fail("Division is tied. The Speaker must cast a casting vote.");
            }
            if (casting.get() == VoteChoice.AYE) {
                aye++;
            } else {
                nay++;
            }
        }

        if (bill.get().type() == BillType.NO_CONFIDENCE) {
            return concludeMotion(kingdomId, bill.get(), aye > nay, mcDayClock.getAsLong());
        }

        if (aye > nay) {
            recordInHansard(kingdomId, bill.get(), true, mcDayClock.getAsLong());
            bill.get().setState(BillState.AWAITING_ASSENT);
            return ParliamentResult.ok("Bill passed the Commons and awaits royal assent.");
        }

        recordInHansard(kingdomId, bill.get(), false, mcDayClock.getAsLong());
        bill.get().setState(BillState.FAILED);
        clearBill(kingdomId);
        return ParliamentResult.ok("Bill failed the division.");
    }

    public ParliamentResult assent(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ParliamentResult.fail("Only the King or Queen may grant royal assent.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.AWAITING_ASSENT) {
            return ParliamentResult.fail("No bill awaits royal assent.");
        }
        bill.get().setState(BillState.ASSENTED);
        return ParliamentResult.ok("Royal assent granted.");
    }

    public ParliamentResult reject(String kingdomId, NobleRank rank) {
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ParliamentResult.fail("Only the King or Queen may withhold assent.");
        }
        ParliamentResult sessionGate = sessionClosed(kingdomId);
        if (sessionGate != null) {
            return sessionGate;
        }
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty() || bill.get().state() != BillState.AWAITING_ASSENT) {
            return ParliamentResult.fail("No bill awaits royal assent.");
        }
        bill.get().setState(BillState.REJECTED);
        clearBill(kingdomId);
        return ParliamentResult.ok("Royal assent withheld. Bill rejected.");
    }

    public Optional<Bill> currentBill(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).map(k -> k.getParliamentState().currentBill()).orElse(Optional.empty());
    }

    public boolean isDivisionTied(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty()
                || bill.get().state() != BillState.DIVISION_OPEN
                || bill.get().type() == BillType.REFERENDUM) {
            return false;
        }
        VoteTally tally = VoteTally.from(bill.get().votesView());
        return tally.aye() == tally.nay();
    }

    public boolean canCloseDivision(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        if (bill.isEmpty()
                || bill.get().state() != BillState.DIVISION_OPEN
                || bill.get().type() == BillType.REFERENDUM) {
            return false;
        }
        if (isDivisionTied(kingdomId)) {
            return bill.get().speakerCastingVote().isPresent();
        }
        return true;
    }

    public Optional<AssentedActDraft> draftForAssentedBill(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        Optional<Bill> bill = kingdom.get().getParliamentState().currentBill();
        if (bill.isEmpty() || bill.get().state() != BillState.ASSENTED) {
            return Optional.empty();
        }

        Bill enacted = bill.get();
        return Optional.of(new AssentedActDraft(
                enacted.kingdomId(),
                enacted.id(),
                enacted.title(),
                enacted.type(),
                clockMs.get(),
                buildBookPages(enacted),
                enacted.votesView(),
                enacted.speakerCastingVote().orElse(null),
                enacted.payload(),
                enacted.conductProvisions()));
    }

    public void clearAssentedBill(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            kingdom.getParliamentState().clearCurrentBill();
            kingdom.getParliamentState().clearPreparedMint();
            kingdom.getParliamentState().clearPreparedPublicWork();
        });
    }

    public Optional<AssentedActDraft> consumeAssentedBill(String kingdomId) {
        Optional<AssentedActDraft> draft = draftForAssentedBill(kingdomId);
        draft.ifPresent(ignored -> clearAssentedBill(kingdomId));
        return draft;
    }

    public void commitArchivedAct(String kingdomId, AssentedActDraft draft, RegistrarSite shelf, int slot) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            AssentedAct act = new AssentedAct(
                    draft.billId(),
                    draft.title(),
                    draft.type(),
                    draft.assentedAtMs(),
                    draft.bookPages(),
                    draft.divisionVotes(),
                    draft.speakerCastingVote(),
                    shelf.worldName(),
                    shelf.blockX(),
                    shelf.blockY(),
                    shelf.blockZ(),
                    slot,
                    draft.conductProvisions());
            kingdom.getParliamentState().addAssentedAct(act);
        });
    }

    public void clearPreparedMintAfterTable(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(k -> k.getParliamentState().clearPreparedMint());
    }

    public boolean isSessionOpen(String kingdomId) {
        return kingdomService.getKingdom(kingdomId)
                .map(k -> k.getParliamentState().isSessionOpen())
                .orElse(false);
    }

    /**
     * Opens the session after the State Opening ceremony (or by royal commission when the Crown
     * cannot attend). Business is refused until this succeeds.
     */
    public ParliamentResult openSession(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        ParliamentState state = kingdom.get().getParliamentState();
        if (state.isSessionOpen()) {
            return ParliamentResult.fail("Parliament is already in session.");
        }
        state.openSession();
        return ParliamentResult.ok("Parliament is in session.");
    }

    public boolean isPremierBlockedByElection(String kingdomId) {
        return kingdomService
                .getKingdom(kingdomId)
                .map(k -> k.getElectionState().election().isActive())
                .orElse(false);
    }

    private ParliamentResult premierActionBlocked(String kingdomId) {
        if (isPremierBlockedByElection(kingdomId)) {
            return ParliamentResult.fail("The Premier cannot act while an election is in progress.");
        }
        return null;
    }

    private ParliamentResult sessionClosed(String kingdomId) {
        if (!isSessionOpen(kingdomId)) {
            return ParliamentResult.fail("Parliament is not in session.");
        }
        return null;
    }

    private void castVillagerMpVotes(String kingdomId, Bill bill) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            kingdom.getElectionState().seatsView().values().stream()
                    .filter(seat -> seat.kind() == MpSeatKind.VILLAGER)
                    .filter(seat -> seat.profession().isPresent())
                    .forEach(seat -> {
                        VoteChoice choice = professionVoteBias.resolve(
                                bill.type(), seat.profession().orElseThrow());
                        bill.recordVote(StableSeatUuid.forSeat(kingdomId, seat.index()), choice);
                    });
        });
    }

    private ParliamentResult tableBill(
            String kingdomId, UUID proposerId, BillType type, String optionalTitle, BillPayload payload) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        ParliamentResult closed = sessionClosed(kingdomId);
        if (closed != null) {
            return closed;
        }
        ParliamentState state = kingdom.get().getParliamentState();
        if (state.currentBill().isPresent()) {
            if (state.currentBill().get().type() == BillType.REFERENDUM) {
                return ParliamentResult.fail(
                        "A referendum is before the realm. No other business may be tabled until polling closes.");
            }
            return ParliamentResult.fail("A bill is already before Parliament.");
        }

        long tabledAt = clockMs.get();
        String title = BillTitles.resolve(type, kingdomId, tabledAt, optionalTitle);
        Bill bill = new Bill(
                nextBillId(kingdomId),
                kingdomId,
                type,
                title,
                BillState.TABLED,
                proposerId,
                payload,
                tabledAt);
        state.setCurrentBill(bill);

        if (type == BillType.SPEND_MINT) {
            state.clearPreparedMint();
        }
        if (type == BillType.SPEND_PUBLIC_WORK) {
            state.clearPreparedPublicWork();
        }

        return ParliamentResult.ok("Bill tabled: " + title);
    }

    private ParliamentResult setChamber(String kingdomId, ChamberSite site, ChamberTarget target) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (site == null) {
            return ParliamentResult.fail("Chamber site is required.");
        }
        switch (target) {
            case COMMONS -> kingdom.get().getParliamentSites().setCommons(site);
            case LORDS -> kingdom.get().getParliamentSites().setLords(site);
        }
        return ParliamentResult.ok(target.label + " site set.");
    }

    /** How the House stands on the business before it, grouped by party and profession bloc. */
    public List<DivisionBloc> divisionBlocs(String kingdomId) {
        Optional<Bill> bill = currentBill(kingdomId);
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (bill.isEmpty() || kingdom.isEmpty()) {
            return List.of();
        }
        return DivisionTally.tally(
                kingdomId, bill.get().votesView(), kingdom.get().getElectionState().seatsView().values());
    }

    /** The grouping of the division that last closed, kept for the result the House is told. */
    public List<DivisionBloc> lastDivisionBlocs(String kingdomId) {
        return lastDivisionBlocs.getOrDefault(kingdomId, List.of());
    }

    /**
     * Enters the result in Hansard as the division closes, so the record survives whatever befalls
     * the server before prorogation.
     */
    private void recordInHansard(String kingdomId, Bill bill, boolean carried, long mcDay) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        VoteTally tally = VoteTally.from(bill.votesView());
        int abstained = (int) bill.votesView().values().stream()
                .filter(choice -> choice == VoteChoice.ABSTAIN)
                .count();
        int seated = (int) kingdom.get().getElectionState().seatsView().values().stream()
                .filter(seat -> seat.isOccupied())
                .count();
        kingdom.get().getParliamentState().addHansardRecord(new HansardRecord(
                bill.title(),
                bill.type().name().toLowerCase(Locale.ROOT),
                carried,
                tally.aye(),
                tally.nay(),
                abstained,
                seated,
                lastDivisionBlocs(kingdomId),
                mcDay));
    }

    private void recordDivisionBlocs(String kingdomId) {
        lastDivisionBlocs.put(kingdomId, divisionBlocs(kingdomId));
    }

    private void clearBill(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(k -> {
            k.getParliamentState().clearCurrentBill();
            k.getParliamentState().clearPendingMotionSecond();
        });
    }

    private boolean isPremierVillagerSeat(String kingdomId, int seatIndex) {
        return kingdomService.getKingdom(kingdomId)
                .map(k -> k.getElectionState().isPremierVillagerSeat(seatIndex))
                .orElse(false);
    }

    private static boolean hasSeatedPlayerMps(dev.mrlemoos.kingdom.model.election.KingdomElectionState electionState) {
        return electionState.seatsView().values().stream()
                .anyMatch(seat -> seat.kind() == MpSeatKind.PLAYER && seat.isOccupied());
    }

    private String nextBillId(String kingdomId) {
        return kingdomId + "-" + billSequence.getAndIncrement();
    }

    private List<String> buildBookPages(Bill bill) {
        List<String> pages = new ArrayList<>();
        pages.add(bill.title());
        pages.add("Type: " + bill.type().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        pages.add(describePayload(bill.payload()));
        if (!bill.conductProvisions().isEmpty()) {
            pages.add("Conduct provisions:");
            for (ConductProvision provision : bill.conductProvisions()) {
                pages.add("- " + describeConduct(provision));
            }
        }
        pages.add("Division:");
        for (Map.Entry<UUID, VoteChoice> entry : bill.votesView().entrySet()) {
            pages.add(entry.getKey() + ": " + entry.getValue().name().toLowerCase(Locale.ROOT));
        }
        bill.speakerCastingVote()
                .ifPresent(choice -> pages.add("Speaker casting vote: " + choice.name().toLowerCase(Locale.ROOT)));
        return pages;
    }

    private static String describeConduct(ConductProvision provision) {
        return switch (provision.kind()) {
            case BUILD_BAN -> "build ban";
            case CURFEW -> "curfew";
            case WAR_LIMIT -> "war limit";
            case TREASON -> "treason";
            case GRAIN_THEFT -> "grain theft";
        };
    }

    private static String describePayload(BillPayload payload) {
        return switch (payload) {
            case BillPayload.Fiscal fiscal -> String.format(
                    Locale.UK,
                    "Base tax %.1f%%, foreign %.1f%%, transfer %.1f%%, cross %.1f%%, interest %.1f%%, tariff %.1f%%",
                    fiscal.rates().baseRate() * 100,
                    fiscal.rates().foreignSurcharge() * 100,
                    fiscal.rates().transferFee() * 100,
                    fiscal.rates().crossKingdomTransferFee() * 100,
                    fiscal.rates().villagerWalletInterest() * 100,
                    fiscal.rates().tariff() * 100);
            case BillPayload.Budget budget -> String.format(Locale.UK, "Budget cap %.2f Corona", budget.amount());
            case BillPayload.SpendMint mint -> String.format(
                    Locale.UK,
                    "Mint at %s %d %d %d for %.2f Corona",
                    mint.mintLocation().worldName(),
                    mint.mintLocation().x(),
                    mint.mintLocation().y(),
                    mint.mintLocation().z(),
                    mint.cost());
            case BillPayload.SpendPublicWork work -> String.format(
                    Locale.UK,
                    "Public work (%s) at %s %d %d %d for %.2f Corona",
                    work.estateType().configKey(),
                    work.worldName(),
                    work.x(),
                    work.y(),
                    work.z(),
                    work.cost());
            case BillPayload.SpendStipend stipend -> {
                String reason = stipend.reason() != null ? " — " + stipend.reason() : "";
                yield String.format(
                        Locale.UK,
                        "Stipend %.2f Corona to %s%s",
                        stipend.amount(),
                        stipend.recipientId(),
                        reason);
            }
            case BillPayload.War war -> String.format(
                    Locale.UK,
                    "War on %s, aim %s, outcome %s, muster deadline %d day(s)",
                    war.targetKingdomId(),
                    war.aim().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                    war.outcome().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                    war.musterDeadlineMcDays());
            case BillPayload.Peace peace -> "Peace ending war " + peace.warId();
            case BillPayload.Treaty treaty -> (treaty.repeal() ? "Repeal " : "Treaty with ")
                    + treaty.counterpartKingdomId() + " (" + treaty.kind().name().toLowerCase(Locale.ROOT) + ")";
            case BillPayload.NoConfidence motion -> "No confidence in the Premier, moved by " + motion.proposerId();
            case BillPayload.Referendum referendum -> "Referendum: " + referendum.question();
        };
    }

    private enum ChamberTarget {
        COMMONS("House of Commons"),
        LORDS("House of Lords");

        private final String label;

        ChamberTarget(String label) {
            this.label = label;
        }
    }

    public record AssentedActDraft(
            String kingdomId,
            String billId,
            String title,
            BillType type,
            long assentedAtMs,
            List<String> bookPages,
            Map<UUID, VoteChoice> divisionVotes,
            VoteChoice speakerCastingVote,
            BillPayload payload,
            List<ConductProvision> conductProvisions) {

        public AssentedActDraft {
            conductProvisions =
                    conductProvisions == null ? List.of() : List.copyOf(conductProvisions);
        }
    }
}
