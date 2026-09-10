package dev.mrlemoos.kingdom.war.capital;

/**
 * Inclusive block box of a WorldGuard region, used to test territory containment and whether a
 * chunk sits in the capital subregion.
 */
public record CapitalRegionBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public CapitalRegionBox {
        int lowX = Math.min(minX, maxX);
        int lowY = Math.min(minY, maxY);
        int lowZ = Math.min(minZ, maxZ);
        int highX = Math.max(minX, maxX);
        int highY = Math.max(minY, maxY);
        int highZ = Math.max(minZ, maxZ);
        minX = lowX;
        minY = lowY;
        minZ = lowZ;
        maxX = highX;
        maxY = highY;
        maxZ = highZ;
    }

    public boolean contains(CapitalRegionBox inner) {
        if (inner == null) {
            return false;
        }
        return inner.minX >= minX
                && inner.minY >= minY
                && inner.minZ >= minZ
                && inner.maxX <= maxX
                && inner.maxY <= maxY
                && inner.maxZ <= maxZ;
    }

    /** True when the chunk's 16×16 column overlaps this box in X/Z. */
    public boolean containsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX * 16;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ * 16;
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMaxX >= minX && chunkMinX <= maxX && chunkMaxZ >= minZ && chunkMinZ <= maxZ;
    }
}
