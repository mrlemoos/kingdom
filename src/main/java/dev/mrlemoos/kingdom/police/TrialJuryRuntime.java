package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.police.gui.TrialJuryBallotGui;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Bukkit seating loop for trial juries: resolve after arrest, open secret ballots, announce,
 * expire windows, and enforce court proximity.
 */
public final class TrialJuryRuntime {

    private final TrialJuryService juryService;
    private final PoliceTrialService trialService;
    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final TrialJuryConfig config;
    private CourtSummonService courtSummonService;
    private VillagerJuryEntityService villagerJuryEntityService;

    public TrialJuryRuntime(
            TrialJuryService juryService,
            PoliceTrialService trialService,
            KingdomService kingdomService,
            TrialJuryConfig config) {
        this(juryService, trialService, kingdomService, null, config);
    }

    public TrialJuryRuntime(
            TrialJuryService juryService,
            PoliceTrialService trialService,
            KingdomService kingdomService,
            PoliceService policeService,
            TrialJuryConfig config) {
        this.juryService = Objects.requireNonNull(juryService, "juryService");
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = policeService;
        this.config = Objects.requireNonNull(config, "config");
    }

    public void setCourtSummonService(CourtSummonService courtSummonService) {
        this.courtSummonService = courtSummonService;
    }

    public void setVillagerJuryEntityService(VillagerJuryEntityService villagerJuryEntityService) {
        this.villagerJuryEntityService = villagerJuryEntityService;
    }

    public TrialJuryService juryService() {
        return juryService;
    }

    public Set<UUID> onlineMemberIds(String kingdomId) {
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            Optional<dev.mrlemoos.kingdom.model.PlayerMembership> membership =
                    kingdomService.getMembership(player.getUniqueId());
            if (membership.isPresent() && kingdomId.equals(membership.get().getKingdomId())) {
                online.add(player.getUniqueId());
            }
        }
        return online;
    }

    public HearingOutcome resolveAfterArrest(String kingdomId, UUID accusedId) {
        HearingOutcome outcome = juryService.resolveHearing(kingdomId, accusedId, onlineMemberIds(kingdomId));
        String accusedName = displayName(accusedId);
        switch (outcome.resolution()) {
            case JURY_SEATED -> {
                broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomSeatedNotice(accusedName));
                outcome.session().ifPresent(session -> {
                    summonToCourt(kingdomId, accusedId, List.copyOf(session.jurorIds()));
                    if (!session.villagerJury()) {
                        openBallots(session, accusedName);
                    }
                });
            }
            case AWAITING_JUDGE -> {
                List<UUID> judges = new ArrayList<>();
                if (policeService != null) {
                    for (UUID judgeId : policeService.judgesView(kingdomId)) {
                        Player judge = Bukkit.getPlayer(judgeId);
                        if (judge != null && judge.isOnline()) {
                            judges.add(judgeId);
                            break;
                        }
                    }
                }
                summonToCourt(kingdomId, accusedId, judges);
            }
            case REALM_HANDLED -> {
                if (outcome.result() instanceof PoliceResult.Success) {
                    String summary = outcome.result().message();
                    if (summary.toLowerCase().contains("fewer")) {
                        broadcastKingdom(
                                kingdomId, TrialJuryAnnouncer.kingdomRealmHandledShortPool(summary));
                    } else {
                        broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomTimedOut(summary));
                    }
                    announceVerdictFeedback(kingdomId, accusedId);
                }
            }
            case JURY_ALREADY_SEATED -> {
                // Arrest message already covers custody; Judge hears separately.
            }
        }
        return outcome;
    }

    /**
     * Patrol-golem detain then the same hearing route as constable arrest (jury / realm-handled /
     * await Judge).
     */
    public PoliceResult detainByPatrolAndResolve(String kingdomId, UUID suspectId) {
        PoliceResult detained = trialService.arrestByPatrolGolem(kingdomId, suspectId);
        if (detained instanceof PoliceResult.Failure) {
            return detained;
        }
        resolveAfterArrest(kingdomId, suspectId);
        return detained;
    }

    public void openBallotFor(Player juror) {
        Optional<TrialJurySession> session = juryService.findSessionForJuror(juror.getUniqueId());
        if (session.isEmpty()) {
            juror.sendMessage(c("&cYou are not seated on a trial jury."));
            return;
        }
        TrialJurySession live = session.get();
        if (live.hasVoted(juror.getUniqueId()) || live.hasAbstained(juror.getUniqueId())) {
            juror.sendMessage(c("&7You have already voted on this jury."));
            return;
        }
        if (!isNearCourt(juror, live.kingdomId())) {
            juror.sendMessage(c("&cYou must stand near the court to cast your ballot."));
            return;
        }
        long remaining = Math.max(0L, live.closesAtMs() - System.currentTimeMillis());
        String accusedName = displayName(live.accusedId());
        TrialJuryBallotGui gui =
                TrialJuryBallotGui.create(live.kingdomId(), live.accusedId(), accusedName, remaining);
        org.bukkit.inventory.Inventory inventory = gui.getInventory();
        if (inventory != null) {
            juror.openInventory(inventory);
        }
    }

    public PoliceResult castFromGui(Player juror, String kingdomId, UUID accusedId, boolean guilty) {
        if (!isNearCourt(juror, kingdomId)) {
            juror.sendMessage(c("&cYou must stand near the court to cast your ballot."));
            return PoliceResult.fail("You must stand near the court to cast your ballot.");
        }
        PoliceResult result = juryService.castVote(kingdomId, accusedId, juror.getUniqueId(), guilty);
        if (result instanceof PoliceResult.Failure) {
            juror.sendMessage(c("&c" + result.message()));
            return result;
        }
        juror.sendMessage(c("&a" + result.message()));
        juror.closeInventory();
        if (juryService.findSession(kingdomId, accusedId).isEmpty()) {
            announceClosed(kingdomId, accusedId, result);
            releaseVillagerJurors(kingdomId);
        }
        return result;
    }

    /** Sweeps timeouts, proximity forfeits, and announces per kingdom. */
    public void sweepTimeouts(long nowMs) {
        forfeitStrayingJurors();
        List<TrialJurySession> due = new ArrayList<>();
        for (TrialJurySession session : juryService.listSessions()) {
            if (session.isTimedOut(nowMs)) {
                due.add(session);
            }
        }
        for (TrialJurySession session : due) {
            PoliceResult result =
                    juryService.expireIfTimedOut(session.kingdomId(), session.accusedId(), nowMs);
            if (result instanceof PoliceResult.Success) {
                broadcastKingdom(session.kingdomId(), TrialJuryAnnouncer.kingdomTimedOut(result.message()));
                announceVerdictFeedback(session.kingdomId(), session.accusedId());
                releaseVillagerJurors(session.kingdomId());
            }
        }
    }

    private void forfeitStrayingJurors() {
        for (TrialJurySession session : List.copyOf(juryService.listSessions())) {
            if (session.villagerJury()) {
                continue;
            }
            for (UUID jurorId : session.jurorIds()) {
                if (session.hasResolvedSeat(jurorId)) {
                    continue;
                }
                Player juror = Bukkit.getPlayer(jurorId);
                if (juror == null || !juror.isOnline() || !isNearCourt(juror, session.kingdomId())) {
                    PoliceResult result =
                            juryService.recordAbstention(session.kingdomId(), session.accusedId(), jurorId);
                    if (juror != null && juror.isOnline()) {
                        juror.sendMessage(c("&7You strayed from the court and forfeit your vote."));
                        juror.closeInventory();
                    }
                    if (juryService.findSession(session.kingdomId(), session.accusedId()).isEmpty()
                            && result instanceof PoliceResult.Success) {
                        announceClosed(session.kingdomId(), session.accusedId(), result);
                        releaseVillagerJurors(session.kingdomId());
                    }
                }
            }
        }
    }

    private void summonToCourt(String kingdomId, UUID accusedId, List<UUID> party) {
        if (courtSummonService != null) {
            courtSummonService.summonHearing(kingdomId, accusedId, party);
        }
    }

    private void releaseVillagerJurors(String kingdomId) {
        if (villagerJuryEntityService != null) {
            villagerJuryEntityService.releaseJurors(kingdomId);
        }
        // Every close path routes through here, so the magistrate turns back to the bench once,
        // wherever the hearing ended.
        if (courtSummonService != null) {
            courtSummonService.riseCourt(kingdomId);
        }
    }

    private boolean isNearCourt(Player player, String kingdomId) {
        if (policeService == null) {
            return true;
        }
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (court.isEmpty()) {
            return false;
        }
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null || !world.getName().equals(court.get().worldName())) {
            return false;
        }
        return CourtProximity.isWithinBallotRange(
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                court.get().x() + 0.5,
                court.get().y(),
                court.get().z() + 0.5);
    }

    private void announceClosed(String kingdomId, UUID accusedId, PoliceResult result) {
        String accusedName = displayName(accusedId);
        Optional<SentenceType> sentence = trialService.lastClosedSentence(kingdomId, accusedId);
        if (sentence.isPresent() && sentence.get() == SentenceType.ACQUITTAL) {
            broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomAcquittal(accusedName));
        } else {
            broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomGuilty(accusedName, result.message()));
        }
        announceVerdictFeedback(kingdomId, accusedId);
    }

    private void announceVerdictFeedback(String kingdomId, UUID accusedId) {
        String accusedName = displayName(accusedId);
        Optional<SentenceType> sentence = trialService.lastClosedSentence(kingdomId, accusedId);
        if (sentence.isPresent() && sentence.get() == SentenceType.ACQUITTAL) {
            RealmFeedback.verdictAcquittal(kingdomService, kingdomId, accusedName);
        } else if (sentence.isPresent()) {
            RealmFeedback.verdictGuilty(kingdomService, kingdomId, accusedName);
        }
    }

    private void openBallots(TrialJurySession session, String accusedName) {
        long remaining = Math.max(0L, session.closesAtMs() - System.currentTimeMillis());
        long windowSeconds = Math.max(1L, config.windowMs() / 1000L);
        for (UUID jurorId : session.jurorIds()) {
            Player juror = Bukkit.getPlayer(jurorId);
            if (juror == null || !juror.isOnline()) {
                continue;
            }
            if (!isNearCourt(juror, session.kingdomId())) {
                juror.sendMessage(
                        c("&e" + TrialJuryAnnouncer.jurorPrivatePrompt(accusedName, windowSeconds)));
                juror.sendMessage(c("&7Stand within eight blocks of the court to open your ballot."));
                continue;
            }
            juror.sendMessage(
                    c("&e" + TrialJuryAnnouncer.jurorPrivatePrompt(accusedName, windowSeconds)));
            TrialJuryBallotGui gui =
                    TrialJuryBallotGui.create(
                            session.kingdomId(), session.accusedId(), accusedName, remaining);
            org.bukkit.inventory.Inventory inventory = gui.getInventory();
            if (inventory != null) {
                juror.openInventory(inventory);
            }
        }
    }

    private void broadcastKingdom(String kingdomId, String message) {
        String coloured = c("&6" + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            Optional<dev.mrlemoos.kingdom.model.PlayerMembership> membership =
                    kingdomService.getMembership(player.getUniqueId());
            if (membership.isPresent() && kingdomId.equals(membership.get().getKingdomId())) {
                player.sendMessage(coloured);
            }
        }
    }

    private static String displayName(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
        String name = offline.getName();
        return name != null ? name : id.toString();
    }
}
