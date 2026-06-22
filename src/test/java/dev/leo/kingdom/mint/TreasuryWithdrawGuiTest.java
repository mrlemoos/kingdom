package dev.leo.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.leo.kingdom.mint.TreasuryWithdrawGui;
import org.junit.jupiter.api.Test;

class TreasuryWithdrawGuiTest {

    @Test
    void amountForSlotReturnsPresetWithdrawAmounts() {
        TreasuryWithdrawGui gui = new TreasuryWithdrawGui("northmarch");

        assertEquals(1, gui.amountForSlot(TreasuryWithdrawGui.SLOT_ONE, 100.0));
        assertEquals(5, gui.amountForSlot(TreasuryWithdrawGui.SLOT_FIVE, 100.0));
        assertEquals(10, gui.amountForSlot(TreasuryWithdrawGui.SLOT_TEN, 100.0));
        assertEquals(32, gui.amountForSlot(TreasuryWithdrawGui.SLOT_THIRTY_TWO, 100.0));
        assertEquals(64, gui.amountForSlot(TreasuryWithdrawGui.SLOT_SIXTY_FOUR, 100.0));
        assertEquals(64, gui.amountForSlot(TreasuryWithdrawGui.SLOT_MAX, 100.0));
        assertEquals(100, gui.amountForSlot(TreasuryWithdrawGui.SLOT_ALL, 100.0));
    }

    @Test
    void amountForSlotReturnsNullWhenBalanceTooLow() {
        TreasuryWithdrawGui gui = new TreasuryWithdrawGui("northmarch");

        assertNull(gui.amountForSlot(TreasuryWithdrawGui.SLOT_TEN, 5.0));
        assertNull(gui.amountForSlot(TreasuryWithdrawGui.SLOT_ALL, 0.0));
    }
}
