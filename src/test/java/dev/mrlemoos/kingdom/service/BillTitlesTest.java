package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.BillType;
import org.junit.jupiter.api.Test;

class BillTitlesTest {

    private static final long TABLED_AT = 1_700_000_000_000L;

    @Test
    void defaultWarTitleIsAWarBill() {
        // Arrange
        // Act
        String title = BillTitles.defaultTitle(BillType.WAR, "northmarch", TABLED_AT);

        // Assert
        assertTrue(title.startsWith("War Bill"));
        assertTrue(title.contains("northmarch"));
    }

    @Test
    void defaultCounterWarTitleIsDistinctFromAFirstStrikeWarBill() {
        // Arrange
        // Act
        String title = BillTitles.defaultCounterWarTitle("southreach", TABLED_AT);

        // Assert
        assertTrue(title.startsWith("Counter-war Bill"));
        assertFalse(title.startsWith("War Bill"));
        assertTrue(title.contains("southreach"));
    }
}
