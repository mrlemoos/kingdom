package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportResult;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpCommand {

    private static final String PERM_TELEPORT = "minecraft.command.teleport";
    private static final String PERM_CHECKPOINT = "kingdom.teleport.checkpoint";

    private final TeleportService teleportService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final KingdomTerritoryResolver territoryResolver;

    public TpCommand(
            TeleportService teleportService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            KingdomTerritoryResolver territoryResolver) {
        this.teleportService = teleportService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.territoryResolver = territoryResolver;
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && "checkpoint".equalsIgnoreCase(args[0])) {
            handleCheckpoint(sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        handleVanillaTeleport(sender, args);
    }

    private void handleCheckpoint(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(error("Usage: /tp checkpoint <create|delete|list> ..."));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCheckpointCreate(sender, args);
            case "delete" -> handleCheckpointDelete(sender, args);
            case "list" -> handleCheckpointList(sender, args);
            default -> sender.sendMessage(error("Usage: /tp checkpoint <create|delete|list> ..."));
        }
    }

    private void handleCheckpointCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may create checkpoints."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /tp checkpoint create <name>"));
            sender.sendMessage(error("       /tp checkpoint create <kingdom> <name>"));
            return;
        }

        String kingdomId;
        String rawName;
        if (args.length >= 3 && kingdomService.getKingdom(args[1]).isPresent()) {
            kingdomId = Kingdom.normaliseId(args[1]);
            rawName = args[2];
        } else {
            Optional<String> territoryKingdom = kingdomAt(player.getLocation());
            if (territoryKingdom.isEmpty()) {
                sender.sendMessage(error("Stand in a kingdom territory or specify the kingdom."));
                return;
            }
            kingdomId = territoryKingdom.get();
            rawName = args[1];
        }

        Location location = player.getLocation();
        TeleportPlace place = TeleportPlace.of(
                rawName,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        TeleportResult result = teleportService.createPlace(kingdomId, place);
        sender.sendMessage(format(result));
        if (result instanceof TeleportResult.Success) {
            store.saveFrom(kingdomService);
        }
        return;
    }

    private void handleCheckpointDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /tp checkpoint delete <name>"));
            sender.sendMessage(error("       /tp checkpoint delete <kingdom> <name>"));
            return;
        }

        String kingdomId;
        String name;
        if (args.length >= 3 && kingdomService.getKingdom(args[1]).isPresent()) {
            kingdomId = Kingdom.normaliseId(args[1]);
            name = args[2];
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(error("Specify the kingdom when using the console."));
                return;
            }
            Optional<String> territoryKingdom = kingdomAt(player.getLocation());
            if (territoryKingdom.isEmpty()) {
                sender.sendMessage(error("Stand in a kingdom territory or specify the kingdom."));
                return;
            }
            kingdomId = territoryKingdom.get();
            name = args[1];
        }

        TeleportResult result = teleportService.deletePlace(kingdomId, name);
        sender.sendMessage(format(result));
        if (result instanceof TeleportResult.Success) {
            store.saveFrom(kingdomService);
        }
        return;
    }

    private void handleCheckpointList(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String kingdomId = Kingdom.normaliseId(args[1]);
            Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
            if (kingdom.isEmpty()) {
                sender.sendMessage(error("Unknown kingdom."));
                return;
            }
            listKingdomCheckpoints(sender, kingdom.get());
            return;
        }

        List<Kingdom> kingdoms = kingdomService.listKingdoms().stream()
                .filter(kingdom -> !kingdom.getTeleportsView().isEmpty())
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
        if (kingdoms.isEmpty()) {
            sender.sendMessage(info("No checkpoints defined."));
            return;
        }
        for (Kingdom kingdom : kingdoms) {
            listKingdomCheckpoints(sender, kingdom);
        }
        return;
    }

    private void listKingdomCheckpoints(CommandSender sender, Kingdom kingdom) {
        List<TeleportPlace> places = teleportService.listPlaces(kingdom.getId());
        if (places.isEmpty()) {
            return;
        }
        sender.sendMessage(info(kingdom.getDisplayName() + " checkpoints:"));
        for (TeleportPlace place : places) {
            sender.sendMessage(c("&7  ")+ place.name() + c("&f — ")+ place.worldName() + " "
                    + formatCoord(place.x()) + " "
                    + formatCoord(place.y()) + " "
                    + formatCoord(place.z()));
        }
    }

    private void handleVanillaTeleport(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(help(sender));
            return;
        }

        if (isCoordinateForm(args)) {
            handleCoordinateTeleport(sender, args);
            return;
        }

        if (args.length == 2 && isBringHereToken(args[0])) {
            handleBringHere(sender, args[1]);
            return;
        }

        if (args.length == 1) {
            handleSelfToDestination(sender, args[0]);
            return;
        }

        if (args.length == 2) {
            handleTargetToDestination(sender, args[0], args[1]);
            return;
        }

        sender.sendMessage(error("Unrecognised teleport command."));
    }

    private void handleCoordinateTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_TELEPORT)) {
            sender.sendMessage(error("You do not have permission to teleport."));
            return;
        }

        Player target;
        int coordStart;
        if (args.length == 3 || args.length == 5) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(error("Specify a player when teleporting from the console."));
                return;
            }
            target = player;
            coordStart = 0;
        } else if (args.length == 4 || args.length == 6) {
            target = findOnlinePlayer(args[0]).orElse(null);
            if (target == null) {
                sender.sendMessage(error("Unknown player."));
                return;
            }
            coordStart = 1;
        } else {
            sender.sendMessage(error("Unrecognised coordinate teleport."));
            return;
        }

        World world = target.getWorld();
        Location destination;
        try {
            destination = TeleportCoordinateParser.parseLocation(world, args, coordStart, target.getLocation());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(error("Invalid coordinates."));
            return;
        }

        String staffNotifyDestination = shouldNotifyStaff(sender, target)
                ? TeleportStaffNotifier.formatCoordinates(destination)
                : null;
        if (!teleportPlayer(sender, target, destination, staffNotifyDestination)) {
            return;
        }
        if (!target.equals(sender)) {
            sender.sendMessage(success("Teleported " + target.getName() + "."));
        }
    }

    private void handleBringHere(CommandSender sender, String targetName) {
        if (!sender.hasPermission(PERM_TELEPORT)) {
            sender.sendMessage(error("You do not have permission to teleport."));
            return;
        }
        if (!(sender instanceof Player source)) {
            sender.sendMessage(error("Only players may bring others here."));
            return;
        }

        Player target = findOnlinePlayer(targetName).orElse(null);
        if (target == null) {
            sender.sendMessage(error("Unknown player."));
            return;
        }

        if (!teleportPlayer(
                sender,
                target,
                source.getLocation(),
                TeleportStaffNotifier.formatCoordinates(source.getLocation()))) {
            return;
        }
        if (!target.equals(sender)) {
            sender.sendMessage(success("Teleported " + target.getName() + " here."));
        }
    }

    private void handleSelfToDestination(CommandSender sender, String destinationName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Specify a player or coordinates when using the console."));
            return;
        }

        Optional<Player> online = findOnlinePlayer(destinationName);
        if (online.isPresent()) {
            if (!sender.hasPermission(PERM_TELEPORT)) {
                sender.sendMessage(error("You do not have permission to teleport."));
                return;
            }
            teleportPlayer(sender, player, online.get().getLocation());
            return;
        }

        Optional<TeleportPlace> checkpoint = resolveMemberCheckpoint(player, destinationName);
        if (checkpoint.isPresent()) {
            teleportToPlace(sender, player, checkpoint.get(), null);
            return;
        }

        sender.sendMessage(error("Unknown destination."));
        return;
    }

    private void handleTargetToDestination(CommandSender sender, String targetName, String destinationName) {
        if (!sender.hasPermission(PERM_TELEPORT)) {
            sender.sendMessage(error("You do not have permission to teleport."));
            return;
        }

        Player target = findOnlinePlayer(targetName).orElse(null);
        if (target == null) {
            sender.sendMessage(error("Unknown player."));
            return;
        }

        Optional<Player> destinationPlayer = findOnlinePlayer(destinationName);
        if (destinationPlayer.isPresent()) {
            String staffNotifyDestination = shouldNotifyStaff(sender, target)
                    ? destinationPlayer.get().getName()
                    : null;
            if (!teleportPlayer(sender, target, destinationPlayer.get().getLocation(), staffNotifyDestination)) {
                return;
            }
            if (!target.equals(sender)) {
                sender.sendMessage(success("Teleported " + target.getName() + " to " + destinationPlayer.get().getName() + "."));
            }
            return;
        }

        Optional<TeleportPlace> checkpoint = resolveCheckpointForSender(sender, target, destinationName);
        if (checkpoint.isPresent()) {
            String staffNotifyDestination = shouldNotifyStaff(sender, target)
                    ? staffNotifyCheckpoint(target, checkpoint.get())
                    : null;
            if (!teleportToPlace(sender, target, checkpoint.get(), staffNotifyDestination)) {
                return;
            }
            if (!target.equals(sender)) {
                sender.sendMessage(success("Teleported " + target.getName() + " to " + checkpoint.get().name() + "."));
            }
            return;
        }

        sender.sendMessage(error("Unknown destination."));
        return;
    }

    private Optional<TeleportPlace> resolveCheckpointForSender(
            CommandSender sender, Player target, String destinationName) {
        if (!sender.hasPermission(PERM_CHECKPOINT) && !target.hasPermission(PERM_CHECKPOINT)) {
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(target.getUniqueId());
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        return teleportService.getPlace(membership.get().getKingdomId(), destinationName);
    }

    private Optional<TeleportPlace> resolveMemberCheckpoint(Player player, String destinationName) {
        if (!player.hasPermission(PERM_CHECKPOINT)) {
            return Optional.empty();
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        return teleportService.getPlace(membership.get().getKingdomId(), destinationName);
    }

    private boolean teleportToPlace(CommandSender sender, Player target, TeleportPlace place, String staffNotifyDestination) {
        World world = Bukkit.getWorld(place.worldName());
        if (world == null) {
            sender.sendMessage(error("Checkpoint world is not loaded."));
            return false;
        }
        return teleportPlayer(
                sender,
                target,
                new Location(world, place.x(), place.y(), place.z(), place.yaw(), place.pitch()),
                staffNotifyDestination);
    }

    private boolean teleportPlayer(CommandSender sender, Player target, Location destination) {
        return teleportPlayer(sender, target, destination, null);
    }

    private boolean teleportPlayer(
            CommandSender sender, Player target, Location destination, String staffNotifyDestination) {
        if (!target.teleport(destination)) {
            sender.sendMessage(error("Teleport failed."));
            return false;
        }
        if (staffNotifyDestination != null) {
            TeleportStaffNotifier.notifyCrossPlayerTeleport(sender, target, staffNotifyDestination);
        }
        if (target.equals(sender)) {
            sender.sendMessage(success("Teleported."));
        }
        return true;
    }

    private String staffNotifyCheckpoint(Player target, TeleportPlace place) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(target.getUniqueId());
        if (membership.isEmpty()) {
            return place.name();
        }
        PlayerMembership member = membership.get();
        Optional<Kingdom> kingdom = kingdomService.getKingdom(member.getKingdomId());
        String kingdomDisplayName =
                kingdom.isPresent() ? kingdom.get().getDisplayName() : member.getKingdomId();
        return TeleportStaffNotifier.formatCheckpointDestination(kingdomDisplayName, place.name());
    }

    private static boolean shouldNotifyStaff(CommandSender sender, Player target) {
        if (!(sender instanceof Player actor)) {
            return true;
        }
        return !actor.equals(target);
    }

    private Optional<String> kingdomAt(Location location) {
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                null);
        if (territory.type() == TerritoryLocation.IncomeLocation.FOREIGN_KINGDOM) {
            return territory.kingdomId();
        }
        return Optional.empty();
    }

    private static boolean isBringHereToken(String token) {
        return "here".equalsIgnoreCase(token) || "@s".equalsIgnoreCase(token);
    }

    private static boolean isCoordinateForm(String[] args) {
        if (args.length == 3 || args.length == 5) {
            return TeleportCoordinateParser.isCoordinateToken(args[0])
                    && TeleportCoordinateParser.isCoordinateToken(args[1])
                    && TeleportCoordinateParser.isCoordinateToken(args[2]);
        }
        if (args.length == 4 || args.length == 6) {
            return TeleportCoordinateParser.isCoordinateToken(args[1])
                    && TeleportCoordinateParser.isCoordinateToken(args[2])
                    && TeleportCoordinateParser.isCoordinateToken(args[3]);
        }
        return false;
    }

    private Optional<Player> findOnlinePlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return Optional.of(exact);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        sender.sendMessage(error("Operators only."));
        return false;
    }

    private String help(CommandSender sender) {
        StringBuilder builder = new StringBuilder(info("Teleport commands:"));
        if (sender.hasPermission(PERM_CHECKPOINT)) {
            builder.append("\n").append(c("&e")).append("/tp <checkpoint>");
            builder.append(c("&7")).append(" — teleport to a kingdom checkpoint");
        }
        if (sender.hasPermission(PERM_TELEPORT)) {
            builder.append("\n").append(c("&e")).append("/tp here <player>");
            builder.append(c("&7")).append(" — bring a player to you");
            builder.append("\n").append(c("&e")).append("/tp @s <player>");
            builder.append(c("&7")).append(" — alias for /tp here");
            builder.append("\n").append(c("&e")).append("/tp <player>");
            builder.append(c("&7")).append(" — teleport to a player");
            builder.append("\n").append(c("&e")).append("/tp <player> <destination>");
            builder.append(c("&7")).append(" — teleport a player to another player or checkpoint");
            builder.append("\n").append(c("&e")).append("/tp <x> <y> <z>");
            builder.append(c("&7")).append(" — teleport to coordinates (~ supported)");
        }
        if (sender.isOp()) {
            builder.append("\n").append(c("&6")).append("/tp checkpoint create <name>");
            builder.append("\n").append(c("&6")).append("/tp checkpoint create <kingdom> <name>");
            builder.append("\n").append(c("&6")).append("/tp checkpoint delete <name>");
            builder.append("\n").append(c("&6")).append("/tp checkpoint list [kingdom]");
        }
        return builder.toString();
    }

    private String format(TeleportResult result) {
        return switch (result) {
            case TeleportResult.Success success -> success(success.message());
            case TeleportResult.Failure failure -> error(failure.message());
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

    private static String formatCoord(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.UK, "%.0f", value);
        }
        return String.format(Locale.UK, "%.2f", value);
    }
}
