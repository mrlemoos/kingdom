package dev.mrlemoos.kingdom.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RealmNeedsTest {

    @Test
    void reportsSitesAndAvoidsCallingUnloadedHearthsEmpty() {
        assertEquals(
                java.util.List.of(
                        "Food: no granary. The Crown must site one from the Realm Hub.",
                        "Warmth: some hearth stores are unloaded; check the hearths in person."),
                RealmNeeds.lines(null, 4, 0, 0, 0, java.util.List.of(), false));
    }

    @Test
    void warnsAtLessThanOneDayAndShowsContributionAction() {
        assertEquals(
                "Food: 2 bales short at north-granary. Take hay there and sneak-right-click its bale.",
                RealmNeeds.lines("north-granary", 2, 1, 1, 1, java.util.List.of("1, 64, 2"), true).get(0));
        assertEquals(
                "Warmth: 1 hearth needs one day's fuel (1 fuel items in all stores). Right-click each lit campfire with fuel.",
                RealmNeeds.lines("north-granary", 2, 1, 1, 1, java.util.List.of("1, 64, 2"), true).get(1));
        assertEquals(
                "Low hearths at 1, 64, 2.",
                RealmNeeds.lines("north-granary", 2, 1, 1, 1, java.util.List.of("1, 64, 2"), true).get(2));
    }
}
