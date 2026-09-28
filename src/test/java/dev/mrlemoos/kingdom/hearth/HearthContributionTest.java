package dev.mrlemoos.kingdom.hearth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HearthContributionTest {

    @Test
    void creditsOnlyAcceptedFuelAndCapsDeliveryAtOneDay() {
        assertEquals(new HearthContribution(2, 0), HearthContribution.plan(1, 8, 2, 3).orElseThrow());
        assertEquals(new HearthContribution(1, 1), HearthContribution.plan(0, 1, 4, 2).orElseThrow());
        assertTrue(HearthContribution.plan(2, 4, 4, 2).isEmpty());
    }
}
