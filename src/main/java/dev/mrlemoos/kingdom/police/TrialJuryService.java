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

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final PoliceTrialService trialService;
    private final MechanicalJusticeService justiceService;
    private final TrialJuryConfig config;
    private final Random random;
    private final Map<String, TrialJurySession> sessionsByCaseId = new HashMap<>();

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
        if (pool.size() < 3) {
            return applyRealmHandled(policeCase, "Fewer than three eligible jurors online. Realm-handled trial conducted.");
        }

        Collections.shuffle(pool, random);
        Set<UUID> jurors = new HashSet<>(pool.subList(0, 3));
        long now = System.currentTimeMillis();
        TrialJurySession session = new TrialJurySession(
                kingdomId,
                accusedId,
                policeCase.id(),
                jurors,
                now,
                now + config.windowMs());
        sessionsByCaseId.put(policeCase.id(), session);
        return PoliceResult.ok("Trial jury of three seated.");
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
        Optional<Boolean> majority = session.majorityGuilty();
        sessionsByCaseId.remove(session.caseId());
        if (majority.isEmpty()) {
            return PoliceResult.fail("Jury vote incomplete.");
        }
        if (!majority.get()) {
            return trialService.sentenceAsRealm(kingdomId, accusedId, SentenceType.ACQUITTAL, 0, 0);
        }
        return applyDrawnSentence(kingdomId, accusedId, "Jury finds guilty. ");
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
        sessionsByCaseId.remove(session.caseId());
        if (open.isEmpty()) {
            return PoliceResult.ok("Jury window expired; case already closed.");
        }
        return applyRealmHandled(
                open.get(), "Jury window expired without a full vote. Realm-handled trial conducted.");
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
