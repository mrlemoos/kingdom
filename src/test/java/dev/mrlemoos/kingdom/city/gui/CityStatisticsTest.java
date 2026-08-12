package dev.mrlemoos.kingdom.city.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CityStatisticsTest {

    @Test
    void readsTheRealmAsEightLines() {
        List<String> lines = new CityStatistics(4, 3, 17, 250.0, 1000.5, 12.0, 8.0, 6).lines();

        assertEquals(8, lines.size());
        assertEquals("Subjects: 4", lines.get(0));
        assertEquals("Permit holders: 3", lines.get(1));
        assertEquals("Villagers: 17", lines.get(2));
        assertEquals("Trades settled (last day): 6", lines.get(7));
    }

    @Test
    void dropsThePenceOnWholeCorona() {
        List<String> lines = new CityStatistics(0, 0, 0, 250.0, 1000.5, 0.0, 0.0, 0).lines();

        assertTrue(lines.contains("Treasury: 250 Corona"), lines.toString());
        assertTrue(lines.contains("Realm wealth: 1000.50 Corona"), lines.toString());
    }
}
