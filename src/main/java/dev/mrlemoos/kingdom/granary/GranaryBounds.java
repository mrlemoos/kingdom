package dev.mrlemoos.kingdom.granary;

/**
 * The box a region stands in, corner to corner and inclusive of both. The granary knows regions by
 * nothing else: enough to walk the blocks and enough to ask whether one box encloses another.
 */
public record GranaryBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public GranaryBounds {
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

    /** How many blocks the box holds; the whole cost of a stocktake. */
    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /** Whether the block at these coordinates stands in the box, edges counting as inside. */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** Whether this box wholly encloses {@code inner}, edges counting as inside. */
    public boolean contains(GranaryBounds inner) {
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
}
