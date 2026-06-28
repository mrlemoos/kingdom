package dev.mrlemoos.kingdom.economy.wealth;

import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.World;

public final class TerritoryWealthScanSession {

    private final String kingdomId;
    private final World world;
    private final RegionBounds bounds;
    private final TerritoryWealthCounts counts = new TerritoryWealthCounts();
    private final TerritoryWealthScanCursor cursor;

    public TerritoryWealthScanSession(String kingdomId, World world, RegionBounds bounds) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.world = Objects.requireNonNull(world, "world");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.cursor = TerritoryWealthScanCursor.start(bounds);
    }

    public String kingdomId() {
        return kingdomId;
    }

    public TerritoryWealthCounts counts() {
        return counts;
    }

    public boolean isComplete() {
        return cursor.isComplete(bounds);
    }

    public int advance(int blockBudget) {
        return cursor.advance(bounds, blockBudget, this::countWealthBlockAt);
    }

    private void countWealthBlockAt(int x, int y, int z) {
        Material material = world.getBlockAt(x, y, z).getType();
        WealthBlockType.fromMaterial(material).ifPresent(type -> counts.adjust(type, 1));
    }
}
