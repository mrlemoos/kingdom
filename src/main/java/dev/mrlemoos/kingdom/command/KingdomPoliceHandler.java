package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.police.PoliceAuthority;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
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
    }

    public boolean handlePolice(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(policeHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "appoint" -> handleAppoint(sender, args);
            case "dismiss" -> handleDismiss(sender, args);
            case "setcell" -> handleSetCell(sender, args);
            case "clearcell" -> handleClearCell(sender, args);
            case "placecourt" -> handlePlaceCourt(sender);
            case "deploy" -> handleDeploy(sender, args);
            case "despawn" -> handleDespawn(sender);
            case "status" -> handleStatus(sender);
            case "list" -> handleList(sender);
            default -> {
                sender.sendMessage(policeHelp());
                yield true;
            }
        };
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

    private boolean handleAppoint(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom police appoint <constable|judge> <player>"));
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID targetId = target.getUniqueId();
        String kingdomId = membership.get().getKingdomId();
        NobleRank rank = membership.get().getRank();
        PoliceResult result = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "constable" -> policeService.appointConstable(kingdomId, rank, targetId);
            case "judge" -> policeService.appointJudge(kingdomId, rank, targetId);
            default -> PoliceResult.fail("Usage: /kingdom police appoint <constable|judge> <player>");
        };
        sender.sendMessage(formatPolice(result));
        if (result instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
            refreshDisplayIfOnline(targetId);
        }
        return true;
    }

    private boolean handleDismiss(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom police dismiss <constable|judge> <player>"));
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID targetId = target.getUniqueId();
        String kingdomId = membership.get().getKingdomId();
        NobleRank rank = membership.get().getRank();
        PoliceResult result = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "constable" -> policeService.dismissConstable(kingdomId, rank, targetId);
            case "judge" -> policeService.dismissJudge(kingdomId, rank, targetId);
            default -> PoliceResult.fail("Usage: /kingdom police dismiss <constable|judge> <player>");
        };
        sender.sendMessage(formatPolice(result));
        if (result instanceof PoliceResult.Success) {
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
        PoliceResult result = policeService.setCell(
                kingdomId,
                membership.get().getRank(),
                sender.isOp(),
                slot,
                cell);
        sender.sendMessage(formatPolice(result));
        if (result instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
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

        PoliceResult result = policeService.clearCell(
                membership.get().getKingdomId(),
                membership.get().getRank(),
                sender.isOp(),
                slot);
        sender.sendMessage(formatPolice(result));
        if (result instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handlePlaceCourt(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!PoliceAuthority.canConfigureSites(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error("Only the King, Queen, or an operator may place the court."));
            return true;
        }

        Block lectern = findLecternBlock(player.get());
        if (lectern == null) {
            sender.sendMessage(error("Stand at or look at a lectern to place the court."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        if (!isLecternInTerritory(lectern, kingdomId)) {
            sender.sendMessage(error(territoryError(kingdomId, lectern.getLocation())));
            return true;
        }

        CourtLocation court = new CourtLocation(
                lectern.getWorld().getName(),
                lectern.getX(),
                lectern.getY(),
                lectern.getZ());
        PoliceResult result = policeService.setCourt(
                kingdomId,
                membership.get().getRank(),
                sender.isOp(),
                court);
        sender.sendMessage(formatPolice(result));
        if (!(result instanceof PoliceResult.Success)) {
            return true;
        }

        courtService.ensureJudge(kingdomId);
        store.saveFrom(kingdomService);
        sender.sendMessage(success("Magistrate seated at court lectern."));
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
        if (!PoliceAuthority.canConfigureSites(membership.get().getRank(), sender.isOp())) {
            sender.sendMessage(error("Only the King, Queen, or an operator may deploy police golems."));
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

    private boolean isLecternInTerritory(Block lectern, String kingdomId) {
        return isInOwnTerritory(lectern.getLocation(), kingdomId);
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
        String linked = kingdom.flatMap(kingdomService::territoryLabel).orElse("not set");
        return "WorldGuard region '" + regions.get(0) + "' is not linked to your kingdom ("
                + linked + "). Ask an admin to run /kingdom setregion.";
    }

    private Block findLecternBlock(Player player) {
        Block atFeet = player.getLocation().getBlock();
        if (atFeet.getType() == Material.LECTERN) {
            return atFeet;
        }
        Block below = atFeet.getRelative(0, -1, 0);
        if (below.getType() == Material.LECTERN) {
            return below;
        }
        Block target = player.getTargetBlockExact(5);
        if (target != null && target.getType() == Material.LECTERN) {
            return target;
        }
        return null;
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
                + "\n" + c("&e/kingdom police appoint constable <player>") + c("&7 — King or Queen")
                + "\n" + c("&e/kingdom police dismiss constable <player>")
                + "\n" + c("&e/kingdom police appoint judge <player>")
                + "\n" + c("&e/kingdom police dismiss judge <player>")
                + "\n" + c("&e/kingdom police setcell <slot>") + c("&7 — mark prison cell in territory")
                + "\n" + c("&e/kingdom police clearcell <slot>")
                + "\n" + c("&e/kingdom police placecourt") + c("&7 — court lectern in territory")
                + "\n" + c("&e/kingdom police deploy patrol") + c("&7 — spawn patrol golem")
                + "\n" + c("&e/kingdom police deploy guard") + c("&7 — spawn guard golem")
                + "\n" + c("&e/kingdom police despawn") + c("&7 — remove aimed or nearest golem")
                + "\n" + c("&e/kingdom police status")
                + "\n" + c("&e/kingdom police list");
    }

    private String formatPolice(PoliceResult result) {
        return switch (result) {
            case PoliceResult.Success success -> success(success.message());
            case PoliceResult.Failure failure -> error(failure.message());
        };
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
