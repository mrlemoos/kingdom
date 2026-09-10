package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.war.capital.CapitalSubregionSiting.Verdict;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Crown links a WorldGuard subregion inside linked territory as the capital for capital-fall aims.
 */
class CapitalSubregionSitingTest {

    private static final CapitalRegionBox TERRITORY = new CapitalRegionBox(-160, 0, -160, 160, 128, 160);
    private static final CapitalRegionBox INSIDE = new CapitalRegionBox(0, 64, 0, 32, 80, 32);
    private static final CapitalRegionBox STRADDLING = new CapitalRegionBox(140, 64, 140, 200, 80, 200);

    private static Verdict link(NobleRank rank) {
        return CapitalSubregionSiting.evaluateLink(
                rank, true, "north_hold", Optional.of(TERRITORY), Optional.of(INSIDE));
    }

    @Test
    void theCrownMayLinkACapitalSubregionInsideTerritory() {
        // Arrange
        // Act
        // Assert
        assertEquals(Verdict.ALLOWED, link(NobleRank.KING));
        assertEquals(Verdict.ALLOWED, link(NobleRank.QUEEN));
    }

    @Test
    void ranksBelowTheCrownMayNotLinkACapitalSubregion() {
        assertEquals(Verdict.NOT_THE_CROWN, link(NobleRank.PRINCE));
        assertEquals(Verdict.NOT_THE_CROWN, link(NobleRank.KNIGHT));
        assertEquals(Verdict.NOT_THE_CROWN, link(null));
    }

    @Test
    void missingWorldGuardRefusesTheLink() {
        assertEquals(
                Verdict.WORLDGUARD_ABSENT,
                CapitalSubregionSiting.evaluateLink(
                        NobleRank.KING, false, "north_hold", Optional.of(TERRITORY), Optional.of(INSIDE)));
    }

    @Test
    void unknownRegionIsRefused() {
        assertEquals(
                Verdict.UNKNOWN_REGION,
                CapitalSubregionSiting.evaluateLink(
                        NobleRank.KING, true, "north_hold", Optional.of(TERRITORY), Optional.empty()));
    }

    @Test
    void aSubregionOutsideTerritoryIsRefused() {
        assertEquals(
                Verdict.OUTSIDE_TERRITORY,
                CapitalSubregionSiting.evaluateLink(
                        NobleRank.KING, true, "north_hold", Optional.of(TERRITORY), Optional.of(STRADDLING)));
    }

    @Test
    void noLinkedTerritoryRefusesTheLink() {
        assertEquals(
                Verdict.NO_TERRITORY,
                CapitalSubregionSiting.evaluateLink(
                        NobleRank.KING, true, null, Optional.empty(), Optional.of(INSIDE)));
    }

    @Test
    void theCrownMayClearAnExistingCapitalSubregion() {
        assertEquals(Verdict.ALLOWED, CapitalSubregionSiting.evaluateClear(NobleRank.QUEEN, true));
    }

    @Test
    void clearingWhenNoneIsSetIsRefused() {
        assertEquals(Verdict.NO_CAPITAL, CapitalSubregionSiting.evaluateClear(NobleRank.KING, false));
    }
}
