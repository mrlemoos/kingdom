package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.locate.LocateCheckpointResolver;
import dev.mrlemoos.kingdom.locate.LocateCompassFactory;
import dev.mrlemoos.kingdom.locate.LocateKeyParser;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.StructureSearchResult;

public final class LocateCommand {

    private static final String PERM_CHECKPOINT = LocateCommandSuggestions.PERM_CHECKPOINT;
    private static final String PERM_LOCATE = LocateCommandSuggestions.PERM_LOCATE;
    private static final int STRUCTURE_RADIUS_CHUNKS = 100;
    private static final int BIOME_RADIUS_BLOCKS = 6400;
    private static final int BIOME_HORIZONTAL_INTERVAL = 32;
    private static final int BIOME_VERTICAL_INTERVAL = 64;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final TeleportService teleportService;

    public LocateCommand(JavaPlugin plugin, KingdomService kingdomService, TeleportService teleportService) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.teleportService = teleportService;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use /locate."));
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(help(player));
            return;
        }

        if ("structure".equalsIgnoreCase(args[0])) {
            handleStructure(player, args);
            return;
        }
        if ("biome".equalsIgnoreCase(args[0])) {
            handleBiome(player, args);
            return;
        }

        handleCheckpoint(player, args[0]);
    }

    private void handleCheckpoint(Player player, String checkpointName) {
        if (!player.hasPermission(PERM_CHECKPOINT)) {
            player.sendMessage(error("You do not have permission to locate kingdom checkpoints."));
            return;
        }
        Optional<TeleportPlace> place = LocateCheckpointResolver.resolve(
                player.getUniqueId(), checkpointName, kingdomService, teleportService);
        if (place.isEmpty()) {
            player.sendMessage(error("Unknown checkpoint."));
            return;
        }

        TeleportPlace checkpoint = place.get();
        World world = Bukkit.getWorld(checkpoint.worldName());
        if (world == null) {
            player.sendMessage(error("Checkpoint world is not loaded."));
            return;
        }
        if (!world.equals(player.getWorld())) {
            player.sendMessage(error(
                    "That checkpoint is in another world. Stand in " + checkpoint.worldName() + " first."));
            return;
        }

        Location target = new Location(world, checkpoint.x(), checkpoint.y(), checkpoint.z());
        giveCompass(player, target, checkpoint.name());
    }

    private void handleStructure(Player player, String[] args) {
        if (!player.hasPermission(PERM_LOCATE)) {
            player.sendMessage(error("You do not have permission to locate structures."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(error("Usage: /locate structure <type>"));
            return;
        }

        String rawType = args[1];
        Optional<Structure> structure = LocateKeyParser.parseStructure(rawType);
        Optional<StructureType> structureType =
                structure.isEmpty() ? LocateKeyParser.parseStructureType(rawType) : Optional.empty();
        if (structure.isEmpty() && structureType.isEmpty()) {
            player.sendMessage(error("Unknown structure type."));
            return;
        }

        String label = LocateKeyParser.displayName(rawType);
        player.sendMessage(info("Searching for the nearest " + label + "..."));
        searchAsync(
                player,
                origin -> {
                    if (structure.isPresent()) {
                        StructureSearchResult result = origin.getWorld()
                                .locateNearestStructure(origin, structure.get(), STRUCTURE_RADIUS_CHUNKS, false);
                        return result != null ? result.getLocation() : null;
                    }
                    StructureSearchResult result = origin.getWorld()
                            .locateNearestStructure(origin, structureType.get(), STRUCTURE_RADIUS_CHUNKS, false);
                    return result != null ? result.getLocation() : null;
                },
                label);
    }

    private void handleBiome(Player player, String[] args) {
        if (!player.hasPermission(PERM_LOCATE)) {
            player.sendMessage(error("You do not have permission to locate biomes."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(error("Usage: /locate biome <type>"));
            return;
        }

        Optional<Biome> biome = LocateKeyParser.parseBiome(args[1]);
        if (biome.isEmpty()) {
            player.sendMessage(error("Unknown biome."));
            return;
        }

        String label = LocateKeyParser.displayName(args[1]);
        player.sendMessage(info("Searching for the nearest " + label + "..."));
        Biome resolved = biome.get();
        searchAsync(
                player,
                origin -> {
                    BiomeSearchResult result = origin.getWorld()
                            .locateNearestBiome(
                                    origin,
                                    BIOME_RADIUS_BLOCKS,
                                    BIOME_HORIZONTAL_INTERVAL,
                                    BIOME_VERTICAL_INTERVAL,
                                    resolved);
                    return result != null ? result.getLocation() : null;
                },
                label);
    }

    @FunctionalInterface
    private interface LocateSearch {
        Location search(Location origin);
    }

    private void searchAsync(Player player, LocateSearch search, String label) {
        Location origin = player.getLocation().clone();
        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {
                            Location found = search.search(origin);
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (!player.isOnline()) {
                                    return;
                                }
                                if (found == null) {
                                    player.sendMessage(error("Could not find a nearby " + label + "."));
                                    return;
                                }
                                giveCompass(player, found, label);
                            });
                        });
    }

    private void giveCompass(Player player, Location target, String label) {
        ItemStack compass = LocateCompassFactory.create(target, label);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(compass);
        for (ItemStack dropped : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), dropped);
        }
        player.sendMessage(success("Compass now points to " + label + "."));
    }

    private String help(Player player) {
        StringBuilder builder = new StringBuilder(info("Locate commands:"));
        if (player.hasPermission(PERM_CHECKPOINT)) {
            builder.append("\n").append(c("&e")).append("/locate <checkpoint>");
            builder.append(c("&7")).append(" — compass to a kingdom checkpoint");
        }
        if (player.hasPermission(PERM_LOCATE)) {
            builder.append("\n").append(c("&e")).append("/locate structure <type>");
            builder.append(c("&7")).append(" — compass to the nearest structure");
            builder.append("\n").append(c("&e")).append("/locate biome <type>");
            builder.append(c("&7")).append(" — compass to the nearest biome");
        }
        return builder.toString();
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
