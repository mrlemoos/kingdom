package dev.mrlemoos.kingdom.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrialBarTextTest {

    @Test
    void labelIncludesAccusedAndSeconds() {
        assertEquals("Trial of Alice — 120s", TrialBarText.label("Alice", 120L));
    }

    @Test
    void progressDrainsFromFullToEmpty() {
        assertEquals(1.0f, TrialBarText.progress(120_000L, 120_000L));
        assertEquals(0.5f, TrialBarText.progress(60_000L, 120_000L));
        assertEquals(0.0f, TrialBarText.progress(0L, 120_000L));
        assertTrue(TrialBarText.progress(-1L, 120_000L) >= 0.0f);
    }
}
