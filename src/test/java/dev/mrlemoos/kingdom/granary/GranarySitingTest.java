package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.mrlemoos.kingdom.granary.GranarySiting.Verdict;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Who may site a granary, and where it may stand: the Crown or an operator alone, and only inside
 * the kingdom's own linked territory.
 */
class GranarySitingTest {

    private static final GranaryBounds TERRITORY = new GranaryBounds(-100, 0, -100, 100, 128, 100);
    private static final GranaryBounds INSIDE = new GranaryBounds(0, 64, 0, 8, 72, 8);
    private static final GranaryBounds STRADDLING = new GranaryBounds(90, 64, 90, 120, 72, 120);

    private static Verdict link(NobleRank rank, boolean operator) {
        return GranarySiting.evaluateLink(
                rank, operator, true, "north_hold", Optional.of(TERRITORY), Optional.of(INSIDE));
    }

    @Test
    void theCrownMaySiteAGranaryInsideItsOwnTerritory() {
        assertEquals(Verdict.ALLOWED, link(NobleRank.KING, false));
        assertEquals(Verdict.ALLOWED, link(NobleRank.QUEEN, false));
    }

    @Test
    void anOperatorMaySiteAGranaryWhateverTheirRank() {
        assertEquals(Verdict.ALLOWED, link(NobleRank.KNIGHT, true));
        assertEquals(Verdict.ALLOWED, link(null, true));
    }

    @Test
    void noOneBelowTheCrownMaySiteAGranary() {
        assertEquals(Verdict.NOT_THE_CROWN, link(NobleRank.PRINCE, false));
        assertEquals(Verdict.NOT_THE_CROWN, link(NobleRank.PREMIER, false));
        assertEquals(Verdict.NOT_THE_CROWN, link(null, false));
    }

    @Test
    void withoutWorldGuardNoGranaryCanBeSited() {
        assertEquals(
                Verdict.WORLDGUARD_ABSENT,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, false, "north_hold", Optional.of(TERRITORY), Optional.of(INSIDE)));
    }

    @Test
    void aKingdomWithNoTerritoryHasNowhereToPutAGranary() {
        assertEquals(
                Verdict.NO_TERRITORY,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, null, Optional.empty(), Optional.of(INSIDE)));
        assertEquals(
                Verdict.NO_TERRITORY,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "  ", Optional.empty(), Optional.of(INSIDE)));
    }

    @Test
    void aTerritoryWorldGuardCannotMeasureRefusesTheSiting() {
        assertEquals(
                Verdict.NO_TERRITORY,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "north_hold", Optional.empty(), Optional.of(INSIDE)));
    }

    @Test
    void aRegionWorldGuardDoesNotKnowIsRefused() {
        assertEquals(
                Verdict.UNKNOWN_REGION,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "north_hold", Optional.of(TERRITORY), Optional.empty()));
    }

    @Test
    void aGranaryReachingOutsideTheTerritoryIsRefused() {
        assertEquals(
                Verdict.OUTSIDE_TERRITORY,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "north_hold", Optional.of(TERRITORY), Optional.of(STRADDLING)));
    }

    @Test
    void aGranaryTooGreatToWalkIsRefusedAtTheSiting() {
        GranaryBounds vast = new GranaryBounds(0, 0, 0, 100, 100, 100);
        assertEquals(
                Verdict.TOO_LARGE,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "north_hold", Optional.of(TERRITORY), Optional.of(vast)));
    }

    @Test
    void aGranaryAtTheVeryLimitIsAllowed() {
        GranaryBounds atTheLimit = new GranaryBounds(0, 0, 0, 99, 99, 99);
        assertEquals(GranarySiting.MAX_GRANARY_VOLUME, atTheLimit.volume());
        assertEquals(
                Verdict.ALLOWED,
                GranarySiting.evaluateLink(
                        NobleRank.KING, false, true, "north_hold", Optional.of(TERRITORY), Optional.of(atTheLimit)));
    }

    @Test
    void theCrownMayClearAGranaryItHasSited() {
        assertEquals(Verdict.ALLOWED, GranarySiting.evaluateClear(NobleRank.QUEEN, false, "north_granary"));
        assertEquals(Verdict.ALLOWED, GranarySiting.evaluateClear(NobleRank.KNIGHT, true, "north_granary"));
    }

    @Test
    void thereIsNothingToClearWhenNoGranaryIsSited() {
        assertEquals(Verdict.NO_GRANARY, GranarySiting.evaluateClear(NobleRank.KING, false, null));
        assertEquals(Verdict.NO_GRANARY, GranarySiting.evaluateClear(NobleRank.KING, false, " "));
    }

    @Test
    void noOneBelowTheCrownMayClearAGranary() {
        assertEquals(Verdict.NOT_THE_CROWN, GranarySiting.evaluateClear(NobleRank.DUKE, false, "north_granary"));
    }

    @Test
    void everyRefusalCarriesAReason() {
        for (Verdict verdict : Verdict.values()) {
            if (verdict == Verdict.ALLOWED) {
                assertEquals("", GranarySiting.refusalMessage(verdict));
                continue;
            }
            assertFalse(GranarySiting.refusalMessage(verdict).isBlank(), verdict.name());
        }
    }
}
