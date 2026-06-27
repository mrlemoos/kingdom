package dev.leo.kingdom.economy.wealth;

import dev.leo.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.Objects;

public final class TerritoryWealthScanCursor {

    @FunctionalInterface
    public interface BlockCoordinateConsumer {
        void accept(int x, int y, int z);
    }

    private int x;
    private int y;
    private int z;

    private TerritoryWealthScanCursor(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static TerritoryWealthScanCursor start(RegionBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        return new TerritoryWealthScanCursor(bounds.minX(), bounds.minY(), bounds.minZ());
    }

    public static long blockVolume(RegionBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        long xSpan = (long) bounds.maxX() - bounds.minX() + 1L;
        long ySpan = (long) bounds.maxY() - bounds.minY() + 1L;
        long zSpan = (long) bounds.maxZ() - bounds.minZ() + 1L;
        return xSpan * ySpan * zSpan;
    }

    public boolean isComplete(RegionBounds bounds) {
        return x > bounds.maxX();
    }

    public int advance(RegionBounds bounds, int blockBudget, BlockCoordinateConsumer consumer) {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(consumer, "consumer");
        if (blockBudget <= 0 || isComplete(bounds)) {
            return 0;
        }

        int processed = 0;
        while (processed < blockBudget && !isComplete(bounds)) {
            consumer.accept(x, y, z);
            step(bounds);
            processed++;
        }
        return processed;
    }

    private void step(RegionBounds bounds) {
        y++;
        if (y > bounds.maxY()) {
            y = bounds.minY();
            z++;
            if (z > bounds.maxZ()) {
                z = bounds.minZ();
                x++;
            }
        }
    }
}
