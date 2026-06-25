package dev.leo.kingdom.command;

import dev.leo.kingdom.display.NoblePrefixDisplay;
import dev.leo.kingdom.election.ElectionResult;
import dev.leo.kingdom.election.ElectionService;
import dev.leo.kingdom.election.ProductiveVillagerScanner;
import dev.leo.kingdom.election.VillagerMpEntityService;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.election.ElectionPhase;
import dev.leo.kingdom.model.election.ElectionType;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ElectionHandler {

    private final ElectionService electionService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final ProductiveVillagerScanner villagerScanner;
    private final VillagerMpEntityService villagerMpEntityService;
    private final NoblePrefixDisplay nobleDisplay;

    public ElectionHandler(
            ElectionService electionService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            ProductiveVillagerScanner villagerScanner,
            VillagerMpEntityService villagerMpEntityService,
            NoblePrefixDisplay nobleDisplay) {
        this.electionService = electionService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.villagerScanner = villagerScanner;
        this.villagerMpEntityService = villagerMpEntityService;
        this.nobleDisplay = nobleDisplay;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(error("Usage: /kingdom election <start|nominate|vote|speaker-vote|status>"));
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> handleStart(sender);
            case "nominate" -> handleNominate(sender);
            case "vote" -> handleVote(sender, args);
            case "speaker-vote" -> handleSpeakerVote(sender, args);
            case "status" -> handleStatus(sender);
            default -> {
                sender.sendMessage(error("Unknown election subcommand."));
                yield true;
            }
        };
    }

    public void closeDueElections() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            var election = kingdom.getElectionState().election();
            if (!election.isActive()) {
                continue;
            }
            if (election.phase() == ElectionPhase.OPEN && System.currentTimeMillis() < election.endsAtMs()) {
                continue;
            }
            finishElection(kingdom.getId());
        }
    }

    public void checkVacancies() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getElectionState().election().isActive()) {
                continue;
            }
            for (var seat : kingdom.getElectionState().seatsView().values()) {
                if (!seat.isOccupied()) {
                    continue;
                }
                if (isSeatVacant(kingdom, seat)) {
                    vacateSeat(kingdom.getId(), seat.index());
                    electionService.startByElection(kingdom.getId(), seat.index());
                    store.saveFrom(kingdomService);
                    Bukkit.broadcastMessage(ChatColor.GOLD + "A by-election has been called for MP seat "
                            + seat.index() + " in " + kingdom.getDisplayName() + ".");
                    return;
                }
            }
        }
    }

    private boolean handleStart(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can call an election."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You are not in a kingdom."));
            return true;
        }
        NobleRank rank = membership.get().getRank();
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            sender.sendMessage(error("Only the King or Queen may call a general election."));
            return true;
        }

        ElectionResult result = electionService.startGeneralElection(membership.get().getKingdomId());
        sender.sendMessage(format(result));
        if (result instanceof ElectionResult.Success) {
            villagerMpEntityService.despawnKingdomVillagerMps(membership.get().getKingdomId());
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleNominate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can nominate."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You are not in a kingdom."));
            return true;
        }
        ElectionResult result = electionService.nominate(membership.get().getKingdomId(), player.getUniqueId());
        sender.sendMessage(format(result));
        if (result instanceof ElectionResult.Success) {
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can vote."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom election vote <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You are not in a kingdom."));
            return true;
        }
        OfflinePlayer candidate = Bukkit.getOfflinePlayer(args[1]);
        ElectionResult result = electionService.castElectionVote(
                membership.get().getKingdomId(), player.getUniqueId(), candidate.getUniqueId());
        sender.sendMessage(format(result));
        if (result instanceof ElectionResult.Success) {
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleSpeakerVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can cast an election vote."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom election speaker-vote <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You are not in a kingdom."));
            return true;
        }
        OfflinePlayer candidate = Bukkit.getOfflinePlayer(args[1]);
        ElectionResult result = electionService.castSpeakerElectionVote(
                membership.get().getKingdomId(), player.getUniqueId(), candidate.getUniqueId());
        sender.sendMessage(format(result));
        if (result instanceof ElectionResult.Success) {
            finishElection(membership.get().getKingdomId());
            sender.sendMessage(success("Election closed."));
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can view election status."));
            return true;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            sender.sendMessage(error("You are not in a kingdom."));
            return true;
        }
        var election = kingdomService
                .getKingdom(membership.get().getKingdomId())
                .orElseThrow()
                .getElectionState()
                .election();
        if (!election.isActive()) {
            sender.sendMessage(info("No election is in progress."));
            return true;
        }
        long remainingMs = Math.max(0, election.endsAtMs() - System.currentTimeMillis());
        sender.sendMessage(info("Election type: "
                + election.type().orElseThrow().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        sender.sendMessage(info("Phase: " + election.phase().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        sender.sendMessage(info("Time remaining: " + formatDuration(remainingMs)));
        sender.sendMessage(info("Nominations: " + election.nominationsView().size()));
        return true;
    }

    private void finishElection(String kingdomId) {
        Kingdom kingdom = kingdomService.getKingdom(kingdomId).orElseThrow();
        var election = kingdom.getElectionState().election();
        boolean general = election.type().orElse(null) == ElectionType.GENERAL;
        var outcome = electionService.tryCloseElection(kingdomId, villagerScanner.professionCounts(kingdom));
        if (outcome.needsSpeakerTieVote()) {
            store.saveFrom(kingdomService);
            return;
        }
        if (outcome.complete()) {
            if (general) {
                World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
                if (world != null) {
                    kingdom.getElectionState().setLastGeneralElectionMcDay(world.getFullTime() / 24000L);
                }
            }
            villagerMpEntityService.syncKingdom(kingdomId);
            store.saveFrom(kingdomService);
            refreshMpDisplays(kingdomId);
            Bukkit.broadcastMessage(ChatColor.GOLD + "The election in "
                    + kingdom.getDisplayName() + " has closed.");
        }
    }

    private void vacateSeat(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).flatMap(k -> k.getElectionState().seat(seatIndex)).ifPresent(seat -> {
            if (seat.kind() == dev.leo.kingdom.model.election.MpSeatKind.VILLAGER) {
                villagerMpEntityService.despawnSeat(kingdomId, seatIndex);
            }
            seat.clear();
        });
    }

    private boolean isSeatVacant(Kingdom kingdom, dev.leo.kingdom.model.election.MpSeat seat) {
        return switch (seat.kind()) {
            case PLAYER -> seat.playerId()
                    .flatMap(kingdomService::getMembership)
                    .map(m -> m.getRank() != NobleRank.MP || !kingdom.getId().equals(m.getKingdomId()))
                    .orElse(true);
            case VILLAGER -> seat.entityId()
                    .map(id -> Bukkit.getServer().getEntity(id) == null)
                    .orElse(true);
            case null -> false;
        };
    }

    private void refreshMpDisplays(String kingdomId) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (kingdomId.equals(membership.getKingdomId()) && membership.getRank() == NobleRank.MP) {
                Player online = Bukkit.getPlayer(membership.getPlayerId());
                if (online != null) {
                    nobleDisplay.refresh(online);
                }
            }
        }
    }

    private static String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes < 60) {
            return minutes + " minutes";
        }
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        return hours + " hours " + remMinutes + " minutes";
    }

    public static List<String> tabSubcommands() {
        return List.of("start", "nominate", "vote", "speaker-vote", "status");
    }

    private static String format(ElectionResult result) {
        return result instanceof ElectionResult.Success
                ? success(result.message())
                : error(result.message());
    }

    private static String success(String message) {
        return ChatColor.GREEN + message;
    }

    private static String error(String message) {
        return ChatColor.RED + message;
    }

    private static String info(String message) {
        return ChatColor.YELLOW + message;
    }
}
