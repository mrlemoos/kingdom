package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.PoliceCase;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Seats a trial jury of three when no eligible player Judge is available; majority decides
 * guilt, with guilty drawing from {@link RealmHandledSentenceTable}. Timeout or &lt;3 eligible
 * falls back to a realm-handled trial. Does not replace the player Judge path.
 */
public final class TrialJuryService {

    @FunctionalInterface
    public interface VillagerJurorProvider {
        Set<UUID> claimJurors(String kingdomId, UUID accusedId, int count);
    }

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final PoliceTrialService trialService;
    private final MechanicalJusticeService justiceService;
    private final TrialJuryConfig config;
    private final Random random;
    private final Map<String, TrialJurySession> sessionsByCaseId = new HashMap<>();
    private VillagerJurorProvider villagerJurorProvider;

    public TrialJuryService(
            KingdomService kingdomService,
            PoliceService policeService,
            PoliceTrialService trialService,
            TrialJuryConfig config,
            Random random) {
        this(
                kingdomService,
                policeService,
                trialService,
                null,
                config,
                random);
    }

    public TrialJuryService(
            KingdomService kingdomService,
            PoliceService policeService,
            PoliceTrialService trialService,
            MechanicalJusticeService justiceService,
            TrialJuryConfig config,
            Random random) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.justiceService = justiceService;
        this.config = Objects.requireNonNull(config, "config");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void setVillagerJurorProvider(VillagerJurorProvider villagerJurorProvider) {
        this.villagerJurorProvider = villagerJurorProvider;
    }

    public PoliceResult trySeatJury(String kingdomId, UUID accusedId, Set<UUID> onlineMemberIds) {
        if (kingdomService.getKingdom(kingdomId).isEmpty()) {
            return PoliceResult.fail("Unknown kingdom.");
        }
        Optional<PoliceCase> open = trialService.findOpenCase(kingdomId, accusedId);
        if (open.isEmpty()) {
            return PoliceResult.fail("No pending trial for that accused.");
        }
        PoliceCase policeCase = open.get();
        if (findSession(kingdomId, accusedId).isPresent()) {
            return PoliceResult.fail("A trial jury is already seated for that case.");
        }

        Set<UUID> exclusions = buildExclusions(policeCase);
        if (hasEligibleOnlineJudge(kingdomId, onlineMemberIds, exclusions)) {
            return PoliceResult.fail("An eligible player Judge is available to hear this case.");
        }

        List<UUID> pool = eligiblePool(kingdomId, onlineMemberIds, exclusions);
        if (pool.size() >= 3) {
            Collections.shuffle(pool, random);
            Set<UUID> jurors = new HashSet<>(pool.subList(0, 3));
            return seatSession(policeCase, jurors, false);
        }

        if (villagerJurorProvider != null) {
            Set<UUID> villagers = villagerJurorProvider.claimJurors(kingdomId, accusedId, 3);
            if (villagers != null && villagers.size() >= 3) {
                return seatSession(policeCase, new HashSet<>(villagers), true);
            }
        }

        return applyRealmHandled(policeCase, "Fewer than three eligible jurors online. Realm-handled trial conducted.");
    }

    public PoliceResult trySeatVillagerJury(String kingdomId, UUID accusedId, Set<UUID> villagerJurorIds) {
        Optional<PoliceCase> open = trialService.findOpenCase(kingdomId, accusedId);
        if (open.isEmpty()) {
            return PoliceResult.fail("No pending trial for that accused.");
        }
        if (findSession(kingdomId, accusedId).isPresent()) {
            return PoliceResult.fail("A trial jury is already seated for that case.");
        }
        if (villagerJurorIds == null || villagerJurorIds.size() < 3) {
            return applyRealmHandled(
                    open.get(), "Fewer than three eligible jurors online. Realm-handled trial conducted.");
        }
        Set<UUID> jurors = new HashSet<>();
        for (UUID id : villagerJurorIds) {
            if (id != null) {
                jurors.add(id);
            }
            if (jurors.size() == 3) {
                break;
            }
        }
        if (jurors.size() < 3) {
            return applyRealmHandled(
                    open.get(), "Fewer than three eligible jurors online. Realm-handled trial conducted.");
        }
        return seatSession(open.get(), jurors, true);
    }

    private PoliceResult seatSession(PoliceCase policeCase, Set<UUID> jurors, boolean villagerJury) {
        long now = System.currentTimeMillis();
        TrialJurySession session = new TrialJurySession(
                policeCase.kingdomId(),
                policeCase.accusedId(),
                policeCase.id(),
                jurors,
                now,
                now + config.windowMs(),
                villagerJury);
        sessionsByCaseId.put(policeCase.id(), session);
        return PoliceResult.ok(
                villagerJury ? "Villager trial jury of three seated." : "Trial jury of three seated.");
    }

    /**
     * Routes a freshly opened pending trial: await player Judge, seat a jury, or realm-handle.
     * If a jury is already seated for the case, leaves it running (Judge login does not seize it).
     */
    public HearingOutcome resolveHearing(String kingdomId, UUID accusedId, Set<UUID> onlineMemberIds) {
        Optional<TrialJurySession> existing = findSession(kingdomId, accusedId);
        if (existing.isPresent()) {
            return HearingOutcome.of(
                    HearingResolution.JURY_ALREADY_SEATED,
                    PoliceResult.ok("A trial jury is already seated for that case."),
                    existing);
        }
        Optional<PoliceCase> open = trialService.findOpenCase(kingdomId, accusedId);
        if (open.isEmpty()) {
            return HearingOutcome.of(
                    HearingResolution.REALM_HANDLED,
                    PoliceResult.fail("No pending trial for that accused."),
                    Optional.empty());
        }
        Set<UUID> exclusions = buildExclusions(open.get());
        if (hasEligibleOnlineJudge(kingdomId, onlineMemberIds, exclusions)) {
            return HearingOutcome.of(
                    HearingResolution.AWAITING_JUDGE,
                    PoliceResult.ok("Eligible player Judge available. Awaiting hearing."),
                    Optional.empty());
        }
        PoliceResult seated = trySeatJury(kingdomId, accusedId, onlineMemberIds);
        Optional<TrialJurySession> session = findSession(kingdomId, accusedId);
        if (session.isPresent()) {
            return HearingOutcome.of(HearingResolution.JURY_SEATED, seated, session);
        }
        return HearingOutcome.of(HearingResolution.REALM_HANDLED, seated, Optional.empty());
    }

    public Optional<TrialJurySession> findSession(String kingdomId, UUID accusedId) {
        for (TrialJurySession session : sessionsByCaseId.values()) {
            if (session.kingdomId().equals(kingdomId) && session.accusedId().equals(accusedId)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public Optional<TrialJurySession> findSessionForJuror(UUID jurorId) {
        if (jurorId == null) {
            return Optional.empty();
        }
        for (TrialJurySession session : sessionsByCaseId.values()) {
            if (session.isJuror(jurorId)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public List<TrialJurySession> listSessions() {
        return List.copyOf(sessionsByCaseId.values());
    }

    /** Expires every timed-out jury session and applies realm-handled fallbacks. */
    public List<PoliceResult> expireDueSessions(long nowMs) {
        List<TrialJurySession> timedOut = new ArrayList<>();
        for (TrialJurySession session : sessionsByCaseId.values()) {
            if (session.isTimedOut(nowMs)) {
                timedOut.add(session);
            }
        }
        List<PoliceResult> results = new ArrayList<>();
        for (TrialJurySession session : timedOut) {
            results.add(expireIfTimedOut(session.kingdomId(), session.accusedId(), nowMs));
        }
        return results;
    }

    public PoliceResult castVote(String kingdomId, UUID accusedId, UUID jurorId, boolean guilty) {
        Optional<TrialJurySession> found = findSession(kingdomId, accusedId);
        if (found.isEmpty()) {
            return PoliceResult.fail("No trial jury is seated for that case.");
        }
        TrialJurySession session = found.get();
        PoliceResult recorded = session.recordVote(jurorId, guilty);
        if (recorded instanceof PoliceResult.Failure) {
            return recorded;
        }
        if (!session.isComplete()) {
            return recorded;
        }
        return concludeSession(session);
    }

    public PoliceResult recordAbstention(String kingdomId, UUID accusedId, UUID jurorId) {
        Optional<TrialJurySession> found = findSession(kingdomId, accusedId);
        if (found.isEmpty()) {
            return PoliceResult.fail("No trial jury is seated for that case.");
        }
        TrialJurySession session = found.get();
        PoliceResult recorded = session.recordAbstention(jurorId);
        if (recorded instanceof PoliceResult.Failure) {
            return recorded;
        }
        if (!session.isComplete()) {
            return recorded;
        }
        return concludeSession(session);
    }

    public PoliceResult expireIfTimedOut(String kingdomId, UUID accusedId, long nowMs) {
        Optional<TrialJurySession> found = findSession(kingdomId, accusedId);
        if (found.isEmpty()) {
            return PoliceResult.fail("No trial jury is seated for that case.");
        }
        TrialJurySession session = found.get();
        if (!session.isTimedOut(nowMs)) {
            return PoliceResult.fail("Jury window is still open.");
        }
        Optional<PoliceCase> open = trialService.findOpenCase(kingdomId, accusedId);
        if (open.isEmpty()) {
            sessionsByCaseId.remove(session.caseId());
            return PoliceResult.ok("Jury window expired; case already closed.");
        }
        if (session.villagerJury()) {
            sessionsByCaseId.remove(session.caseId());
            return applyRealmHandled(
                    open.get(), "Villager jury spectacle concluded. Realm-handled trial conducted.");
        }
        // Decide on ballots cast so far; mark unresolved seats as abstentions.
        for (UUID jurorId : session.jurorIds()) {
            if (!session.hasResolvedSeat(jurorId)) {
                session.recordAbstention(jurorId);
            }
        }
        return concludeSession(session);
    }

    private PoliceResult concludeSession(TrialJurySession session) {
        sessionsByCaseId.remove(session.caseId());
        JuryDecision decision = session.decision();
        return switch (decision) {
            case NOT_GUILTY -> trialService.sentenceAsRealm(
                    session.kingdomId(), session.accusedId(), SentenceType.ACQUITTAL, 0, 0);
            case GUILTY -> applyDrawnSentence(session.kingdomId(), session.accusedId(), "Jury finds guilty. ");
            case ALL_ABSTAIN -> {
                Optional<PoliceCase> open =
                        trialService.findOpenCase(session.kingdomId(), session.accusedId());
                if (open.isEmpty()) {
                    yield PoliceResult.ok("Jury concluded; case already closed.");
                }
                yield applyRealmHandled(
                        open.get(),
                        "Jury reached no majority. Realm-handled trial conducted.");
            }
        };
    }

    private PoliceResult applyRealmHandled(PoliceCase policeCase, String preface) {
        sessionsByCaseId.remove(policeCase.id());
        RealmHandledSentenceTable.DrawnSentence drawn = RealmHandledSentenceTable.draw(random);
        PoliceResult applied = trialService.sentenceAsRealm(
                policeCase.kingdomId(),
                policeCase.accusedId(),
                drawn.type(),
                drawn.fineAmount(),
                drawn.prisonMinutes());
        if (applied instanceof PoliceResult.Failure) {
            return applied;
        }
        return PoliceResult.ok(preface + applied.message());
    }

    private PoliceResult applyDrawnSentence(String kingdomId, UUID accusedId, String preface) {
        RealmHandledSentenceTable.DrawnSentence drawn = RealmHandledSentenceTable.draw(random);
        PoliceResult applied = trialService.sentenceAsRealm(
                kingdomId, accusedId, drawn.type(), drawn.fineAmount(), drawn.prisonMinutes());
        if (applied instanceof PoliceResult.Failure) {
            return applied;
        }
        return PoliceResult.ok(preface + applied.message());
    }

    private boolean hasEligibleOnlineJudge(String kingdomId, Set<UUID> online, Set<UUID> exclusions) {
        for (UUID judgeId : policeService.judgesView(kingdomId)) {
            if (online.contains(judgeId) && !exclusions.contains(judgeId)) {
                return true;
            }
        }
        return false;
    }

    private List<UUID> eligiblePool(String kingdomId, Set<UUID> online, Set<UUID> exclusions) {
        List<UUID> pool = new ArrayList<>();
        if (online == null) {
            return pool;
        }
        for (UUID memberId : online) {
            if (memberId == null || exclusions.contains(memberId)) {
                continue;
            }
            if (policeService.isJudge(kingdomId, memberId)) {
                continue;
            }
            Optional<dev.mrlemoos.kingdom.model.PlayerMembership> membership =
                    kingdomService.getMembership(memberId);
            if (membership.isEmpty()) {
                continue;
            }
            if (!kingdomId.equals(membership.get().getKingdomId())) {
                continue;
            }
            pool.add(memberId);
        }
        return pool;
    }

    private Set<UUID> buildExclusions(PoliceCase policeCase) {
        Set<UUID> exclusions = new HashSet<>();
        exclusions.add(policeCase.accusedId());
        policeCase.arrestingConstableId().ifPresent(exclusions::add);
        if (justiceService != null) {
            Optional<Warrant> warrant =
                    justiceService.findById(policeCase.kingdomId(), policeCase.warrantId());
            if (warrant.isPresent()) {
                warrant.get().approvedBy().ifPresent(exclusions::add);
            }
        }
        return exclusions;
    }
}
