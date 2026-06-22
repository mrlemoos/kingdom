package dev.leo.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.model.MintLocation;
import org.junit.jupiter.api.Test;

class TreasuryLordPlacementTest {

    @Test
    void lordSpawnsNorthOfLectern() {
        MintLocation mint = new MintLocation("world", 10, 64, 20);

        assertEquals(10, TreasuryLordPlacement.lordBlockX(mint));
        assertEquals(64, TreasuryLordPlacement.lordBlockY(mint));
        assertEquals(19, TreasuryLordPlacement.lordBlockZ(mint));
    }

    @Test
    void displayNameMatchesTreasuryLord() {
        assertTrue(TreasuryLordPlacement.isTreasuryLordDisplayName(
                TreasuryLordPlacement.LORD_DISPLAY_NAME));
        assertFalse(TreasuryLordPlacement.isTreasuryLordDisplayName("Villager"));
    }
}
