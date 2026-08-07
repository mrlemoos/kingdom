package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.GolemOrder;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PoliceGolemOrderGuiTest {

    @Test
    void orderForSlotMapsCommands() {
        PoliceGolemOrderGui gui = new PoliceGolemOrderGui(UUID.randomUUID());

        assertEquals(GolemOrder.FOLLOW, gui.orderForSlot(PoliceGolemOrderGui.SLOT_FOLLOW));
        assertEquals(GolemOrder.STAY, gui.orderForSlot(PoliceGolemOrderGui.SLOT_STAY));
        assertEquals(GolemOrder.PATROL, gui.orderForSlot(PoliceGolemOrderGui.SLOT_PATROL));
        assertNull(gui.orderForSlot(0));
    }

    @Test
    void onlyCrownMayCommand() {
        assertTrue(PoliceGolemOrderGui.canCommand(NobleRank.KING));
        assertTrue(PoliceGolemOrderGui.canCommand(NobleRank.QUEEN));
        assertTrue(PoliceGolemOrderGui.canCommand(NobleRank.PRINCE));
        assertFalse(PoliceGolemOrderGui.canCommand(NobleRank.PREMIER));
        assertFalse(PoliceGolemOrderGui.canCommand(NobleRank.KNIGHT));
    }
}
