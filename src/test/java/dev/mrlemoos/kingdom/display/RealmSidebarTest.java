package dev.mrlemoos.kingdom.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RealmSidebarTest {

    @Test
    void showsTheRealmTheSeasonAndThePurse() {
        RealmSidebar sidebar = RealmSidebar.of("Avalon", Season.WINTER, 12.5, "Steadfast").orElseThrow();

        assertEquals("&6Avalon", sidebar.title());
        assertEquals("&7Season: &fWinter", sidebar.lines().get(0));
        assertEquals("&7Wallet: &f12.50 Corona", sidebar.lines().get(1));
        assertEquals("&7Morale: &fSteadfast", sidebar.lines().get(2));
    }

    @Test
    void roundNumbersCarryNoPence() {
        RealmSidebar sidebar = RealmSidebar.of("Avalon", Season.SPRING, 40.0, "Not open").orElseThrow();

        assertEquals("&7Wallet: &f40 Corona", sidebar.lines().get(1));
        assertEquals("&7Morale: &fNot open", sidebar.lines().get(2));
    }

    @Test
    void nothingIsShownOutsideAnyRealm() {
        assertTrue(RealmSidebar.of(null, Season.SUMMER, 40.0, "Steadfast").isEmpty());
        assertTrue(RealmSidebar.of("  ", Season.SUMMER, 40.0, "Steadfast").isEmpty());
    }
}
