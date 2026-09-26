package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.election.ProductiveVillagerScanner;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.election.VillagerPremierInauguralService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.parliament.CommonsReturnAnnouncer;
import dev.mrlemoos.kingdom.parliament.StateOpeningCeremony;
import dev.mrlemoos.kingdom.parliament.StateOpeningSummons;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ElectionHandler {

    /** Kept for operators; the realm's own people use the Parliament hub and the poll card. */
    private static final java.util.Set<String> OPERATOR_ONLY = java.util.Set.of("start", "nominate", "vote", "speaker-vote");

    private final ElectionService electionService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final ProductiveVillagerScanner villagerScanner;
    private final VillagerMpEntityService villagerMpEntityService;
    private final NoblePrefixDisplay nobleDisplay;
    private final VillagerPremierInauguralService villagerPremierInauguralService;
    private StateOpeningCeremony stateOpeningCeremony;
    private CommonsReturnAnnouncer commonsReturnAnnouncer;

    public ElectionHandler(
            ElectionService electionService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            ProductiveVillagerScanner villagerScanner,
            VillagerMpEntityService villagerMpEntityService,
            NoblePrefixDisplay nobleDisplay,
            VillagerPremierInauguralService villagerPremierInauguralService) {
        this.electionService = electionService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.villagerScanner = villagerScanner;
        this.villagerMpEntityService = villagerMpEntityService;
        this.nobleDisplay = nobleDisplay;
        this.villagerPremierInauguralService = villagerPremierInauguralService;
    }

    public void setStateOpeningCeremony(StateOpeningCeremony stateOpeningCeremony) {
        this.stateOpeningCeremony = stateOpeningCeremony;
    }

    public void setCommonsReturnAnnouncer(CommonsReturnAnnouncer commonsReturnAnnouncer) {
        this.commonsReturnAnnouncer = commonsReturnAnnouncer;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!sender.isOp()) {
                sender.sendMessage(error("Usage: /kingdom election status"));
                sender.sendMessage(info("Right-click your poll card to stand or vote."));
                return true;
            }
            sender.sendMessage(error(
                    "Usage: /kingdom election <start|nominate|vote|speaker-vote|status>"));
            sender.sendMessage(info("To stand under a party: /kingdom election nominate "
                    + "<party> <colour> [manifesto...]"));
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (OPERATOR_ONLY.contains(subcommand) && !sender.isOp()) {
            sender.sendMessage(error("Elections are carried on a poll card. The Crown starts one from the "
                    + "Parliament hub; members stand and vote by right-clicking their poll card."));
            return true;
        }
        return switch (subcommand) {
            case "start" -> handleStart(sender);
            case "nominate" -> handleNominate(sender, args);
            case "vote" -> handleVote(sender, args);
            case "speaker-vote" -> handleSpeakerVote(sender, args);
            case "status" -> handleStatus(sender);
            default -> {
                sender.sendMessage(error("Unknown election subcommand."));
                yield true;
            }
        };
    }

    public ElectionResult openGeneralElection(String kingdomId) {
        villagerMpEntityService.releaseKingdomVillagerMps(kingdomId);
        return electionService.startGeneralElection(kingdomId);
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
                    Bukkit.broadcastMessage(c("&6A by-election has been called for MP seat ")+ seat.index() + " in " + kingdom.getDisplayName() + ".");
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
        sender.sendMessage(format(callGeneralElection(player)));
        return true;
    }

    /** The Crown dissolves Parliament and sends the realm to the polls. */
    public ElectionResult callGeneralElection(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return ElectionResult.fail("You are not in a kingdom.");
        }
        NobleRank rank = membership.get().getRank();
        if (rank != NobleRank.KING && rank != NobleRank.QUEEN) {
            return ElectionResult.fail("Only the King or Queen may call a general election.");
        }
        ElectionResult result = openGeneralElection(membership.get().getKingdomId());
        if (result instanceof ElectionResult.Success) {
            store.saveFrom(kingdomService);
        }
        return result;
    }

    private boolean handleNominate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can nominate."));
            return true;
        }
        CandidateDeclaration declaration;
        try {
            declaration = readDeclaration(args);
        } catch (IllegalArgumentException rejected) {
            sender.sendMessage(error(rejected.getMessage()));
            return true;
        }
        sender.sendMessage(format(standForElection(player, declaration)));
        return true;
    }

    /** A member puts their name forward in their realm's open election. */
    public ElectionResult standForElection(Player player, CandidateDeclaration declaration) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return ElectionResult.fail("You are not in a kingdom.");
        }
        ElectionResult result =
                electionService.nominate(membership.get().getKingdomId(), player.getUniqueId(), declaration);
        if (result instanceof ElectionResult.Success) {
            store.saveFrom(kingdomService);
        }
        return result;
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
        OfflinePlayer candidate = Bukkit.getOfflinePlayer(args[1]);
        sender.sendMessage(format(castVote(player, candidate.getUniqueId())));
        return true;
    }

    /** A member casts their ballot for a nominated candidate. */
    public ElectionResult castVote(Player player, UUID candidateId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return ElectionResult.fail("You are not in a kingdom.");
        }
        ElectionResult result = electionService.castElectionVote(
                membership.get().getKingdomId(), player.getUniqueId(), candidateId);
        if (result instanceof ElectionResult.Success) {
            store.saveFrom(kingdomService);
        }
        return result;
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
        OfflinePlayer candidate = Bukkit.getOfflinePlayer(args[1]);
        ElectionResult result = castSpeakerVote(player, candidate.getUniqueId());
        sender.sendMessage(format(result));
        if (result instanceof ElectionResult.Success) {
            sender.sendMessage(success("Election closed."));
        }
        return true;
    }

    /** The Speaker breaks a tied count; the election then closes at once. */
    public ElectionResult castSpeakerVote(Player player, UUID candidateId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return ElectionResult.fail("You are not in a kingdom.");
        }
        ElectionResult result = electionService.castSpeakerElectionVote(
                membership.get().getKingdomId(), player.getUniqueId(), candidateId);
        if (result instanceof ElectionResult.Success) {
            finishElection(membership.get().getKingdomId());
        }
        return result;
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
        for (UUID candidate : election.nominationsView()) {
            CandidateDeclaration declared = election.declaration(candidate);
            String name = Bukkit.getOfflinePlayer(candidate).getName();
            sender.sendMessage(c(declared.partyColour() + "  " + (name == null ? "Unknown" : name) + " &7("
                    + declared.partyLabel() + ")"));
            if (declared.hasManifesto()) {
                sender.sendMessage(c("&7    \"" + declared.manifesto() + "\""));
            }
        }
        return true;
    }

    /**
     * Reads what a candidate declares on nomination: {@code /kingdom election nominate [party]
     * [colour] [manifesto...]}. A candidate may decline to declare and simply stand.
     */
    private static CandidateDeclaration readDeclaration(String[] args) {
        return CandidateDeclaration.parse(
                args.length < 2 ? "" : String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }

    private void finishElection(String kingdomId) {
        Kingdom kingdom = kingdomService.getKingdom(kingdomId).orElseThrow();
        var election = kingdom.getElectionState().election();
        ElectionType electionType = election.type().orElse(null);
        boolean general = electionType == ElectionType.GENERAL;
        boolean premier = electionType == ElectionType.PREMIER;
        boolean villagerByElection = electionType == ElectionType.BY_ELECTION_VILLAGER;
        Integer byElectionSeat = election.byElectionSeatIndex().orElse(null);
        Map<String, Integer> professionCounts =
                villagerScanner.professionCounts(kingdom, villagerMpEntityService::isImmutableNpcVillager);
        if (general) {
            villagerMpEntityService.releaseKingdomVillagerMps(kingdomId);
        } else if (villagerByElection) {
            election.byElectionSeatIndex()
                    .ifPresent(seatIndex -> villagerMpEntityService.releaseSeat(kingdomId, seatIndex));
        }
        var outcome = electionService.tryCloseElection(kingdomId, professionCounts);
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
                villagerMpEntityService.syncKingdom(kingdomId);
                store.saveFrom(kingdomService);
                refreshMpDisplays(kingdomId);
                announceResult(kingdomId);
                Bukkit.broadcastMessage(c("&6The general election in ")+ kingdom.getDisplayName() + " has closed.");
                ElectionResult premierResult =
                        villagerPremierInauguralService.resolveAfterGeneralElection(kingdomId, professionCounts);
                if (premierResult instanceof ElectionResult.Success success
                        && success.message().startsWith("Premier election open")) {
                    store.saveFrom(kingdomService);
                    Bukkit.broadcastMessage(c("&6Premier election open in ")+ kingdom.getDisplayName() + ". Seated MPs may nominate and vote.");
                } else if (premierResult instanceof ElectionResult.Success success) {
                    store.saveFrom(kingdomService);
                    villagerMpEntityService.syncKingdom(kingdomId);
                    Bukkit.broadcastMessage(c("&6" + success.message()));
                    requestStateOpening(kingdom);
                } else {
                    Bukkit.broadcastMessage(c("&eNo player MPs were elected in ")+ kingdom.getDisplayName()
                            + ", and no Premier villager could be appointed.");
                }
                return;
            }
            if (premier) {
                store.saveFrom(kingdomService);
                refreshMpDisplays(kingdomId);
                announceResult(kingdomId);
                if (outcome.premierWinner() != null) {
                    Player online = Bukkit.getPlayer(outcome.premierWinner());
                    if (online != null) {
                        nobleDisplay.refresh(online);
                    }
                    Bukkit.broadcastMessage(c("&6A Premier has been elected in ")+ kingdom.getDisplayName() + ".");
                }
                requestStateOpening(kingdom);
                return;
            }
            villagerMpEntityService.syncKingdom(kingdomId);
            store.saveFrom(kingdomService);
            refreshMpDisplays(kingdomId);
            announceResult(kingdomId);
            Bukkit.broadcastMessage(c("&6The election in ")+ kingdom.getDisplayName() + " has closed.");
            if (commonsReturnAnnouncer != null && byElectionSeat != null) {
                commonsReturnAnnouncer.announceSeatReturn(kingdomId, byElectionSeat);
            }
            return;
        }
        if (premier) {
            ElectionResult appointed =
                    villagerPremierInauguralService.resolveAfterFailedPremierElection(kingdomId, professionCounts);
            if (appointed instanceof ElectionResult.Success success) {
                store.saveFrom(kingdomService);
                villagerMpEntityService.syncKingdom(kingdomId);
                Bukkit.broadcastMessage(c("&6" + success.message()));
                requestStateOpening(kingdom);
            } else {
                Bukkit.broadcastMessage(c("&eThe Premier election in ")+ kingdom.getDisplayName()
                        + " closed without electing a Premier.");
            }
        }
    }

    /**
     * A government has formed, so the Crown is summoned to open the new session. Parliament stays
     * prorogued until it does, or until the royal commission fires.
     */
    private void requestStateOpening(Kingdom kingdom) {
        if (stateOpeningCeremony == null) {
            return;
        }
        World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
        long currentMcDay = world != null ? world.getFullTime() / 24000L : 0L;
        StateOpeningSummons summons =
                stateOpeningCeremony.stateOpeningService().requestStateOpening(kingdom.getId(), currentMcDay);
        store.saveFrom(kingdomService);
        switch (summons) {
            case AWAITING_CROWN -> stateOpeningCeremony.announceSummons(kingdom.getId());
            case COMMISSIONED -> Bukkit.broadcastMessage(c("&6Parliament in ")
                    + kingdom.getDisplayName()
                    + " has been opened by royal commission — no House of Lords is set.");
            case NOT_NEEDED -> {
            }
        }
    }

    private void vacateSeat(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).flatMap(k -> k.getElectionState().seat(seatIndex)).ifPresent(seat -> {
            if (seat.kind() == dev.mrlemoos.kingdom.model.election.MpSeatKind.VILLAGER) {
                villagerMpEntityService.releaseSeat(kingdomId, seatIndex);
            }
            seat.clear();
        });
    }

    private boolean isSeatVacant(Kingdom kingdom, dev.mrlemoos.kingdom.model.election.MpSeat seat) {
        return switch (seat.kind()) {
            case PLAYER -> seat.playerId()
                    .flatMap(kingdomService::getMembership)
                    .map(m -> m.getRank() != NobleRank.MP || !kingdom.getId().equals(m.getKingdomId()))
                    .orElse(true);
            case VILLAGER -> villagerMpEntityService.isVillagerSeatVacant(kingdom, seat);
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

    /**
     * The result heard by the whole realm, and told again to each member returned to office.
     */
    private void announceResult(String kingdomId) {
        RealmFeedback.electionResult(kingdomService, kingdomId);
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (!kingdomId.equals(membership.getKingdomId())) {
                continue;
            }
            if (membership.getRank() == NobleRank.MP) {
                RealmFeedback.tookOffice(membership.getPlayerId(), "a Member of Parliament");
            } else if (membership.getRank() == NobleRank.PREMIER) {
                RealmFeedback.tookOffice(membership.getPlayerId(), "Premier");
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
        return c("&a" + message);
    }

    private static String error(String message) {
        return c("&c" + message);
    }

    private static String info(String message) {
        return c("&e" + message);
    }
}
