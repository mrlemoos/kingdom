package dev.mrlemoos.kingdom.city.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OathGuiTest {

    @Test
    void swearAndDeclineAreDistinctSlots() {
        OathGui gui = new OathGui("northmarch");

        assertTrue(gui.isSwearSlot(OathGui.SLOT_SWEAR));
        assertTrue(gui.isDeclineSlot(OathGui.SLOT_DECLINE));
        assertFalse(gui.isSwearSlot(OathGui.SLOT_DECLINE));
        assertFalse(gui.isDeclineSlot(OathGui.SLOT_SWEAR));
        assertFalse(gui.isSwearSlot(OathGui.SLOT_TEXT));
    }

    @Test
    void theBookHoldsTheOath() {
        assertEquals(13, OathGui.SLOT_TEXT);
        assertEquals(11, OathGui.SLOT_SWEAR);
        assertEquals(15, OathGui.SLOT_DECLINE);
    }
}
