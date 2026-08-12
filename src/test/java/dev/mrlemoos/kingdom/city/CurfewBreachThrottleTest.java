package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurfewBreachThrottleTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000071");

    @Test
    void allowsOnceThenBlocksWithinInterval() {
        CurfewBreachThrottle throttle = new CurfewBreachThrottle(30_000L);

        assertTrue(throttle.shouldOpen("northmarch", PLAYER, 1_000L));
        assertFalse(throttle.shouldOpen("northmarch", PLAYER, 20_000L));
        assertTrue(throttle.shouldOpen("northmarch", PLAYER, 31_001L));
    }

    @Test
    void differentKingdomsAreIndependent() {
        CurfewBreachThrottle throttle = new CurfewBreachThrottle(30_000L);

        assertTrue(throttle.shouldOpen("northmarch", PLAYER, 1_000L));
        assertTrue(throttle.shouldOpen("southmarch", PLAYER, 1_000L));
    }
}
