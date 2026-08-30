package dev.mrlemoos.kingdom.church;

import dev.mrlemoos.kingdom.city.PrisonStatusPort;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.church.FuneralRecord;
import dev.mrlemoos.kingdom.model.church.Marriage;
import dev.mrlemoos.kingdom.model.church.VillagerFuneralRecord;
import dev.mrlemoos.kingdom.model.church.KingdomChurchState;
import dev.mrlemoos.kingdom.police.PoliceAuthority;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * The church and its rites. A kingdom that has sited no church is not gated at all; once a church
 * stands and has been consecrated, the sworn priest — or the cleric villager in his place — may
 * hold mass, marry, bury and crown.
 */
public final class ChurchService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final LongSupplier realmDay;
    private final ChurchConfig config;
    /** Who has already been blessed at the mass now in session, by kingdom. */
    private final Map<String, Set<UUID>> massAttendance = new HashMap<>();
    private PrisonStatusPort prisonStatusPort;

    public ChurchService(
            KingdomService kingdomService,
            PoliceService policeService,
            PrisonStatusPort prisonStatusPort,
            LongSupplier realmDay,
            ChurchConfig config) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.prisonStatusPort = Objects.requireNonNull(prisonStatusPort, "prisonStatusPort");
        this.realmDay = Objects.requireNonNull(realmDay, "realmDay");
        this.config = Objects.requireNonNull(config, "config");
    }

    public ChurchConfig config() {
        return config;
    }

    public void setPrisonStatusPort(PrisonStatusPort port) {
        this.prisonStatusPort = Objects.requireNonNull(port, "prisonStatusPort");
    }

    // --- siting and consecration -----------------------------------------

    public Optional<ChurchSite> church(String kingdomId) {
        return churchState(kingdomId).flatMap(KingdomChurchState::church);
    }

    public boolean hasChurch(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        return state.isPresent() && state.get().hasChurch();
    }

    public boolean isConsecrated(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        return state.isPresent() && state.get().isConsecrated();
    }

    public ChurchResult setChurch(String kingdomId, NobleRank actorRank, ChurchSite site) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return ChurchResult.fail("Only the King or Queen may site a church.");
        }
        if (site == null) {
            return ChurchResult.fail("A church needs a place to stand.");
        }
        KingdomChurchState church = kingdom.get().getChurchState();
        boolean moved = church.hasChurch();
        church.setChurch(site);
        return ChurchResult.ok(moved
                ? "The church has been moved, and must be consecrated afresh."
                : "The church has been sited. It must be consecrated before any rite may be held.");
    }

    public ChurchResult clearChurch(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return ChurchResult.fail("Only the King or Queen may clear the church.");
        }
        KingdomChurchState church = kingdom.get().getChurchState();
        if (!church.hasChurch()) {
            return ChurchResult.fail("This kingdom has no church.");
        }
        church.clearChurch();
        return ChurchResult.ok("The church has been closed. No rite may be held in this realm.");
    }

    public ChurchResult consecrate(String kingdomId, Celebrant celebrant) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (celebrant == null || celebrant == Celebrant.NONE) {
            return ChurchResult.fail("Only the priest or the cleric may consecrate a church.");
        }
        KingdomChurchState church = kingdom.get().getChurchState();
        if (!church.hasChurch()) {
            return ChurchResult.fail("Site a church before consecrating one.");
        }
        if (church.isConsecrated()) {
            return ChurchResult.fail("This church is already consecrated.");
        }
        church.consecrate();
        return ChurchResult.ok("The church has been consecrated.");
    }

    // --- the priesthood ---------------------------------------------------

    public Optional<UUID> priest(String kingdomId) {
        return churchState(kingdomId).flatMap(KingdomChurchState::priestId);
    }

    public boolean isPriest(String kingdomId, UUID playerId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        return state.isPresent() && state.get().isPriest(playerId);
    }

    public ChurchResult swearPriest(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return ChurchResult.fail("Only the King or Queen may swear a priest.");
        }
        if (playerId == null || !isMember(kingdom.get().getId(), playerId)) {
            return ChurchResult.fail("That player is not a member of this kingdom.");
        }
        String resolvedId = kingdom.get().getId();
        if (policeService.isConstable(resolvedId, playerId) || policeService.isJudge(resolvedId, playerId)) {
            return ChurchResult.fail("A constable or judge cannot also serve as priest.");
        }
        KingdomChurchState church = kingdom.get().getChurchState();
        if (church.isPriest(playerId)) {
            return ChurchResult.fail("That player is already the priest.");
        }
        if (church.priestId().isPresent()) {
            return ChurchResult.fail("This kingdom already has a priest.");
        }
        church.swearPriest(playerId);
        return ChurchResult.ok("Priest sworn.");
    }

    public ChurchResult unswearPriest(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return ChurchResult.fail("Only the King or Queen may unswear a priest.");
        }
        KingdomChurchState church = kingdom.get().getChurchState();
        if (!church.isPriest(playerId)) {
            return ChurchResult.fail("That player is not the priest.");
        }
        church.unswearPriest();
        return ChurchResult.ok("Priest released from his office.");
    }

    // --- the cleric -------------------------------------------------------

    /** True when a cleric villager should be standing at this kingdom's church. */
    public boolean clericWanted(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty() || !state.get().hasChurch()) {
            return false;
        }
        Optional<UUID> priest = state.get().priestId();
        return priest.isEmpty() || prisonStatusPort.isUnderPrisonSentence(priest.get());
    }

    /**
     * Who stands at the altar to hold a rite: the cleric wherever one is wanted, otherwise the
     * sworn priest — and only when he is actually there. A realm whose priest never comes to the
     * church holds no rites, which is the Crown's cue to unswear him.
     *
     * @param priestAtChurch whether the sworn priest is standing at the church right now
     */
    public Celebrant presidingCelebrant(String kingdomId, boolean priestAtChurch) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty() || !state.get().hasChurch()) {
            return Celebrant.NONE;
        }
        if (clericWanted(kingdomId)) {
            return Celebrant.CLERIC;
        }
        return priestAtChurch ? Celebrant.PRIEST : Celebrant.NONE;
    }

    // --- mass and blessing -------------------------------------------------

    /** True when this realm's church is consecrated and its week between masses has run out. */
    public boolean massDue(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty() || !state.get().isConsecrated()) {
            return false;
        }
        Optional<Long> last = state.get().lastMassDay();
        return last.isEmpty() || realmDay.getAsLong() - last.get() >= config.massIntervalDays();
    }

    /** Mass stands open for the rest of the realm day it was called on, and no longer. */
    public boolean massInSession(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty()) {
            return false;
        }
        Optional<Long> last = state.get().lastMassDay();
        return last.isPresent() && last.get() == realmDay.getAsLong();
    }

    /** Realm days until the next mass falls due, where a consecrated church stands to hold one. */
    public Optional<Long> daysUntilMass(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty() || !state.get().isConsecrated()) {
            return Optional.empty();
        }
        Optional<Long> last = state.get().lastMassDay();
        if (last.isEmpty()) {
            return Optional.of(0L);
        }
        long due = last.get() + config.massIntervalDays() - realmDay.getAsLong();
        return Optional.of(Math.max(0L, due));
    }

    /** Calls the week's mass. The celebrant holds it; the realm is welcome to come. */
    public ChurchResult callMass(String kingdomId, Celebrant celebrant) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!massDue(kingdomId)) {
            return ChurchResult.fail("Mass has already been held this week.");
        }
        churchState(kingdomId).orElseThrow().setLastMassDay(realmDay.getAsLong());
        massAttendance.put(kingdomId, new HashSet<>());
        return ChurchResult.ok("Mass is called at the church.");
    }

    /** A subject come to the altar during mass: the blessing is laid on them, once to a mass. */
    public ChurchResult attend(String kingdomId, UUID subjectId) {
        if (!massInSession(kingdomId)) {
            return ChurchResult.fail("No mass is being held.");
        }
        if (subjectId == null || !isMember(kingdomId, subjectId)) {
            return ChurchResult.fail("Only a subject of this realm may attend mass.");
        }
        // ponytail: attendance is memory-only — a restart mid-mass lets a subject be blessed twice,
        // which is cheaper than a persisted register for a once-a-week rite.
        if (!massAttendance.computeIfAbsent(kingdomId, id -> new HashSet<>()).add(subjectId)) {
            return ChurchResult.fail("You have already attended this mass.");
        }
        return ChurchResult.ok("A blessing is laid upon you.");
    }

    // --- marriage ---------------------------------------------------------

    public Optional<UUID> spouseOf(String kingdomId, UUID playerId) {
        return churchState(kingdomId).flatMap(church -> church.spouseOf(playerId));
    }

    public boolean isMarried(String kingdomId, UUID playerId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        return state.isPresent() && state.get().isMarried(playerId);
    }

    public ChurchResult wed(String kingdomId, Celebrant celebrant, UUID first, UUID second) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (first == null || second == null || first.equals(second)) {
            return ChurchResult.fail("A marriage needs two subjects.");
        }
        if (!isMember(kingdomId, first) || !isMember(kingdomId, second)) {
            return ChurchResult.fail("Both parties must be subjects of this realm.");
        }
        KingdomChurchState church = churchState(kingdomId).orElseThrow();
        if (church.isMarried(first) || church.isMarried(second)) {
            return ChurchResult.fail("One of the parties is already wed.");
        }
        church.wed(new Marriage(first, second, System.currentTimeMillis()));
        return ChurchResult.ok("They are wed.");
    }

    public ChurchResult divorce(String kingdomId, Celebrant celebrant, UUID playerId) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        KingdomChurchState church = churchState(kingdomId).orElseThrow();
        if (!church.dissolve(playerId)) {
            return ChurchResult.fail("That subject is not wed.");
        }
        return ChurchResult.ok("The marriage is dissolved.");
    }

    /** The Crown's remedy where one spouse will not consent to a divorce. */
    public ChurchResult annul(String kingdomId, NobleRank actorRank, UUID playerId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty()) {
            return ChurchResult.fail("Unknown kingdom.");
        }
        if (!PoliceAuthority.canAppointSwornRole(actorRank)) {
            return ChurchResult.fail("Only the King or Queen may annul a marriage.");
        }
        if (!state.get().dissolve(playerId)) {
            return ChurchResult.fail("That subject is not wed.");
        }
        return ChurchResult.ok("The marriage is annulled.");
    }

    /** Ends any marriage this player holds anywhere: leaving the realm, or being moved out of it. */
    public boolean endMarriagesFor(UUID playerId) {
        boolean ended = false;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            ended |= kingdom.getChurchState().dissolve(playerId);
        }
        return ended;
    }

    // --- funerals ---------------------------------------------------------

    /** Holds a dead member's dropped experience against their funeral. */
    public void holdFuneralRecord(String kingdomId, UUID playerId, int experience) {
        churchState(kingdomId)
                .ifPresent(church ->
                        church.holdFuneralRecord(playerId, new FuneralRecord(experience, realmDay.getAsLong())));
    }

    public Optional<FuneralRecord> funeralRecord(String kingdomId, UUID playerId) {
        return churchState(kingdomId).flatMap(church -> church.funeralRecord(playerId));
    }

    public FuneralOutcome funeral(String kingdomId, Celebrant celebrant, UUID deceasedId) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return new FuneralOutcome(refusal.get(), 0);
        }
        KingdomChurchState church = churchState(kingdomId).orElseThrow();
        Optional<FuneralRecord> record = church.funeralRecord(deceasedId);
        if (record.isEmpty()) {
            return FuneralOutcome.refused("There is nothing held against that subject's name.");
        }
        if (record.get().isExpired(realmDay.getAsLong(), config.funeralWindowDays())) {
            church.takeFuneralRecord(deceasedId);
            return FuneralOutcome.refused("The rites were left too long; nothing remains to be returned.");
        }
        church.takeFuneralRecord(deceasedId);
        int returned = (int) Math.floor(record.get().heldExperience() * config.funeralExperienceShare());
        return new FuneralOutcome(ChurchResult.ok("The dead are honoured."), returned);
    }

    /** Freezes a dead productive villager's wallet balance awaiting its rites. */
    public void holdVillagerFuneralRecord(String kingdomId, UUID villagerId, double balance) {
        churchState(kingdomId)
                .ifPresent(church -> church.holdVillagerFuneralRecord(
                        villagerId, new VillagerFuneralRecord(balance, realmDay.getAsLong())));
    }

    /** The villager who has waited longest on its rites and has not yet lapsed. */
    public Optional<UUID> nextVillagerAwaitingRites(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty()) {
            return Optional.empty();
        }
        long today = realmDay.getAsLong();
        return state.get().villagerFuneralRecordsView().entrySet().stream()
                .filter(entry -> !entry.getValue().isExpired(today, config.funeralWindowDays()))
                .min(java.util.Comparator.comparingLong(entry -> entry.getValue().diedOnDay()))
                .map(Map.Entry::getKey);
    }

    public VillagerFuneralOutcome villagerFuneral(String kingdomId, Celebrant celebrant, UUID villagerId) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return new VillagerFuneralOutcome(refusal.get(), 0.0d, 0.0d, true);
        }
        KingdomChurchState church = churchState(kingdomId).orElseThrow();
        Optional<VillagerFuneralRecord> record = church.villagerFuneralRecord(villagerId);
        if (record.isEmpty()) {
            return VillagerFuneralOutcome.refused("No villager of this realm awaits its rites by that name.");
        }
        if (record.get().isExpired(realmDay.getAsLong(), config.funeralWindowDays())) {
            // Left standing for the daily sweep to escheat to the treasury; dropping it here would
            // lose the balance. It never blocks the queue: nextVillagerAwaitingRites skips it.
            return VillagerFuneralOutcome.refused("That villager's estate has already escheated to the Crown.");
        }
        church.takeVillagerFuneralRecord(villagerId);
        double held = record.get().heldBalance();
        double tithe = held * config.titheShare();
        return new VillagerFuneralOutcome(
                ChurchResult.ok("The villager is buried."), held - tithe, tithe, celebrant == Celebrant.CLERIC);
    }

    /**
     * Sweeps villagers whose rites were never held: the whole balance escheats to the treasury.
     *
     * @return the total escheated, for the caller to credit
     */
    public double escheatLapsedVillagerFunerals(String kingdomId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty()) {
            return 0.0d;
        }
        long today = realmDay.getAsLong();
        double escheated = 0.0d;
        for (Map.Entry<UUID, VillagerFuneralRecord> entry :
                state.get().villagerFuneralRecordsView().entrySet()) {
            if (entry.getValue().isExpired(today, config.funeralWindowDays())) {
                state.get().takeVillagerFuneralRecord(entry.getKey());
                escheated += entry.getValue().heldBalance();
            }
        }
        return escheated;
    }

    // --- coronation -------------------------------------------------------

    /** The gate bites only where a consecrated church stands to be crowned in. */
    public boolean coronationGateActive(String kingdomId) {
        return isConsecrated(kingdomId);
    }

    public boolean isCrowned(String kingdomId, UUID monarchId) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        return state.isPresent() && state.get().isCrowned(monarchId);
    }

    /** Royal assent, honours, swearing roles and granting titles. Nothing else is gated. */
    public boolean mayExerciseCeremonialPowers(String kingdomId, UUID playerId, NobleRank rank) {
        if (!coronationGateActive(kingdomId)) {
            return true;
        }
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return true;
        }
        return isCrowned(kingdomId, playerId);
    }

    /** The refusal to show an uncrowned monarch reaching for a ceremonial power, if any. */
    public Optional<String> ceremonialRefusal(String kingdomId, UUID playerId, NobleRank rank) {
        if (mayExerciseCeremonialPowers(kingdomId, playerId, rank)) {
            return Optional.empty();
        }
        return Optional.of("You have not been crowned. Hold a coronation at the church first.");
    }

    public ChurchResult crown(String kingdomId, Celebrant celebrant, UUID monarchId) {
        Optional<ChurchResult.Failure> refusal = riteRefusal(kingdomId, celebrant);
        if (refusal.isPresent()) {
            return refusal.get();
        }
        if (!isMember(kingdomId, monarchId)) {
            return ChurchResult.fail("Only a subject of this realm may be crowned.");
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(monarchId);
        NobleRank rank = membership.map(PlayerMembership::getRank).orElse(null);
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ChurchResult.fail("The priest crowns the rightful monarch, and no one else.");
        }
        KingdomChurchState church = churchState(kingdomId).orElseThrow();
        if (church.isCrowned(monarchId)) {
            return ChurchResult.fail("The monarch is already crowned.");
        }
        church.crown(monarchId);
        return ChurchResult.ok("The monarch is crowned.");
    }

    // --- shared rite preconditions ---------------------------------------

    /** The standing refusal for any rite: a church, consecrated, with somebody to celebrate it. */
    Optional<ChurchResult.Failure> riteRefusal(String kingdomId, Celebrant celebrant) {
        Optional<KingdomChurchState> state = churchState(kingdomId);
        if (state.isEmpty()) {
            return Optional.of(ChurchResult.fail("Unknown kingdom."));
        }
        if (!state.get().hasChurch()) {
            return Optional.of(ChurchResult.fail("This kingdom has no church."));
        }
        if (!state.get().isConsecrated()) {
            return Optional.of(ChurchResult.fail("This church has not been consecrated."));
        }
        if (celebrant == null || celebrant == Celebrant.NONE) {
            return Optional.of(ChurchResult.fail("There is nobody to hold the rite."));
        }
        return Optional.empty();
    }

    boolean isMember(String kingdomId, UUID playerId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty() || playerId == null) {
            return false;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent() && kingdom.get().getId().equals(membership.get().getKingdomId());
    }

    Optional<KingdomChurchState> churchState(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).map(Kingdom::getChurchState);
    }
}
