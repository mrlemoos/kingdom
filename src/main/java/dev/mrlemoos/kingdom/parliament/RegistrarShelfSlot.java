package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure next-slot choice for registrar shelving on a live face-connected cluster. */
public final class RegistrarShelfSlot {

    private static final int[][] FACE_DELTAS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}
    };

    private RegistrarShelfSlot() {}

    public static RegistrarShelfWriter.ShelfPlacement next(
            RegistrarSite anchor,
            List<RegistrarSite> cluster,
            Map<RegistrarSite, Set<Integer>> occupiedSlotsByShelf,
            int slotsPerShelf) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(occupiedSlotsByShelf, "occupiedSlotsByShelf");
        if (slotsPerShelf < 1) {
            throw new IllegalArgumentException("slotsPerShelf must be positive");
        }

        for (RegistrarSite site : cluster) {
            Set<Integer> occupied = occupiedSlotsByShelf.getOrDefault(site, Set.of());
            for (int slot = 0; slot < slotsPerShelf; slot++) {
                if (!occupied.contains(slot)) {
                    return new RegistrarShelfWriter.ShelfPlacement(site, slot);
                }
            }
        }

        Set<String> existing = new HashSet<>();
        for (RegistrarSite site : cluster) {
            existing.add(key(site));
        }
        List<RegistrarSite> searchFrom = cluster.isEmpty() ? List.of(anchor) : cluster;
        for (RegistrarSite member : searchFrom) {
            for (int[] delta : FACE_DELTAS) {
                RegistrarSite candidate = RegistrarSite.of(
                        member.worldName(),
                        member.blockX() + delta[0],
                        member.blockY() + delta[1],
                        member.blockZ() + delta[2]);
                if (!existing.contains(key(candidate))) {
                    return new RegistrarShelfWriter.ShelfPlacement(candidate, 0);
                }
            }
        }
        return new RegistrarShelfWriter.ShelfPlacement(
                RegistrarSite.of(anchor.worldName(), anchor.blockX() + 1, anchor.blockY(), anchor.blockZ()),
                0);
    }

    private static String key(RegistrarSite site) {
        return site.worldName() + '|' + site.blockX() + '|' + site.blockY() + '|' + site.blockZ();
    }
}
