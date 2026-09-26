package dev.mrlemoos.kingdom.police;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Who built that golem. Vanilla names no builder when an iron golem rises, so the head that
 * completed it is remembered as it is placed and the golem is credited to the nearest recent one.
 */
public final class GolemBuilderMatcher {

    /** How long a placed head waits for its golem. */
    static final long WINDOW_MS = 1_000L;

    /** A golem rises at the foot of its body, two blocks under the head; allow a little slack. */
    private static final int MAX_DISTANCE_SQ = 9;

    private record Head(UUID placer, String world, int x, int y, int z, long at) {}

    private final List<Head> heads = new ArrayList<>();

    public void headPlaced(UUID placer, String world, int x, int y, int z, long nowMs) {
        prune(nowMs);
        heads.add(new Head(placer, world, x, y, z, nowMs));
    }

    /** The placer of the nearest fresh head to a golem risen here, spent once matched. */
    public Optional<UUID> builderOf(String world, int x, int y, int z, long nowMs) {
        prune(nowMs);
        Head nearest = null;
        int nearestSq = Integer.MAX_VALUE;
        for (Head head : heads) {
            if (!head.world().equals(world)) {
                continue;
            }
            int dx = head.x() - x;
            int dy = head.y() - y;
            int dz = head.z() - z;
            int sq = dx * dx + dy * dy + dz * dz;
            if (sq <= MAX_DISTANCE_SQ && sq < nearestSq) {
                nearest = head;
                nearestSq = sq;
            }
        }
        if (nearest == null) {
            return Optional.empty();
        }
        heads.remove(nearest);
        return Optional.of(nearest.placer());
    }

    private void prune(long nowMs) {
        heads.removeIf(head -> nowMs - head.at() > WINDOW_MS);
    }
}
