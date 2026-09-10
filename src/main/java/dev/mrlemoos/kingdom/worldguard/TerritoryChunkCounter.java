package dev.mrlemoos.kingdom.worldguard;

import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Counts the two-dimensional union of WorldGuard region bounds in chunks. */
final class TerritoryChunkCounter {

    private TerritoryChunkCounter() {}

    static long count(Collection<RegionBounds> bounds) {
        List<ChunkRectangle> rectangles = bounds.stream().map(ChunkRectangle::from).toList();
        List<Long> xEdges = rectangles.stream()
                .flatMap(rectangle -> java.util.stream.Stream.of(rectangle.minX(), rectangle.maxXExclusive()))
                .distinct()
                .sorted()
                .toList();
        long total = 0;
        for (int index = 0; index + 1 < xEdges.size(); index++) {
            long minX = xEdges.get(index);
            long maxXExclusive = xEdges.get(index + 1);
            List<ChunkInterval> zIntervals = rectangles.stream()
                    .filter(rectangle -> rectangle.minX() <= minX && rectangle.maxXExclusive() >= maxXExclusive)
                    .map(rectangle -> new ChunkInterval(rectangle.minZ(), rectangle.maxZExclusive()))
                    .sorted(Comparator.comparingLong(ChunkInterval::min))
                    .toList();
            total = Math.addExact(total, Math.multiplyExact(maxXExclusive - minX, unionLength(zIntervals)));
        }
        return total;
    }

    private static long unionLength(List<ChunkInterval> intervals) {
        if (intervals.isEmpty()) {
            return 0;
        }
        long total = 0;
        long min = intervals.getFirst().min();
        long maxExclusive = intervals.getFirst().maxExclusive();
        for (int index = 1; index < intervals.size(); index++) {
            ChunkInterval next = intervals.get(index);
            if (next.min() > maxExclusive) {
                total = Math.addExact(total, maxExclusive - min);
                min = next.min();
                maxExclusive = next.maxExclusive();
            } else {
                maxExclusive = Math.max(maxExclusive, next.maxExclusive());
            }
        }
        return Math.addExact(total, maxExclusive - min);
    }

    private record ChunkRectangle(long minX, long maxXExclusive, long minZ, long maxZExclusive) {
        private static ChunkRectangle from(RegionBounds bounds) {
            return new ChunkRectangle(
                    Math.floorDiv(bounds.minX(), 16),
                    Math.floorDiv(bounds.maxX(), 16) + 1L,
                    Math.floorDiv(bounds.minZ(), 16),
                    Math.floorDiv(bounds.maxZ(), 16) + 1L);
        }
    }

    private record ChunkInterval(long min, long maxExclusive) {}
}
