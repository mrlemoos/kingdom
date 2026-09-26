package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.honours.SwornRoleAppointments;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.police.CourtBench;
import dev.mrlemoos.kingdom.police.PoliceAuthority;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceSiting;
import dev.mrlemoos.kingdom.police.TrialJuryRuntime;
import dev.mrlemoos.kingdom.police.WarrantDesk;
import dev.mrlemoos.kingdom.appeal.AppealService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;

public final class KingdomPoliceHandler {

    private final PoliceService policeService;
    private final PoliceCourtService courtService;
    private final PoliceGolemService golemService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final TerritoryResolver territoryResolver;
    private final NoblePrefixDisplay nobleDisplay;
    private final PoliceConfig config;
    private final PoliceSiting policeSiting;
    private dev.mrlemoos.kingdom.church.ChurchService churchService;
    private TrialJuryRuntime trialJuryRuntime;
    private SwornRoleAppointments swornRoles;
    private AppealService appealService;
    private WarrantDesk warrantDesk;
    private java.util.function.Consumer<String> appealDelivery = ignored -> {};

    public KingdomPoliceHandler(
            PoliceService policeService,
            PoliceCourtService courtService,
            PoliceGolemService golemService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            TerritoryResolver territoryResolver,
            NoblePrefixDisplay nobleDisplay) {
        this.policeService = policeService;
        this.courtService = courtService;
        this.golemService = golemService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.territoryResolver = territoryResolver;
        this.nobleDisplay = nobleDisplay;
        this.config = policeService.config();
        this.policeSiting = new PoliceSiting(policeService, courtService, golemService, kingdomService, store);
    }

    /** The court and cells, for the foundation stones as well as the operators' commands. */
    public PoliceSiting policeSiting() {
        return policeSiting;
    }

    public void setTrialJuryRuntime(TrialJuryRuntime trialJuryRuntime) {
        this.trialJuryRuntime = trialJuryRuntime;
    }
    public void setAppealService(AppealService appealService) { this.appealService = appealService; }
    /** The one road for arrests, rewards and cancellations, shared with the sword, the court and the Hub. */
    public void setWarrantDesk(WarrantDesk warrantDesk) { this.warrantDesk = warrantDesk; }
    public void setAppealDelivery(java.util.function.Consumer<String> appealDelivery) { this.appealDelivery = appealDelivery; }

    public boolean handlePolice(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(policeHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "appoint" -> handleAppoint(sender, args);
            case "dismiss" -> handleDismiss(sender, args);
            case "setcell" -> refusedUnlessOperator(sender, CELLS_BY_STONE) || handleSetCell(sender, args);
            case "clearcell" -> refusedUnlessOperator(sender, CELLS_BY_STONE) || handleClearCell(sender, args);
            case "court" -> refusedUnlessOperator(sender, COURT_BY_STONE) || handleCourt(sender, args);
            case "placecourt" -> refusedUnlessOperator(sender, COURT_BY_STONE)
                    || handleCourt(sender, new String[] {"court", "set"});
            case "deploy" -> refusedUnlessOperator(sender, GOLEMS_BY_BUILDING) || handleDeploy(sender, args);
            case "despawn" -> refusedUnlessOperator(sender, GOLEMS_BY_BUILDING) || handleDespawn(sender);
            case "status" -> handleStatus(sender);
            case "list" -> handleList(sender);
            case "reward" -> refusedUnlessOperator(sender, REWARD_AT_COURT) || handleReward(sender, args);
            case "cancelwarrant" -> refusedUnlessOperator(sender, WARRANTS_FROM_REGISTER)
                    || handleCancelWarrant(sender, args);
            case "arrest" -> refusedUnlessOperator(sender, ARREST_BY_SWORD) || handleArrest(sender, args);
            case "jury" -> handleJury(sender);
            case "appeal" -> handleAppeal(sender);
            default -> {
                sender.sendMessage(policeHelp());
                yield true;
            }
        };
    }

    private boolean handleAppeal(CommandSender sender) {
        if (appealService == null) return true;
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) return true;
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) return true;
        dev.mrlemoos.kingdom.appeal.AppealResult result = appealService.petition(membership.get().getKingdomId(), player.get().getUniqueId());
        sender.sendMessage(formatAppeal(result));
        if (result instanceof dev.mrlemoos.kingdom.appeal.AppealResult.Success) appealDelivery.accept(membership.get().getKingdomId());
        return true;
    }

    public void respawnAllJudges() {
        courtService.respawnAllJudges();
        store.saveFrom(kingdomService);
    }

    public void pruneStaleEntities() {
        golemService.pruneStaleGolems();
        courtService.pruneStaleJudges();
        store.saveFrom(kingdomService);
    }

    /** Wires the coronation gate over sworn appointments. */
    public void setChurchService(dev.mrlemoos.kingdom.church.ChurchService churchService) {
        this.churchService = churchService;
        this.swornRoles = churchService == null ? null : new SwornRoleAppointments(policeService, churchService);
    }

    /** The one road for sworn roles, shared with the golden sword. */
    public SwornRoleAppointments swornRoles() {
        return swornRoles;
    }

    private boolean handleAppoint(CommandSender sender, String[] args) {
        return handleSworn(sender, args, true);
    }

    private boolean handleDismiss(CommandSender sender, String[] args) {
        return handleSworn(sender, args, false);
    }

    /** The operators' escape hatch: swear or unswear for the Crown of the subject's own realm. */
    private boolean handleSworn(CommandSender sender, String[] args, boolean swearing) {
        if (refusedUnlessOperator(sender, SWORN_BY_SWORD)) {
            return true;
        }
        String usage = "Usage: /kingdom police " + (swearing ? "appoint" : "dismiss") + " <constable|judge> <player>";
        if (args.length < 3 || swornRoles == null) {
            sender.sendMessage(error(usage));
            return true;
        }
        SwornRole role = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "constable" -> SwornRole.CONSTABLE;
            case "judge" -> SwornRole.JUDGE;
            default -> null;
        };
        if (role == null) {
            sender.sendMessage(error(usage));
            return true;
        }
        UUID targetId = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
        Optional<PlayerMembership> subject = kingdomService.getMembership(targetId);
        if (subject.isEmpty()) {
            sender.sendMessage(error("That player is not a member of any kingdom."));
            return true;
        }
        String kingdomId = subject.get().getKingdomId();
        SwornRoleAppointments.Outcome outcome = swearing
                ? swornRoles.swear(kingdomId, null, NobleRank.KING, targetId, role)
                : swornRoles.unswear(kingdomId, null, NobleRank.KING, targetId, role);
        sender.sendMessage(outcome.success() ? success(outcome.message()) : error(outcome.message()));
        if (outcome.success()) {
            store.saveFrom(kingdomService);
            refreshDisplayIfOnline(targetId);
        }
        return true;
    }

    private boolean handleSetCell(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom police setcell <slot>"));
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(error("Cell slot must be a number."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Location location = player.get().getLocation();
        if (!isInOwnTerritory(location, kingdomId)) {
            sender.sendMessage(error(territoryError(kingdomId, location)));
            return true;
        }

        PrisonCellLocation cell = new PrisonCellLocation(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        sender.sendMessage(formatPolice(policeSiting.setCell(
                kingdomId,
                membership.get().getRank(),
                sender.isOp(),
                slot,
                cell)));
        return true;
    }

    private boolean handleClearCell(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom police clearcell <slot>"));
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(error("Cell slot must be a number."));
            return true;
        }

        sender.sendMessage(formatPolice(policeSiting.clearCell(
                membership.get().getKingdomId(),
                membership.get().getRank(),
                sender.isOp(),
                slot)));
        return true;
    }

    private boolean handleCourt(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom police court <set|clear>"));
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> handleCourtSet(sender);
            case "clear" -> handleCourtClear(sender);
            default -> {
                sender.sendMessage(error("Usage: /kingdom police court <set|clear>"));
                yield true;
            }
        };
    }

    private boolean handleCourtSet(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!PoliceAuthority.canConfigureSites(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error("Only the King, Queen, or an operator may set the court."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Location standing = player.get().getLocation();
        if (standing.getWorld() == null) {
            sender.sendMessage(error("You must be in a loaded world to set the court."));
            return true;
        }
        if (!isInOwnTerritory(standing, kingdomId)) {
            sender.sendMessage(error(territoryError(kingdomId, standing)));
            return true;
        }

        CourtLocation court = new CourtLocation(
                standing.getWorld().getName(),
                standing.getBlockX(),
                standing.getBlockY(),
                standing.getBlockZ(),
                CourtBench.normaliseYaw(standing.getYaw()));
        sender.sendMessage(formatPolice(policeSiting.siteCourt(
                kingdomId,
                membership.get().getRank(),
                sender.isOp(),
                court)));
        return true;
    }

    private boolean handleCourtClear(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!PoliceAuthority.canConfigureSites(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error("Only the King, Queen, or an operator may clear the court."));
            return true;
        }

        sender.sendMessage(formatPolice(policeSiting.clearCourt(
                membership.get().getKingdomId(), membership.get().getRank(), sender.isOp())));
        return true;
    }

    private boolean handleDeploy(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom police deploy <patrol|guard>"));
            return true;
        }
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!PoliceAuthority.canDeployGolems(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error(
                    "Only the King, Queen, a Knight, or an operator may deploy police golems."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Location location = player.get().getLocation();
        if (!isInOwnTerritory(location, kingdomId)) {
            sender.sendMessage(error(territoryError(kingdomId, location)));
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "patrol" -> deployPatrol(sender, kingdomId, location);
            case "guard" -> deployGuard(sender, kingdomId, location);
            default -> {
                sender.sendMessage(error("Usage: /kingdom police deploy <patrol|guard>"));
                yield true;
            }
        };
    }

    private boolean deployPatrol(CommandSender sender, String kingdomId, Location location) {
        IronGolem golem = golemService.spawnPatrol(kingdomId, location);
        PoliceResult result = policeService.registerPatrolGolem(kingdomId, golem.getUniqueId());
        if (result instanceof PoliceResult.Failure) {
            golemService.removeGolem(golem);
            sender.sendMessage(formatPolice(result));
            return true;
        }
        store.saveFrom(kingdomService);
        sender.sendMessage(success("Patrol constable deployed."));
        return true;
    }

    private boolean deployGuard(CommandSender sender, String kingdomId, Location location) {
        IronGolem golem = golemService.spawnGuard(kingdomId, location);
        PoliceResult result = policeService.registerGuardGolem(kingdomId, golem.getUniqueId());
        if (result instanceof PoliceResult.Failure) {
            golemService.removeGolem(golem);
            sender.sendMessage(formatPolice(result));
            return true;
        }
        store.saveFrom(kingdomService);
        sender.sendMessage(success("Guard watch deployed."));
        return true;
    }

    private boolean handleDespawn(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!PoliceAuthority.canConfigureSites(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error("Only the King, Queen, or an operator may despawn police golems."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Optional<IronGolem> golem = golemService.findGolemForDespawn(player.get(), kingdomId);
        if (golem.isEmpty()) {
            sender.sendMessage(error("No registered police golem found nearby."));
            return true;
        }

        UUID entityId = golem.get().getUniqueId();
        PoliceResult result = policeService.deregisterGolem(kingdomId, entityId);
        golemService.removeGolem(golem.get());
        sender.sendMessage(formatPolice(result));
        if (result instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        KingdomPoliceState police = policeService.policeState(kingdomId);
        if (police == null) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        boolean ready = policeService.isPoliceReady(kingdomId);
        sender.sendMessage(info("Police readiness: " + (ready ? "ready" : "not ready")));
        sender.sendMessage(c("&7Configured cells: ")+ c("&f" + police.configuredCellCount()));
        sender.sendMessage(c("&7Court: ")+ c("&f" + (police.hasCourt() ? "configured" : "not configured")));
        sender.sendMessage(c("&7Constables: ")+ c("&f" + police.constablesView().size()));
        sender.sendMessage(c("&7Judges: ")+ c("&f" + police.judgesView().size()));
        sender.sendMessage(c("&7Patrol golems: ")+ c("&f" + police.patrolGolemCount()) + c("&7 / ")+ config.maxPatrolGolems());
        sender.sendMessage(c("&7Guard golems: ")+ c("&f" + police.guardGolemCount()) + c("&7 / ")+ config.maxGuardGolems());
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        KingdomPoliceState police = policeService.policeState(kingdomId);
        if (police == null) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }

        sender.sendMessage(info("Police department:"));
        if (police.cellsView().isEmpty()) {
            sender.sendMessage(c("&7No prison cells configured."));
        } else {
            for (Map.Entry<Integer, PrisonCellLocation> entry : police.cellsView().entrySet()) {
                PrisonCellLocation cell = entry.getValue();
                sender.sendMessage(c("&7Cell ")+ entry.getKey() + ": "
                        + cell.worldName() + " @ " + cell.x() + ", " + cell.y() + ", " + cell.z());
            }
        }

        if (police.hasCourt()) {
            CourtLocation court = police.court().orElseThrow();
            sender.sendMessage(c("&7Court: ")+ court.worldName() + " @ "
                    + court.x() + ", " + court.y() + ", " + court.z());
        } else {
            sender.sendMessage(c("&7Court: not configured."));
        }

        listSwornRole(sender, "Constables", police.constablesView());
        listSwornRole(sender, "Judges", police.judgesView());
        return true;
    }

    private boolean handleReward(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty() || deskMissing(sender)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom police reward <player> <amount>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!warrantDesk.nearCourt(player.get().getLocation(), kingdomId)) {
            sender.sendMessage(error("Post arrest rewards at the court."));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(error("Amount must be a number."));
            return true;
        }
        sender.sendMessage(formatPolice(warrantDesk.postReward(
                kingdomId, player.get().getUniqueId(), target.getUniqueId(), amount)));
        return true;
    }

    private boolean handleCancelWarrant(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty() || deskMissing(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom police cancelwarrant <player>"));
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        sender.sendMessage(formatPolice(warrantDesk.cancelForSuspect(
                membership.get().getKingdomId(), player.get().getUniqueId(), target.getUniqueId())));
        return true;
    }

    private boolean handleArrest(CommandSender sender, String[] args) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty() || deskMissing(sender)) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!warrantDesk.isConstable(kingdomId, player.get().getUniqueId())) {
            sender.sendMessage(error("Only a constable may arrest."));
            return true;
        }
        Optional<Player> suspect = resolveArrestTarget(player.get(), args);
        if (suspect.isEmpty()) {
            sender.sendMessage(error("Usage: /kingdom police arrest <player> (or aim at a player)"));
            return true;
        }
        if (!warrantDesk.inJurisdiction(suspect.get().getLocation(), kingdomId)) {
            sender.sendMessage(error("The suspect must be inside your kingdom's territory."));
            return true;
        }
        sender.sendMessage(formatPolice(
                warrantDesk.arrest(kingdomId, player.get().getUniqueId(), suspect.get().getUniqueId())));
        return true;
    }

    private boolean deskMissing(CommandSender sender) {
        if (warrantDesk != null) {
            return false;
        }
        sender.sendMessage(error("Police trial services are not ready."));
        return true;
    }

    private boolean handleJury(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        if (trialJuryRuntime == null) {
            sender.sendMessage(error("Trial jury is not ready."));
            return true;
        }
        trialJuryRuntime.openBallotFor(player.get());
        return true;
    }

    private Optional<Player> resolveArrestTarget(Player constable, String[] args) {
        if (args.length >= 2) {
            Player named = Bukkit.getPlayerExact(args[1]);
            return Optional.ofNullable(named);
        }
        var targetEntity = constable.getTargetEntity(6);
        if (targetEntity instanceof Player aimed) {
            return Optional.of(aimed);
        }
        return Optional.empty();
    }

    private void listSwornRole(CommandSender sender, String label, java.util.Set<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            sender.sendMessage(c("&7" + label + ": none."));
            return;
        }
        sender.sendMessage(c("&7" + label + ":"));
        for (UUID playerId : playerIds) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(playerId);
            String name = member.getName() != null ? member.getName() : playerId.toString();
            sender.sendMessage(c("&7 - ")+ c("&f" + name));
        }
    }

    private boolean isInOwnTerritory(Location location, String kingdomId) {
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
        return territory.type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
    }

    private String territoryError(String kingdomId, Location location) {
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
        List<String> regions = WorldGuardBridge.regionsAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        if (regions.isEmpty()) {
            return "You must be inside your kingdom's linked territory.";
        }
        if (territory.type() == TerritoryLocation.IncomeLocation.FOREIGN_KINGDOM) {
            String other = territory.kingdomId().orElse("another kingdom");
            return "That location is in " + other + "'s territory, not yours.";
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        String linked = kingdom.isPresent()
                ? kingdomService.territoryLabel(kingdom.get()).orElse("not set")
                : "not set";
        return "WorldGuard region '" + regions.get(0) + "' is not linked to your kingdom ("
                + linked + "). Ask an admin to run /kingdom setregion.";
    }

    private void refreshDisplayIfOnline(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            nobleDisplay.refresh(online);
        }
    }

    private Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(error("Only players may use this command."));
        return Optional.empty();
    }

    private static final String CELLS_BY_STONE = "The cells are sited by laying their foundation stones. "
            + "Type /kingdom, open Police and take one from The Prison Cells.";
    static final String SWORN_BY_SWORD = "Constables, judges and the priest are sworn with the golden sword. "
            + "Strike a subject with one to open the honours window.";
    private static final String GOLEMS_BY_BUILDING = "Police golems are built, not summoned. "
            + "The Crown or a Knight builds an iron golem inside the realm's territory; "
            + "the Crown right-clicks an officer to stand it down.";
    private static final String ARREST_BY_SWORD = "A constable arrests by striking a wanted subject "
            + "with an iron sword inside the realm's territory.";
    private static final String REWARD_AT_COURT = "Arrest rewards are posted at the court. "
            + "Right-click the court's lectern, or sneak and right-click the judge.";
    private static final String WARRANTS_FROM_REGISTER = "The Crown cancels warrants from the warrant register. "
            + "Type /kingdom, open Police and click The Warrant Register.";
    private static final String COURT_BY_STONE = "The court is sited by laying its lectern. "
            + "Type /kingdom, open Police and take it from The Court.";

    /** The siting commands are the operators' escape hatch; everyone else is pointed to the Hub. */
    private boolean refusedUnlessOperator(CommandSender sender, String pointer) {
        if (sender.isOp()) {
            return false;
        }
        sender.sendMessage(error(pointer));
        return true;
    }

    private Optional<PlayerMembership> requireMembership(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        return membership;
    }

    private String policeHelp() {
        return info("Police commands:")
                + "\n" + c("&e/kingdom police appoint|dismiss constable|judge <player>")
                + c("&7 — operators; the Crown strikes a subject with a golden sword")
                + "\n" + c("&e/kingdom police setcell|clearcell <slot>") + c("&7 — operators; the Crown lays cell stones from /kingdom")
                + "\n" + c("&e/kingdom police court set|clear") + c("&7 — operators; the Crown lays the court's lectern from /kingdom")
                + "\n" + c("&e/kingdom police deploy patrol") + c("&7 — operators; the Crown or a Knight builds an iron golem")
                + "\n" + c("&e/kingdom police deploy guard") + c("&7 — operators")
                + "\n" + c("&e/kingdom police despawn") + c("&7 — operators; the Crown stands an officer down from its orders window")
                + "\n" + c("&e/kingdom police reward <player> <amount>") + c("&7 — operators; posted at the court's lectern")
                + "\n" + c("&e/kingdom police cancelwarrant <player>") + c("&7 — operators; the Crown uses the warrant register in /kingdom")
                + "\n" + c("&e/kingdom police arrest <player>") + c("&7 — operators; a constable strikes the wanted with an iron sword")
                + "\n" + c("&e/kingdom police jury") + c("&7 — reopen trial-jury ballot")
                + "\n" + c("&e/kingdom police appeal") + c("&7 — petition Crown against active prison sentence")
                + "\n" + c("&e/kingdom police status")
                + "\n" + c("&e/kingdom police list");
    }

    private String formatPolice(PoliceResult result) {
        return switch (result) {
            case PoliceResult.Success success -> success(success.message());
            case PoliceResult.Failure failure -> error(failure.message());
        };
    }

    private String formatAppeal(dev.mrlemoos.kingdom.appeal.AppealResult result) {
        return result instanceof dev.mrlemoos.kingdom.appeal.AppealResult.Success success
                ? success(success.message()) : error(((dev.mrlemoos.kingdom.appeal.AppealResult.Failure) result).message());
    }

    private String success(String message) {
        return c("&a" + message);
    }

    private String error(String message) {
        return c("&c" + message);
    }

    private String info(String message) {
        return c("&b" + message);
    }
}
