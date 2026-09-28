package dev.mrlemoos.kingdom.hearth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HearthWarningPolicyTest {

    @Test
    void warnsOnceUntilSupplyStateChangesAndIgnoresIncompleteScans() {
        var low = HearthWarningPolicy.observe(null, true, 2, 1);
        assertEquals("low-fuel", low.state());
        assertTrue(low.changed());

        var unchanged = HearthWarningPolicy.observe(low.state(), true, 2, 1);
        assertFalse(unchanged.changed());

        var unknown = HearthWarningPolicy.observe(low.state(), false, 0, 0);
        assertEquals(low.state(), unknown.state());
        assertFalse(unknown.changed());

        var ready = HearthWarningPolicy.observe(low.state(), true, 2, 0);
        assertEquals("ready", ready.state());
        assertTrue(ready.changed());
    }
}
