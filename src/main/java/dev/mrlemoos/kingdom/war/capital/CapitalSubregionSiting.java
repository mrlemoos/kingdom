package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import java.util.Optional;

/**
 * Who may link the capital-fall WorldGuard subregion, and where it may stand: the Crown of that
 * kingdom, and only a region wholly inside linked territory.
 */
public final class CapitalSubregionSiting {

    public enum Verdict {
        ALLOWED,
        NOT_THE_CROWN,
        WORLDGUARD_ABSENT,
        NO_TERRITORY,
        UNKNOWN_REGION,
        OUTSIDE_TERRITORY,
        NO_CAPITAL
    }

    private CapitalSubregionSiting() {}

    public static Verdict evaluateLink(
            NobleRank rank,
            boolean worldGuardAvailable,
            String territoryRegionId,
            Optional<CapitalRegionBox> territoryBounds,
            Optional<CapitalRegionBox> capitalBounds) {
        if (!RankAuthority.canSiteCapital(rank)) {
            return Verdict.NOT_THE_CROWN;
        }
        if (!worldGuardAvailable) {
            return Verdict.WORLDGUARD_ABSENT;
        }
        if (territoryRegionId == null || territoryRegionId.isBlank()) {
            return Verdict.NO_TERRITORY;
        }
        if (capitalBounds.isEmpty()) {
            return Verdict.UNKNOWN_REGION;
        }
        if (territoryBounds.isEmpty()) {
            return Verdict.NO_TERRITORY;
        }
        if (!territoryBounds.get().contains(capitalBounds.get())) {
            return Verdict.OUTSIDE_TERRITORY;
        }
        return Verdict.ALLOWED;
    }

    public static Verdict evaluateClear(NobleRank rank, boolean capitalSet) {
        if (!RankAuthority.canSiteCapital(rank)) {
            return Verdict.NOT_THE_CROWN;
        }
        if (!capitalSet) {
            return Verdict.NO_CAPITAL;
        }
        return Verdict.ALLOWED;
    }

    public static String refusalMessage(Verdict verdict) {
        return switch (verdict) {
            case ALLOWED -> "";
            case NOT_THE_CROWN -> "Only the King or Queen may site the capital region.";
            case WORLDGUARD_ABSENT -> "WorldGuard is not installed.";
            case NO_TERRITORY -> "Your kingdom has no linked territory. Ask an admin to run /kingdom setregion.";
            case UNKNOWN_REGION -> "That region was not found in your kingdom's world. "
                    + "Run /rg list there and use the exact id.";
            case OUTSIDE_TERRITORY -> "The capital region must lie inside your kingdom's territory.";
            case NO_CAPITAL -> "Your kingdom has no capital region to release.";
        };
    }
}
