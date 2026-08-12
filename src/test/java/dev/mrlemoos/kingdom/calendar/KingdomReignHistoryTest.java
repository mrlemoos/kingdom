package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KingdomReignHistoryTest {

    @Test
    void openingAReignRecordsTheAccessionDay() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 100L);

        ReignRecord current = RegnalDating.currentReign(history.view()).orElseThrow();
        assertEquals("Leo", current.monarchName());
        assertEquals(100L, current.accessionDay());
        assertEquals(1, current.ordinal());
        assertTrue(current.isOpen());
    }

    @Test
    void openingASecondReignOfTheSameNameNumbersIt() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 0L);
        history.closeOpenReign(300L);
        history.openReign("id-leo-2", "Leo", "King", 300L);

        assertEquals(2, RegnalDating.currentReign(history.view()).orElseThrow().ordinal());
    }

    @Test
    void openingAReignClosesAnyReignLeftOpen() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 0L);
        history.openReign("id-ana", "Ana", "Queen", 300L);

        assertEquals(2, history.view().size());
        assertEquals(300L, history.view().get(0).endDay());
        assertEquals("Ana", RegnalDating.currentReign(history.view()).orElseThrow().monarchName());
    }

    @Test
    void reopeningTheSameMonarchIsIgnored() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 0L);
        history.openReign("id-leo", "Leo", "King", 50L);

        assertEquals(1, history.view().size());
        assertEquals(0L, history.view().get(0).accessionDay());
    }

    @Test
    void closingLeavesTheRealmInInterregnum() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 0L);
        assertTrue(history.closeOpenReign(300L));
        assertTrue(RegnalDating.currentReign(history.view()).isEmpty());
        assertFalse(history.closeOpenReign(400L));
    }

    @Test
    void aReignNeverEndsBeforeItBegan() {
        KingdomReignHistory history = new KingdomReignHistory();
        history.openReign("id-leo", "Leo", "King", 300L);
        history.closeOpenReign(100L);

        assertEquals(300L, history.view().get(0).endDay());
    }
}
