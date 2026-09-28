package dev.mrlemoos.kingdom.hearth;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The world's side of the hearth: the thin shell that walks a kingdom's loaded territory, finds the
 * lit campfires with a container set against them, and hands them to the day's reckoning. Nothing is
 * recorded — the sweep asks the world afresh each day.
 */
public final class BukkitHearthScan {

    public record Supply(int fuel, int hearths, int lowHearths, List<String> lowLocations, boolean complete) {}

    private BukkitHearthScan() {}

    /** Every hearth standing in {@code regionId}'s loaded chunks, with the fuel its container holds. */
    public static List<HearthSite> hearthsIn(World world, String regionId, HearthConfig config) {
        List<HearthSite> hearths = new ArrayList<>();
        if (world == null || regionId == null || regionId.isBlank()) {
            return hearths;
        }
        String normalised = Kingdom.normaliseId(regionId);
        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState state : chunk.getTileEntities(false)) {
                Block block = state.getBlock();
                if (!isLitCampfire(block)) {
                    continue;
                }
                if (!isInRegion(world.getName(), normalised, block)) {
                    continue;
                }
                Container container = adjacentContainer(block);
                if (container == null) {
                    continue;
                }
                hearths.add(new HearthSite(
                        block.getX(),
                        block.getY(),
                        block.getZ(),
                        new ContainerFuelStore(container.getInventory(), config)));
            }
        }
        return hearths;
    }

    /** Current fuel in linked territory; complete only when every territory chunk is loaded. */
    public static Supply supplyIn(World world, List<String> regionIds, HearthConfig config) {
        if (world == null || regionIds == null || regionIds.isEmpty()) {
            return new Supply(0, 0, 0, List.of(), false);
        }
        List<WorldGuardBridge.RegionBounds> bounds = new ArrayList<>();
        for (String regionId : regionIds) {
            var found = WorldGuardBridge.regionBounds(world.getName(), regionId);
            if (found.isEmpty()) return new Supply(0, 0, 0, List.of(), false);
            bounds.add(found.get());
        }
        var chunkCount = WorldGuardBridge.territoryChunkCount(world.getName(), regionIds);
        if (chunkCount.isEmpty()) return new Supply(0, 0, 0, List.of(), false);
        int loadedTerritoryChunks = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            int x = chunk.getX();
            int z = chunk.getZ();
            if (bounds.stream().anyMatch(box -> Math.floorDiv(box.minX(), 16) <= x
                    && Math.floorDiv(box.maxX(), 16) >= x
                    && Math.floorDiv(box.minZ(), 16) <= z
                    && Math.floorDiv(box.maxZ(), 16) >= z)) {
                loadedTerritoryChunks++;
            }
        }
        boolean complete = loadedTerritoryChunks == chunkCount.getAsLong();
        List<HearthSite> hearths = regionIds.stream()
                .flatMap(region -> hearthsIn(world, region, config).stream())
                .toList();
        int fuel = hearths.stream().mapToInt(site -> site.fuel().fuelCount()).sum();
        int lowHearths = (int) hearths.stream().filter(site -> site.fuel().fuelCount() < config.fuelPerDay()).count();
        List<String> lowLocations = hearths.stream()
                .filter(site -> site.fuel().fuelCount() < config.fuelPerDay())
                .map(site -> site.x() + ", " + site.y() + ", " + site.z())
                .toList();
        return new Supply(fuel, hearths.size(), lowHearths, lowLocations, complete);
    }

    /** Whether the block at these coordinates is a burning campfire of either kind. */
    private static boolean isLitCampfire(Block block) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Lightable lightable)) {
            return false;
        }
        String key = block.getType().name();
        if (!"CAMPFIRE".equals(key) && !"SOUL_CAMPFIRE".equals(key)) {
            return false;
        }
        return lightable.isLit();
    }

    /** The container set face to face against the campfire, if there is one; never a diagonal. */
    public static Container adjacentContainer(Block campfire) {
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[] {
            org.bukkit.block.BlockFace.NORTH,
            org.bukkit.block.BlockFace.SOUTH,
            org.bukkit.block.BlockFace.EAST,
            org.bukkit.block.BlockFace.WEST,
            org.bukkit.block.BlockFace.UP,
            org.bukkit.block.BlockFace.DOWN
        }) {
            BlockState state = campfire.getRelative(face).getState();
            if (state instanceof Container container) {
                return container;
            }
        }
        return null;
    }

    private static boolean isInRegion(String worldName, String normalisedRegionId, Block block) {
        List<String> found = WorldGuardBridge.regionsAt(worldName, block.getX(), block.getY(), block.getZ());
        return found.stream().anyMatch(region -> Kingdom.normaliseId(region).equals(normalisedRegionId));
    }

    /** A hearth's container as the day's burning sees it: so many fuel items, taken from the top. */
    private record ContainerFuelStore(Inventory inventory, HearthConfig config) implements HearthFuelStore {

        @Override
        public int fuelCount() {
            int total = 0;
            for (ItemStack stack : inventory.getContents()) {
                if (stack != null && config.isFuel(stack.getType().name())) {
                    total += stack.getAmount();
                }
            }
            return total;
        }

        @Override
        public void consumeFuel(int amount) {
            int remaining = amount;
            ItemStack[] contents = inventory.getContents();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack stack = contents[slot];
                if (stack == null || !config.isFuel(stack.getType().name())) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getAmount());
                remaining -= taken;
                if (taken >= stack.getAmount()) {
                    inventory.setItem(slot, null);
                } else {
                    stack.setAmount(stack.getAmount() - taken);
                    inventory.setItem(slot, stack);
                }
            }
        }
    }
}
