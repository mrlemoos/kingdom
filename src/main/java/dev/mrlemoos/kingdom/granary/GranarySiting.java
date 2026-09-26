package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.war.capital.CapitalRegionBox;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser.Candidate;
import java.util.Collection;
import java.util.Optional;

/**
 * Who may site a kingdom's granary, and where it may stand: the King or Queen of that kingdom, or
 * an operator, and only over a region lying inside the kingdom's own linked territory.
 */
public final class GranarySiting {

    /** The reason a granary may not be linked or released, or {@link #ALLOWED} when it may. */
    public enum Verdict {
        ALLOWED,
        NOT_THE_CROWN,
        WORLDGUARD_ABSENT,
        NO_TERRITORY,
        UNKNOWN_REGION,
        OUTSIDE_TERRITORY,
        TOO_LARGE,
        NO_GRANARY,
        NO_REGION_AROUND
    }

    /**
     * What came of laying the granary's hay bale: the verdict, and the region it links when allowed.
     *
     * @param regionId the region linked; empty unless the verdict is {@link Verdict#ALLOWED}
     */
    public record Stone(Verdict verdict, Optional<String> regionId) {

        static Stone refused(Verdict verdict) {
            return new Stone(verdict, Optional.empty());
        }
    }

    /**
     * The largest box a granary may occupy, a hundred blocks on every side. The stocktake walks the
     * region block by block, so the Crown is refused an unwalkable region at the moment it links one
     * rather than left to find out from a stalled {@code /kingdom info}.
     */
    public static final long MAX_GRANARY_VOLUME = 1_000_000L;

    private GranarySiting() {}

    /** Only a reigning King or Queen may site a granary; an operator may do it for them. */
    public static boolean canSite(NobleRank rank, boolean operator) {
        return operator || rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    /**
     * @param rank the actor's rank in their own kingdom, or null when they hold no title
     * @param operator whether the actor is a server operator
     * @param worldGuardAvailable whether WorldGuard is installed and answering
     * @param territoryRegionId the kingdom's linked territory region, or null when it has none
     * @param territoryBounds the box that territory region stands in, if WorldGuard knows it
     * @param granaryBounds the box the offered granary region stands in, if WorldGuard knows it
     */
    public static Verdict evaluateLink(
            NobleRank rank,
            boolean operator,
            boolean worldGuardAvailable,
            String territoryRegionId,
            Optional<GranaryBounds> territoryBounds,
            Optional<GranaryBounds> granaryBounds) {
        if (!canSite(rank, operator)) {
            return Verdict.NOT_THE_CROWN;
        }
        if (!worldGuardAvailable) {
            return Verdict.WORLDGUARD_ABSENT;
        }
        if (territoryRegionId == null || territoryRegionId.isBlank()) {
            return Verdict.NO_TERRITORY;
        }
        if (granaryBounds.isEmpty()) {
            return Verdict.UNKNOWN_REGION;
        }
        if (territoryBounds.isEmpty()) {
            return Verdict.NO_TERRITORY;
        }
        if (granaryBounds.get().volume() > MAX_GRANARY_VOLUME) {
            return Verdict.TOO_LARGE;
        }
        // ponytail: containment is bounding box against bounding box, the only measure the
        // WorldGuard bridge gives cheaply. A polygonal territory whose box covers the granary
        // therefore passes even where the polygon itself would not; a granary reaching outside the
        // territory's box never does.
        if (!territoryBounds.get().contains(granaryBounds.get())) {
            return Verdict.OUTSIDE_TERRITORY;
        }
        return Verdict.ALLOWED;
    }

    /**
     * The granary's hay bale links the smallest region around it that lies inside the territory and
     * is not the territory itself, on the same terms as a region linked by name.
     *
     * @param around the regions WorldGuard reports at the hay bale's block
     * @param territory the kingdom's linked territory regions
     */
    public static Stone evaluateStone(
            NobleRank rank,
            boolean worldGuardAvailable,
            int x,
            int y,
            int z,
            Collection<Candidate> around,
            Collection<Candidate> territory) {
        if (!canSite(rank, false)) {
            return Stone.refused(Verdict.NOT_THE_CROWN);
        }
        if (!worldGuardAvailable) {
            return Stone.refused(Verdict.WORLDGUARD_ABSENT);
        }
        if (territory.isEmpty()) {
            return Stone.refused(Verdict.NO_TERRITORY);
        }
        Optional<Candidate> chosen = SubregionChooser.smallest(x, y, z, around, territory);
        if (chosen.isEmpty()) {
            return Stone.refused(Verdict.NO_REGION_AROUND);
        }
        GranaryBounds granary = bounds(chosen.get().bounds());
        Optional<GranaryBounds> enclosing = Optional.empty();
        for (Candidate linked : territory) {
            if (bounds(linked.bounds()).contains(granary)) {
                enclosing = Optional.of(bounds(linked.bounds()));
                break;
            }
        }
        Verdict verdict = evaluateLink(
                rank, false, true, territory.iterator().next().regionId(), enclosing, Optional.of(granary));
        return verdict == Verdict.ALLOWED
                ? new Stone(verdict, Optional.of(chosen.get().regionId()))
                : Stone.refused(verdict);
    }

    private static GranaryBounds bounds(CapitalRegionBox box) {
        return new GranaryBounds(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    /** Releasing a granary asks only that the actor be the Crown and that there be one to release. */
    public static Verdict evaluateClear(NobleRank rank, boolean operator, String granaryRegionId) {
        if (!canSite(rank, operator)) {
            return Verdict.NOT_THE_CROWN;
        }
        if (granaryRegionId == null || granaryRegionId.isBlank()) {
            return Verdict.NO_GRANARY;
        }
        return Verdict.ALLOWED;
    }

    public static String refusalMessage(Verdict verdict) {
        return switch (verdict) {
            case ALLOWED -> "";
            case NOT_THE_CROWN -> "Only the King, Queen, or an operator may site the granary.";
            case WORLDGUARD_ABSENT -> "WorldGuard is not installed.";
            case NO_TERRITORY -> "Your kingdom has no linked territory. Ask an admin to run /kingdom setregion.";
            case UNKNOWN_REGION -> "That region was not found in your kingdom's world. "
                    + "Run /rg list there and use the exact id.";
            case OUTSIDE_TERRITORY -> "The granary must lie inside your kingdom's territory.";
            case TOO_LARGE -> "That region is too great to keep grain in — no more than "
                    + "a hundred blocks on a side. Set its floor and ceiling with /rg selection.";
            case NO_GRANARY -> "Your kingdom has no granary to release.";
            case NO_REGION_AROUND -> "No region lies around the hay bale inside your territory. "
                    + "Mark the granary out as a WorldGuard region first, then lay the bale inside it.";
        };
    }
}
