package dev.mrlemoos.kingdom.command;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportResult;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class TpCommand implements CommandExecutor, TabCompleter {

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "checkpoint".equalsIgnoreCase(args[0])) {
            return handleCheckpoint(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return handleVanillaTeleport(sender, args);
    }

    private boolean handleCheckpoint(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(error("Usage: /tp checkpoint <create|delete|list> ..."));
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCheckpointCreate(sender, args);
            case "delete" -> handleCheckpointDelete(sender, args);
            case "list" -> handleCheckpointList(sender, args);
            default -> {
                sender.sendMessage(error("Usage: /tp checkpoint <create|delete|list> ..."));
                yield true;
            }
        };
    }

    private boolean handleCheckpointCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may create checkpoints."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /tp checkpoint create <name>"));
            sender.sendMessage(error("       /tp checkpoint create <kingdom> <name>"));
            return true;
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
                return true;
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
        return true;
    }

    private boolean handleCheckpointDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /tp checkpoint delete <name>"));
            sender.sendMessage(error("       /tp checkpoint delete <kingdom> <name>"));
            return true;
        }

        String kingdomId;
        String name;
        if (args.length >= 3 && kingdomService.getKingdom(args[1]).isPresent()) {
            kingdomId = Kingdom.normaliseId(args[1]);
            name = args[2];
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(error("Specify the kingdom when using the console."));
                return true;
            }
            Optional<String> territoryKingdom = kingdomAt(player.getLocation());
            if (territoryKingdom.isEmpty()) {
                sender.sendMessage(error("Stand in a kingdom territory or specify the kingdom."));
                return true;
            }
            kingdomId = territoryKingdom.get();
            name = args[1];
        }

        TeleportResult result = teleportService.deletePlace(kingdomId, name);
        sender.sendMessage(format(result));
        if (result instanceof TeleportResult.Success) {
            store.saveFrom(kingdomService);
        }
        return true;
    }

    private boolean handleCheckpointList(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String kingdomId = Kingdom.normaliseId(args[1]);
            Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
            if (kingdom.isEmpty()) {
                sender.sendMessage(error("Unknown kingdom."));
                return true;
            }
            listKingdomCheckpoints(sender, kingdom.get());
            return true;
        }

        List<Kingdom> kingdoms = kingdomService.listKingdoms().stream()
                .filter(kingdom -> !kingdom.getTeleportsView().isEmpty())
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
        if (kingdoms.isEmpty()) {
            sender.sendMessage(info("No checkpoints defined."));
            return true;
        }
        for (Kingdom kingdom : kingdoms) {
            listKingdomCheckpoints(sender, kingdom);
        }
        return true;
    }

    private void listKingdomCheckpoints(CommandSender sender, Kingdom kingdom) {
        List<TeleportPlace> places = teleportService.listPlaces(kingdom.getId());
        if (places.isEmpty()) {
            return;
        }
        sender.sendMessage(info(kingdom.getDisplayName() + " checkpoints:"));
        for (TeleportPlace place : places) {
            sender.sendMessage(ChatColor.GRAY + "  " + place.name() + ChatColor.WHITE + " — "
                    + place.worldName() + " "
                    + formatCoord(place.x()) + " "
                    + formatCoord(place.y()) + " "
                    + formatCoord(place.z()));
        }
    }

    private boolean handleVanillaTeleport(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(help(sender));
            return true;
        }

        if (isCoordinateForm(args)) {
            return handleCoordinateTeleport(sender, args);
        }

        if (args.length == 1) {
            return handleSelfToDestination(sender, args[0]);
        }

        if (args.length == 2) {
            return handleTargetToDestination(sender, args[0], args[1]);
        }

        sender.sendMessage(error("Unrecognised teleport command."));
        return true;
    }

    private boolean handleCoordinateTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_TELEPORT)) {
            sender.sendMessage(error("You do not have permission to teleport."));
            return true;
        }

        Player target;
        int coordStart;
        if (args.length == 3 || args.length == 5) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(error("Specify a player when teleporting from the console."));
                return true;
            }
            target = player;
            coordStart = 0;
        } else if (args.length == 4 || args.length == 6) {
            target = findOnlinePlayer(args[0]).orElse(null);
            if (target == null) {
                sender.sendMessage(error("Unknown player."));
                return true;
            }
            coordStart = 1;
        } else {
            sender.sendMessage(error("Unrecognised coordinate teleport."));
            return true;
        }

        World world = target.getWorld();
        Location destination;
        try {
            destination = TeleportCoordinateParser.parseLocation(world, args, coordStart, target.getLocation());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(error("Invalid coordinates."));
            return true;
        }

        if (!teleportPlayer(sender, target, destination)) {
            return true;
        }
        if (!target.equals(sender)) {
            sender.sendMessage(success("Teleported " + target.getName() + "."));
        }
        return true;
    }

    private boolean handleSelfToDestination(CommandSender sender, String destinationName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Specify a player or coordinates when using the console."));
            return true;
        }

        Optional<Player> online = findOnlinePlayer(destinationName);
        if (online.isPresent()) {
            if (!sender.hasPermission(PERM_TELEPORT)) {
                sender.sendMessage(error("You do not have permission to teleport."));
                return true;
            }
            return teleportPlayer(sender, player, online.get().getLocation());
        }

        Optional<TeleportPlace> checkpoint = resolveMemberCheckpoint(player, destinationName);
        if (checkpoint.isPresent()) {
            return teleportToPlace(sender, player, checkpoint.get());
        }

        sender.sendMessage(error("Unknown destination."));
        return true;
    }

    private boolean handleTargetToDestination(CommandSender sender, String targetName, String destinationName) {
        if (!sender.hasPermission(PERM_TELEPORT)) {
            sender.sendMessage(error("You do not have permission to teleport."));
            return true;
        }

        Player target = findOnlinePlayer(targetName).orElse(null);
        if (target == null) {
            sender.sendMessage(error("Unknown player."));
            return true;
        }

        Optional<Player> destinationPlayer = findOnlinePlayer(destinationName);
        if (destinationPlayer.isPresent()) {
            if (!teleportPlayer(sender, target, destinationPlayer.get().getLocation())) {
                return true;
            }
            if (!target.equals(sender)) {
                sender.sendMessage(success("Teleported " + target.getName() + " to " + destinationPlayer.get().getName() + "."));
            }
            return true;
        }

        Optional<TeleportPlace> checkpoint = resolveCheckpointForSender(sender, target, destinationName);
        if (checkpoint.isPresent()) {
            if (!teleportToPlace(sender, target, checkpoint.get())) {
                return true;
            }
            if (!target.equals(sender)) {
                sender.sendMessage(success("Teleported " + target.getName() + " to " + checkpoint.get().name() + "."));
            }
            return true;
        }

        sender.sendMessage(error("Unknown destination."));
        return true;
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

    private boolean teleportToPlace(CommandSender sender, Player target, TeleportPlace place) {
        World world = Bukkit.getWorld(place.worldName());
        if (world == null) {
            sender.sendMessage(error("Checkpoint world is not loaded."));
            return false;
        }
        return teleportPlayer(sender, target, new Location(world, place.x(), place.y(), place.z(), place.yaw(), place.pitch()));
    }

    private boolean teleportPlayer(CommandSender sender, Player target, Location destination) {
        if (!target.teleport(destination)) {
            sender.sendMessage(error("Teleport failed."));
            return false;
        }
        if (target.equals(sender)) {
            sender.sendMessage(success("Teleported."));
        }
        return true;
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
            builder.append("\n").append(ChatColor.YELLOW).append("/tp <checkpoint>");
            builder.append(ChatColor.GRAY).append(" — teleport to a kingdom checkpoint");
        }
        if (sender.hasPermission(PERM_TELEPORT)) {
            builder.append("\n").append(ChatColor.YELLOW).append("/tp <player>");
            builder.append(ChatColor.GRAY).append(" — teleport to a player");
            builder.append("\n").append(ChatColor.YELLOW).append("/tp <player> <destination>");
            builder.append(ChatColor.GRAY).append(" — teleport a player to another player or checkpoint");
            builder.append("\n").append(ChatColor.YELLOW).append("/tp <x> <y> <z>");
            builder.append(ChatColor.GRAY).append(" — teleport to coordinates (~ supported)");
        }
        if (sender.isOp()) {
            builder.append("\n").append(ChatColor.GOLD).append("/tp checkpoint create <name>");
            builder.append("\n").append(ChatColor.GOLD).append("/tp checkpoint create <kingdom> <name>");
            builder.append("\n").append(ChatColor.GOLD).append("/tp checkpoint delete <name>");
            builder.append("\n").append(ChatColor.GOLD).append("/tp checkpoint list [kingdom]");
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
        return ChatColor.GREEN + message;
    }

    private String error(String message) {
        return ChatColor.RED + message;
    }

    private String info(String message) {
        return ChatColor.AQUA + message;
    }

    private static String formatCoord(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.UK, "%.0f", value);
        }
        return String.format(Locale.UK, "%.2f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.isOp()) {
                options.add("checkpoint");
            }
            if (sender.hasPermission(PERM_TELEPORT)) {
                options.addAll(onlineNames());
            }
            if (sender instanceof Player player && sender.hasPermission(PERM_CHECKPOINT)) {
                kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> options.addAll(
                        teleportService.listPlaces(membership.getKingdomId()).stream()
                                .map(TeleportPlace::name)
                                .toList()));
            }
            return filter(options, args[0]);
        }

        if ("checkpoint".equalsIgnoreCase(args[0])) {
            if (!sender.isOp()) {
                return List.of();
            }
            if (args.length == 2) {
                return filter(List.of("create", "delete", "list"), args[1]);
            }
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 3) {
                return switch (sub) {
                    case "create", "delete" -> filter(kingdomIds(), args[2]);
                    case "list" -> filter(kingdomIds(), args[2]);
                    default -> List.of();
                };
            }
            if (args.length == 4) {
                String kingdomId = kingdomService.getKingdom(args[2]).map(Kingdom::getId).orElse(null);
                if (kingdomId == null) {
                    return List.of();
                }
                return filter(
                        teleportService.listPlaces(kingdomId).stream()
                                .map(TeleportPlace::name)
                                .toList(),
                        args[3]);
            }
        }

        if (sender.hasPermission(PERM_TELEPORT)) {
            if (args.length == 2 && !TeleportCoordinateParser.isCoordinateToken(args[0])) {
                List<String> destinations = new ArrayList<>(onlineNames());
                if (sender instanceof Player player) {
                    kingdomService.getMembership(player.getUniqueId()).ifPresent(membership -> destinations.addAll(
                            teleportService.listPlaces(membership.getKingdomId()).stream()
                                    .map(TeleportPlace::name)
                                    .toList()));
                }
                return filter(destinations, args[1]);
            }
        }

        return List.of();
    }

    private List<String> kingdomIds() {
        return kingdomService.listKingdoms().stream().map(Kingdom::getId).sorted().toList();
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
