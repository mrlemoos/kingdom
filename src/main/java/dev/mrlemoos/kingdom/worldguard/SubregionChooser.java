package dev.mrlemoos.kingdom.worldguard;

import dev.mrlemoos.kingdom.war.capital.CapitalRegionBox;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Which WorldGuard region a foundation stone for a region-shaped site links: the smallest one around
 * the stone that lies wholly inside the realm's territory and is not the territory itself. The
 * capital's stone links its war region this way, and the granary's hay bale its granary.
 */
public final class SubregionChooser {

    /** A WorldGuard region by id and bounds. */
    public record Candidate(String regionId, CapitalRegionBox bounds) {}

    private SubregionChooser() {}

    /**
     * @param regions the regions to choose among, typically those at the stone's block
     * @param territory the kingdom's linked territory regions
     */
    public static Optional<Candidate> smallest(
            int x, int y, int z, Collection<Candidate> regions, Collection<Candidate> territory) {
        return regions.stream()
                .filter(region -> region.bounds().containsBlock(x, y, z))
                .filter(region -> territory.stream()
                        .noneMatch(linked -> linked.regionId().equalsIgnoreCase(region.regionId())))
                .filter(region -> territory.stream().anyMatch(linked -> linked.bounds().contains(region.bounds())))
                .min(Comparator.comparingLong((Candidate region) -> region.bounds().volume())
                        .thenComparing(Candidate::regionId));
    }

    /** The id of {@link #smallest}; empty when no region qualifies. */
    public static Optional<String> smallestAround(
            int x, int y, int z, Collection<Candidate> regions, Collection<Candidate> territory) {
        Optional<Candidate> smallest = smallest(x, y, z, regions, territory);
        return smallest.isPresent() ? Optional.of(smallest.get().regionId()) : Optional.empty();
    }

    /** The regions WorldGuard can measure in a world, by id; those it cannot are left out. */
    public static List<Candidate> candidates(String worldName, Iterable<String> regionIds) {
        List<Candidate> candidates = new ArrayList<>();
        for (String regionId : regionIds) {
            Optional<RegionBounds> bounds = WorldGuardBridge.regionBounds(worldName, regionId);
            if (bounds.isPresent()) {
                RegionBounds box = bounds.get();
                candidates.add(new Candidate(
                        regionId,
                        new CapitalRegionBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())));
            }
        }
        return candidates;
    }
}
