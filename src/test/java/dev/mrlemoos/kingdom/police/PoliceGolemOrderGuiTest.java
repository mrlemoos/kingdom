package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.GolemOfficerKind;
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
        assertNull(gui.orderForSlot(PoliceGolemOrderGui.SLOT_STAND_DOWN));
    }

    @Test
    void aPatrolIsPostedAsGuardAndAGuardSentOnPatrol() {
        PoliceGolemOrderGui patrol = new PoliceGolemOrderGui(UUID.randomUUID(), GolemOfficerKind.PATROL);
        PoliceGolemOrderGui guard = new PoliceGolemOrderGui(UUID.randomUUID(), GolemOfficerKind.GUARD);

        assertEquals(GolemOfficerKind.GUARD, patrol.kindForSlot(PoliceGolemOrderGui.SLOT_KIND));
        assertEquals(GolemOfficerKind.PATROL, guard.kindForSlot(PoliceGolemOrderGui.SLOT_KIND));
        assertNull(patrol.kindForSlot(PoliceGolemOrderGui.SLOT_PATROL));
        assertNull(patrol.orderForSlot(PoliceGolemOrderGui.SLOT_KIND));
    }

    @Test
    void aGuardTakesNoMarchingOrders() {
        PoliceGolemOrderGui guard = new PoliceGolemOrderGui(UUID.randomUUID(), GolemOfficerKind.GUARD);

        assertNull(guard.orderForSlot(PoliceGolemOrderGui.SLOT_FOLLOW));
        assertNull(guard.orderForSlot(PoliceGolemOrderGui.SLOT_PATROL));
    }

    @Test
    void onlyTheStandDownSlotStandsTheOfficerDown() {
        assertTrue(PoliceGolemOrderGui.isStandDown(PoliceGolemOrderGui.SLOT_STAND_DOWN));
        assertFalse(PoliceGolemOrderGui.isStandDown(PoliceGolemOrderGui.SLOT_PATROL));
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
