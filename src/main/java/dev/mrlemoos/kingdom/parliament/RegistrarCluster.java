package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Face-adjacent flood-fill of the registrar shelf cluster from a monarch-set anchor.
 */
public final class RegistrarCluster {

    public static final int MAX_SHELVES = 64;

    private static final int[][] FACE_DELTAS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private RegistrarCluster() {}

    public static List<RegistrarSite> floodFill(
            RegistrarSite anchor, Predicate<RegistrarSite> isShelf, int maxShelves) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(isShelf, "isShelf");
        if (maxShelves < 1) {
            return List.of();
        }
        if (!isShelf.test(anchor)) {
            return List.of();
        }

        List<RegistrarSite> cluster = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ArrayDeque<RegistrarSite> queue = new ArrayDeque<>();
        queue.add(anchor);
        seen.add(key(anchor));

        while (!queue.isEmpty() && cluster.size() < maxShelves) {
            RegistrarSite current = queue.removeFirst();
            cluster.add(current);
            if (cluster.size() >= maxShelves) {
                break;
            }
            for (int[] delta : FACE_DELTAS) {
                RegistrarSite neighbour = RegistrarSite.of(
                        current.worldName(),
                        current.blockX() + delta[0],
                        current.blockY() + delta[1],
                        current.blockZ() + delta[2]);
                if (!seen.add(key(neighbour))) {
                    continue;
                }
                if (isShelf.test(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return List.copyOf(cluster);
    }

    /**
     * Kingdom whose registrar anchor flood-fill contains the block. Empty when none or more than one
     * claim it.
     */
    public static Optional<String> resolveOwner(
            RegistrarSite block,
            Map<String, RegistrarSite> anchorsByKingdomId,
            Predicate<RegistrarSite> isShelf) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(anchorsByKingdomId, "anchorsByKingdomId");
        Objects.requireNonNull(isShelf, "isShelf");

        String match = null;
        for (Map.Entry<String, RegistrarSite> entry : anchorsByKingdomId.entrySet()) {
            RegistrarSite anchor = entry.getValue();
            if (anchor == null || !anchor.worldName().equals(block.worldName())) {
                continue;
            }
            List<RegistrarSite> cluster = floodFill(anchor, isShelf, MAX_SHELVES);
            boolean contains = false;
            for (RegistrarSite site : cluster) {
                if (sameBlock(site, block)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                continue;
            }
            if (match != null) {
                return Optional.empty();
            }
            match = entry.getKey();
        }
        return Optional.ofNullable(match);
    }

    private static boolean sameBlock(RegistrarSite a, RegistrarSite b) {
        return a.worldName().equals(b.worldName())
                && a.blockX() == b.blockX()
                && a.blockY() == b.blockY()
                && a.blockZ() == b.blockZ();
    }

    private static String key(RegistrarSite site) {
        return site.worldName() + '|' + site.blockX() + '|' + site.blockY() + '|' + site.blockZ();
    }
}
