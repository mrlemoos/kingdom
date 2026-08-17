package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** What the Crown sees of the thefts: the last hands caught in the granary, newest first. */
class GranaryTheftLogTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void theNewestTheftIsReadFirst() {
        GranaryTheftLog log = new GranaryTheftLog();

        log.record(KINGDOM, FIRST, 900L);
        log.record(KINGDOM, SECOND, 901L);

        List<GranaryTheftLog.Entry> entries = log.entries(KINGDOM);
        assertEquals(2, entries.size());
        assertEquals(SECOND, entries.get(0).thiefId());
        assertEquals(901L, entries.get(0).mcDay());
        assertEquals(FIRST, entries.get(1).thiefId());
    }

    @Test
    void onlyTheLastFewAreKept() {
        GranaryTheftLog log = new GranaryTheftLog();

        for (int theft = 0; theft < GranaryTheftLog.KEPT + 5; theft++) {
            log.record(KINGDOM, FIRST, 900L + theft);
        }

        assertEquals(GranaryTheftLog.KEPT, log.entries(KINGDOM).size());
        assertEquals(900L + GranaryTheftLog.KEPT + 4, log.entries(KINGDOM).get(0).mcDay());
    }

    @Test
    void eachRealmKeepsItsOwnLog() {
        GranaryTheftLog log = new GranaryTheftLog();

        log.record(KINGDOM, FIRST, 900L);

        assertEquals(1, log.entries(KINGDOM).size());
        assertTrue(log.entries("southreach").isEmpty());
    }
}
