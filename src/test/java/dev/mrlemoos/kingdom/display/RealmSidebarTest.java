package dev.mrlemoos.kingdom.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RealmSidebarTest {

    @Test
    void showsTheRealmTheSeasonAndThePurse() {
        RealmSidebar sidebar = RealmSidebar.of("Avalon", Season.WINTER, 12.5).orElseThrow();

        assertEquals("&6&lKingdom of Avalon", sidebar.title());
        assertEquals("&7Season: &fWinter", sidebar.lines().get(0));
        assertEquals("&7Wallet: &f12.50 Corona", sidebar.lines().get(1));
    }

    @Test
    void roundNumbersCarryNoPence() {
        RealmSidebar sidebar = RealmSidebar.of("Avalon", Season.SPRING, 40.0).orElseThrow();

        assertEquals("&7Wallet: &f40 Corona", sidebar.lines().get(1));
    }

    @Test
    void nothingIsShownOutsideAnyRealm() {
        assertTrue(RealmSidebar.of(null, Season.SUMMER, 40.0).isEmpty());
        assertTrue(RealmSidebar.of("  ", Season.SUMMER, 40.0).isEmpty());
    }
}
