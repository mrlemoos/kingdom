package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import org.junit.jupiter.api.Test;

class TreasuryLordPlacementTest {

    @Test
    void lordStandsOnTheMintItself() {
        MintLocation mint = new MintLocation("world", 10, 64, 20);

        assertEquals(10, TreasuryLordPlacement.lordBlockX(mint));
        assertEquals(64, TreasuryLordPlacement.lordBlockY(mint));
        assertEquals(20, TreasuryLordPlacement.lordBlockZ(mint));
    }

    @Test
    void lordFacesTheWayTheMintWasSited() {
        assertEquals(-90f, TreasuryLordPlacement.lordYaw(new MintLocation("world", 10, 64, 20, -90f, null)), 0.001f);
    }

    @Test
    void aMintSitedBeforeTheYawExistedFacesDueSouth() {
        assertEquals(0f, TreasuryLordPlacement.lordYaw(new MintLocation("world", 10, 64, 20)), 0.001f);
    }

    @Test
    void anOutOfRangeYawIsWrappedIntoRange() {
        assertEquals(-90f, TreasuryLordPlacement.lordYaw(new MintLocation("world", 1, 1, 1, 270f, null)), 0.001f);
    }

    @Test
    void theLordUuidRidesAlongsideTheYaw() {
        MintLocation mint = new MintLocation("world", 10, 64, 20, 45f, null)
                .withTreasuryLordUuid("00000000-0000-0000-0000-000000000001");

        assertEquals(45f, mint.yaw(), 0.001f);
        assertTrue(mint.lordEntityId().isPresent());
    }

    @Test
    void displayNameMatchesTreasuryLord() {
        assertTrue(TreasuryLordPlacement.isTreasuryLordDisplayName(
                TreasuryLordPlacement.LORD_DISPLAY_NAME));
        assertFalse(TreasuryLordPlacement.isTreasuryLordDisplayName("Villager"));
    }
}
