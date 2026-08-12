package dev.mrlemoos.kingdom.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DivisionBarTextTest {

    @Test
    void readsTheBillAndTheRunningTally() {
        assertEquals("Division: Finance Bill — 3 aye / 2 nay", DivisionBarText.label("Finance Bill", 3, 2));
    }

    @Test
    void anUntitledBillIsStillReadOut() {
        assertEquals("Division: a bill — 0 aye / 0 nay", DivisionBarText.label("  ", 0, 0));
        assertEquals("Division: a bill — 0 aye / 0 nay", DivisionBarText.label(null, 0, 0));
    }

    @Test
    void aFreshDivisionShowsAFullBar() {
        assertEquals(1.0f, DivisionBarText.progress(10L, 12L, 2), 0.0001f);
    }

    @Test
    void theBarDrainsAsTheWindowRuns() {
        assertEquals(0.5f, DivisionBarText.progress(11L, 12L, 2), 0.0001f);
    }

    @Test
    void anExpiredOrOverrunWindowShowsAnEmptyBar() {
        assertEquals(0.0f, DivisionBarText.progress(12L, 12L, 2), 0.0001f);
        assertEquals(0.0f, DivisionBarText.progress(20L, 12L, 2), 0.0001f);
    }

    @Test
    void aDivisionWithNoClosingDayStaysFull() {
        assertEquals(1.0f, DivisionBarText.progress(10L, -1L, 2), 0.0001f);
        assertEquals(1.0f, DivisionBarText.progress(10L, 12L, 0), 0.0001f);
    }
}
