package dev.mrlemoos.kingdom.honours;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class HonoursGuiTest {

    @Test
    void theCrownDoesNotGiveAwayItsOwnTitlesOrElectedSeats() {
        assertFalse(HonoursGui.GRANTABLE.contains(NobleRank.KING));
        assertFalse(HonoursGui.GRANTABLE.contains(NobleRank.QUEEN));
        assertFalse(HonoursGui.GRANTABLE.contains(NobleRank.MP));
        assertFalse(HonoursGui.GRANTABLE.contains(NobleRank.PREMIER));
    }

    @Test
    void slotsMapOntoTheHonoursList() {
        assertEquals(NobleRank.PRINCE, HonoursGui.rankForSlot(HonoursGui.FIRST_SLOT));
        assertEquals(
                HonoursGui.GRANTABLE.get(HonoursGui.GRANTABLE.size() - 1),
                HonoursGui.rankForSlot(HonoursGui.FIRST_SLOT + HonoursGui.GRANTABLE.size() - 1));
        assertNull(HonoursGui.rankForSlot(HonoursGui.FIRST_SLOT - 1));
        assertNull(HonoursGui.rankForSlot(HonoursGui.FIRST_SLOT + HonoursGui.GRANTABLE.size()));
        assertNull(HonoursGui.rankForSlot(HonoursGui.SLOT_STRIP));
        assertTrue(HonoursGui.isStripSlot(HonoursGui.SLOT_STRIP));
    }
}
