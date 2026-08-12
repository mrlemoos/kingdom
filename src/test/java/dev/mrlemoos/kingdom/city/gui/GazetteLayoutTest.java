package dev.mrlemoos.kingdom.city.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GazetteLayoutTest {

    @Test
    void pageCountRoundsUp() {
        assertEquals(1, GazetteLayout.pageCount(0));
        assertEquals(1, GazetteLayout.pageCount(45));
        assertEquals(2, GazetteLayout.pageCount(46));
    }

    @Test
    void pageSliceRespectsBounds() {
        List<String> entries = List.of("a", "b", "c");
        assertEquals(List.of("a", "b", "c"), GazetteLayout.pageSlice(entries, 0));
        assertTrue(GazetteLayout.pageSlice(entries, 1).isEmpty());
    }

    @Test
    void navigationFlags() {
        assertFalse(GazetteLayout.hasPrevious(0));
        assertTrue(GazetteLayout.hasNext(0, 50));
        assertFalse(GazetteLayout.hasNext(1, 50));
    }
}
