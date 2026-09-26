package dev.mrlemoos.kingdom.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.war.capital.CapitalRegionBox;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser.Candidate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The capital and granary stones link the smallest WorldGuard subregion around them inside territory. */
class SubregionChooserTest {

    private static final Candidate TERRITORY = new Candidate("north_hold", new CapitalRegionBox(-160, 0, -160, 160, 128, 160));
    private static final Candidate CITY = new Candidate("city", new CapitalRegionBox(-40, 0, -40, 40, 128, 40));
    private static final Candidate KEEP = new Candidate("keep", new CapitalRegionBox(-8, 60, -8, 8, 90, 8));
    private static final Candidate MARKET = new Candidate("market", new CapitalRegionBox(20, 60, 20, 30, 90, 30));

    private static Optional<String> around(int x, int y, int z, List<Candidate> regions) {
        return SubregionChooser.smallestAround(x, y, z, regions, List.of(TERRITORY));
    }

    @Test
    void theSmallestSubregionAroundTheStoneIsChosen() {
        assertEquals(Optional.of("keep"), around(0, 64, 0, List.of(TERRITORY, CITY, KEEP, MARKET)));
    }

    @Test
    void aSubregionThatDoesNotHoldTheStoneIsPassedOver() {
        assertEquals(Optional.of("city"), around(15, 64, 15, List.of(TERRITORY, CITY, KEEP, MARKET)));
    }

    @Test
    void withNoSubregionTheWarRegionIsLeftUnset() {
        assertEquals(Optional.empty(), around(100, 64, 100, List.of(TERRITORY, CITY, KEEP)));
        assertEquals(Optional.empty(), around(0, 64, 0, List.of()));
    }

    @Test
    void theTerritoryItselfIsNeverTheCapital() {
        assertEquals(Optional.empty(), around(0, 64, 0, List.of(TERRITORY)));
    }

    @Test
    void aRegionStraddlingTheBorderIsPassedOver() {
        Candidate straddling = new Candidate("border_fort", new CapitalRegionBox(150, 60, 150, 170, 90, 170));

        assertEquals(Optional.empty(), around(155, 64, 155, List.of(TERRITORY, straddling)));
    }

    @Test
    void equalSizesFallToTheFirstIdAlphabetically() {
        Candidate east = new Candidate("east", new CapitalRegionBox(-8, 60, -8, 8, 90, 8));

        assertEquals(Optional.of("east"), around(0, 64, 0, List.of(KEEP, east, TERRITORY)));
    }
}
