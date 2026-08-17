package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The one thing the winter day leaves behind: whether each realm drew its ration or went short. */
class WinterLarderTest {

    @Test
    @DisplayName("a realm that drew its whole ration is fed")
    void aFullDrawIsFed() {
        RationDay day = new RationDay(275L, 4, 4);

        assertTrue(day.fed());
        assertFalse(day.unfed());
        assertEquals(0, day.shortBy());
    }

    @Test
    @DisplayName("a realm that drew short is unfed, and by how much is on the record")
    void aShortDrawIsUnfed() {
        RationDay day = new RationDay(275L, 4, 1);

        assertFalse(day.fed());
        assertTrue(day.unfed());
        assertEquals(3, day.shortBy());
    }

    @Test
    @DisplayName("a realm with no granary drew nothing at all, and is unfed for it")
    void noGranaryIsUnfed() {
        RationDay day = new RationDay(275L, 4, 0);

        assertTrue(day.unfed());
        assertEquals(4, day.shortBy());
    }

    @Test
    @DisplayName("a realm with no mouths to feed owes nothing and is fed")
    void noMouthsIsFed() {
        RationDay day = new RationDay(275L, 0, 0);

        assertTrue(day.fed());
        assertEquals(0, day.shortBy());
    }

    @Test
    @DisplayName("the larder keeps the last winter day settled for each realm")
    void theLarderKeepsTheLastDay() {
        WinterLarder larder = new WinterLarder();
        larder.record("north", new RationDay(275L, 4, 1));
        larder.record("south", new RationDay(275L, 2, 2));

        assertTrue(larder.isUnfed("north"));
        assertFalse(larder.isUnfed("south"));
        assertEquals(java.util.Set.of("north"), larder.unfedKingdoms());
        Optional<RationDay> north = larder.lastRation("north");
        assertTrue(north.isPresent());
        assertEquals(3, north.get().shortBy());
    }

    @Test
    @DisplayName("a later day replaces the one before it")
    void aLaterDayReplacesTheOneBefore() {
        WinterLarder larder = new WinterLarder();
        larder.record("north", new RationDay(275L, 4, 1));
        larder.record("north", new RationDay(276L, 4, 4));

        assertFalse(larder.isUnfed("north"));
        Optional<RationDay> north = larder.lastRation("north");
        assertTrue(north.isPresent());
        assertEquals(276L, north.get().realmDay());
    }

    @Test
    @DisplayName("a realm never settled is neither fed nor unfed, and the larder can be emptied")
    void anUnknownRealmIsNotUnfed() {
        WinterLarder larder = new WinterLarder();

        assertFalse(larder.isUnfed("north"));
        assertTrue(larder.lastRation("north").isEmpty());

        larder.record("north", new RationDay(275L, 4, 0));
        larder.forget("north");
        assertFalse(larder.isUnfed("north"));
        assertTrue(larder.unfedKingdoms().isEmpty());
    }
}
