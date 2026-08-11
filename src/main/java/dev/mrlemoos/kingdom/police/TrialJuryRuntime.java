package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Bukkit seating loop for trial juries: resolve after arrest, open secret ballots, announce,
 * expire windows.
 */
public final class TrialJuryRuntime {

    private final TrialJuryService juryService;
    private final PoliceTrialService trialService;
    private final KingdomService kingdomService;
    private final TrialJuryConfig config;

    public TrialJuryRuntime(
            TrialJuryService juryService,
            PoliceTrialService trialService,
            KingdomService kingdomService,
            TrialJuryConfig config) {
        this.juryService = Objects.requireNonNull(juryService, "juryService");
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.config = Objects.requireNonNull(config, "config");
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
                outcome.session().ifPresent(session -> openBallots(session, accusedName));
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
                }
            }
            case AWAITING_JUDGE, JURY_ALREADY_SEATED -> {
                // Arrest message already covers custody; Judge hears separately.
            }
        }
        return outcome;
    }

    public void openBallotFor(Player juror) {
        Optional<TrialJurySession> session = juryService.findSessionForJuror(juror.getUniqueId());
        if (session.isEmpty()) {
            juror.sendMessage(c("&cYou are not seated on a trial jury."));
            return;
        }
        TrialJurySession live = session.get();
        if (live.hasVoted(juror.getUniqueId())) {
            juror.sendMessage(c("&7You have already voted on this jury."));
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
        PoliceResult result = juryService.castVote(kingdomId, accusedId, juror.getUniqueId(), guilty);
        if (result instanceof PoliceResult.Failure) {
            juror.sendMessage(c("&c" + result.message()));
            return result;
        }
        juror.sendMessage(c("&a" + result.message()));
        juror.closeInventory();
        if (juryService.findSession(kingdomId, accusedId).isEmpty()) {
            announceClosed(kingdomId, accusedId, result);
        }
        return result;
    }

    /** Sweeps timeouts and announces per kingdom. */
    public void sweepTimeouts(long nowMs) {
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
            }
        }
    }

    private void announceClosed(String kingdomId, UUID accusedId, PoliceResult result) {
        String accusedName = displayName(accusedId);
        Optional<SentenceType> sentence = trialService.lastClosedSentence(kingdomId, accusedId);
        if (sentence.isPresent() && sentence.get() == SentenceType.ACQUITTAL) {
            broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomAcquittal(accusedName));
            return;
        }
        broadcastKingdom(kingdomId, TrialJuryAnnouncer.kingdomGuilty(accusedName, result.message()));
    }

    private void openBallots(TrialJurySession session, String accusedName) {
        long remaining = Math.max(0L, session.closesAtMs() - System.currentTimeMillis());
        long windowSeconds = Math.max(1L, config.windowMs() / 1000L);
        for (UUID jurorId : session.jurorIds()) {
            Player juror = Bukkit.getPlayer(jurorId);
            if (juror == null || !juror.isOnline()) {
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
