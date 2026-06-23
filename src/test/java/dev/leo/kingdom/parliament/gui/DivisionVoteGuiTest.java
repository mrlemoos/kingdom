package dev.leo.kingdom.parliament.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DivisionVoteGuiTest {

    @Test
    void actionForSlotMapsVoteChoices() {
        DivisionVoteGui gui = new DivisionVoteGui("northmarch", "Finance Act 2026");

        assertEquals(ParliamentHubAction.VOTE_AYE, gui.actionForSlot(DivisionVoteGui.SLOT_AYE));
        assertEquals(ParliamentHubAction.VOTE_NAY, gui.actionForSlot(DivisionVoteGui.SLOT_NAY));
        assertEquals(ParliamentHubAction.VOTE_ABSTAIN, gui.actionForSlot(DivisionVoteGui.SLOT_ABSTAIN));
        assertNull(gui.actionForSlot(DivisionVoteGui.SLOT_BILL_INFO));
        assertNull(gui.actionForSlot(0));
    }
}
