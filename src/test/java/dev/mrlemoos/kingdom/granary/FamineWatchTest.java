package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The day famine was last announced, so no realm hears of the same famine twice. */
class FamineWatchTest {

    private static final String KINGDOM = "northmarch";

    @Test
    void aFamineIsAnnouncedOnceAndNotAgainWhileItRuns() {
        FamineWatch watch = new FamineWatch();

        assertTrue(watch.claim(KINGDOM, 275L));
        assertFalse(watch.claim(KINGDOM, 276L));
        assertFalse(watch.claim(KINGDOM, 300L));
        assertEquals(275L, watch.lastAnnouncedDay(KINGDOM));
    }

    @Test
    void aFamineRelievedAndComeAgainIsAnnouncedAfresh() {
        FamineWatch watch = new FamineWatch();
        watch.claim(KINGDOM, 275L);

        watch.relieve(KINGDOM);

        assertEquals(-1L, watch.lastAnnouncedDay(KINGDOM));
        assertTrue(watch.claim(KINGDOM, 280L));
        assertEquals(280L, watch.lastAnnouncedDay(KINGDOM));
    }

    @Test
    void eachRealmIsAnnouncedOnItsOwnAccount() {
        FamineWatch watch = new FamineWatch();

        assertTrue(watch.claim(KINGDOM, 275L));
        assertTrue(watch.claim("southreach", 275L));
        assertEquals(Map.of(KINGDOM, Long.valueOf(275L), "southreach", Long.valueOf(275L)), watch.allView());
    }

    @Test
    void theWatchIsRestoredFromDisk() {
        FamineWatch watch = new FamineWatch();

        watch.replaceAll(Map.of(KINGDOM, Long.valueOf(275L)));

        assertEquals(275L, watch.lastAnnouncedDay(KINGDOM));
        assertFalse(watch.claim(KINGDOM, 276L));

        watch.replaceAll(Map.of());
        assertEquals(-1L, watch.lastAnnouncedDay(KINGDOM));
    }
}
