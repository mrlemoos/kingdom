package dev.mrlemoos.kingdom.parliament.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WarPrepareGuiTest {

    @Test
    void inventoryTitleDistinguishesCounterWarFromFirstStrike() {
        // Arrange
        // Act
        String firstStrike = WarPrepareGui.inventoryTitle(false);
        String counterWar = WarPrepareGui.inventoryTitle(true);

        // Assert
        assertTrue(firstStrike.toLowerCase().contains("war bill"));
        assertFalse(firstStrike.toLowerCase().contains("counter-war"));
        assertTrue(counterWar.toLowerCase().contains("counter-war"));
    }

    @Test
    void confirmLabelDistinguishesCounterWarFromFirstStrike() {
        // Arrange
        // Act
        String firstStrike = WarPrepareGui.confirmLabel(false);
        String counterWar = WarPrepareGui.confirmLabel(true);

        // Assert
        assertTrue(firstStrike.toLowerCase().contains("war bill"));
        assertFalse(firstStrike.toLowerCase().contains("counter-war"));
        assertTrue(counterWar.toLowerCase().contains("counter-war"));
    }
}
