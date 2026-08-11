package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatrolDetainPolicyTest {

    @Test
    void requiresReadyWarrantJurisdictionAndNoPending() {
        assertTrue(PatrolDetainPolicy.mayDetain(true, true, true, false));
        assertFalse(PatrolDetainPolicy.mayDetain(false, true, true, false));
        assertFalse(PatrolDetainPolicy.mayDetain(true, false, true, false));
        assertFalse(PatrolDetainPolicy.mayDetain(true, true, false, false));
        assertFalse(PatrolDetainPolicy.mayDetain(true, true, true, true));
    }
}
