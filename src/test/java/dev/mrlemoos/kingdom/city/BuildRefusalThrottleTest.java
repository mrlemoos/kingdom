package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuildRefusalThrottleTest {

    private static final UUID MINER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    @Test
    void theFirstRefusalAlwaysSpeaks() {
        BuildRefusalThrottle throttle = new BuildRefusalThrottle(30_000L);

        assertTrue(throttle.shouldSend(MINER, 1_000L));
    }

    @Test
    void repeatedRefusalsInsideTheWindowStaySilent() {
        BuildRefusalThrottle throttle = new BuildRefusalThrottle(30_000L);
        throttle.shouldSend(MINER, 1_000L);

        assertFalse(throttle.shouldSend(MINER, 1_500L));
        assertFalse(throttle.shouldSend(MINER, 30_999L));
    }

    @Test
    void theWindowReopensOnceItHasElapsed() {
        BuildRefusalThrottle throttle = new BuildRefusalThrottle(30_000L);
        throttle.shouldSend(MINER, 1_000L);

        assertTrue(throttle.shouldSend(MINER, 31_000L));
        assertFalse(throttle.shouldSend(MINER, 31_001L));
    }

    @Test
    void eachPlayerIsThrottledSeparately() {
        BuildRefusalThrottle throttle = new BuildRefusalThrottle(30_000L);
        throttle.shouldSend(MINER, 1_000L);

        assertTrue(throttle.shouldSend(OTHER, 1_000L));
    }

    @Test
    void forgettingAPlayerLetsThemBeToldAgainImmediately() {
        BuildRefusalThrottle throttle = new BuildRefusalThrottle(30_000L);
        throttle.shouldSend(MINER, 1_000L);
        throttle.forget(MINER);

        assertTrue(throttle.shouldSend(MINER, 1_100L));
    }
}
