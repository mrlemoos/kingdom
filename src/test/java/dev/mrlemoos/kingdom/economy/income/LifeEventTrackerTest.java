package dev.mrlemoos.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifeEventTrackerTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final long DAY_ONE = 19_000L;
    private static final long DAY_TWO = 19_001L;

    private LifeEventTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new LifeEventTracker(EconomyConfig.defaults());
    }

    @Test
    void sleepAllowedOncePerNight() {
        assertTrue(tracker.canClaimSleep(PLAYER, DAY_ONE, 1L));
        tracker.recordSleep(PLAYER, DAY_ONE, 1L, 0.8);

        assertFalse(tracker.canClaimSleep(PLAYER, DAY_ONE, 1L));
        assertTrue(tracker.canClaimSleep(PLAYER, DAY_ONE, 2L));
    }

    @Test
    void eatLimitedToThreePerDay() {
        assertTrue(tracker.canClaimEat(PLAYER, DAY_ONE));
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);

        assertFalse(tracker.canClaimEat(PLAYER, DAY_ONE));
    }

    @Test
    void buildingEarnedTodayTracksBaseAmount() {
        tracker.recordBuild(PLAYER, DAY_ONE, 0.3, 0.45);

        assertEquals(0.3, tracker.buildingEarnedToday(PLAYER, DAY_ONE), 1e-9);
        assertEquals(0.45, tracker.totalEarnedToday(PLAYER, DAY_ONE), 1e-9);
    }

    @Test
    void socialRewardRespectsInterval() {
        tracker.recordSocial(PLAYER, DAY_ONE, 0L, 0.1);

        assertFalse(tracker.canClaimSocial(PLAYER, DAY_ONE, 299_999L));
        assertTrue(tracker.canClaimSocial(PLAYER, DAY_ONE, 300_000L));
    }

    @Test
    void dailyCapBlocksFurtherEarnings() {
        tracker.recordSleep(PLAYER, DAY_ONE, 1L, 1.2);
        tracker.recordEat(PLAYER, DAY_ONE, 0.3);
        tracker.recordEat(PLAYER, DAY_ONE, 0.3);
        tracker.recordEat(PLAYER, DAY_ONE, 0.3);

        assertEquals(2.1, tracker.totalEarnedToday(PLAYER, DAY_ONE), 1e-9);
        assertFalse(tracker.canEarnWithinDailyCap(PLAYER, DAY_ONE, 0.1));
        assertEquals(0.0, tracker.remainingDailyCap(PLAYER, DAY_ONE), 1e-9);
    }

    @Test
    void stateResetsOnNewEpochDay() {
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);
        tracker.recordEat(PLAYER, DAY_ONE, 0.2);

        assertFalse(tracker.canClaimEat(PLAYER, DAY_ONE));
        assertTrue(tracker.canClaimEat(PLAYER, DAY_TWO));
        assertEquals(0.0, tracker.totalEarnedToday(PLAYER, DAY_TWO), 1e-9);
    }

    @Test
    void remainingDailyCapReflectsEarnedTotal() {
        tracker.recordSocial(PLAYER, DAY_ONE, 0L, 0.5);

        assertEquals(1.5, tracker.remainingDailyCap(PLAYER, DAY_ONE), 1e-9);
        assertTrue(tracker.canEarnWithinDailyCap(PLAYER, DAY_ONE, 1.5));
        assertFalse(tracker.canEarnWithinDailyCap(PLAYER, DAY_ONE, 1.51));
    }
}
