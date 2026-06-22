package dev.leo.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityCooldownTrackerTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private EconomyConfig config;
    private ActivityCooldownTracker tracker;

    @BeforeEach
    void setUp() {
        config = EconomyConfig.defaults();
        tracker = new ActivityCooldownTracker(config);
    }

    @Test
    void canEarnWhenNoPriorActivity() {
        assertTrue(tracker.canEarn(ActivityCategory.HARVEST, PLAYER, 1_000L));
    }

    @Test
    void harvestCooldownBlocksUntilElapsed() {
        tracker.record(ActivityCategory.HARVEST, PLAYER, 1_000L);

        assertFalse(tracker.canEarn(ActivityCategory.HARVEST, PLAYER, 3_999L));
        assertTrue(tracker.canEarn(ActivityCategory.HARVEST, PLAYER, 4_000L));
    }

    @Test
    void craftCooldownIsShorterThanHarvest() {
        tracker.record(ActivityCategory.CRAFT, PLAYER, 0L);

        assertFalse(tracker.canEarn(ActivityCategory.CRAFT, PLAYER, 999L));
        assertTrue(tracker.canEarn(ActivityCategory.CRAFT, PLAYER, 1_000L));
    }

    @Test
    void villagerTradeCooldownIsIndependent() {
        tracker.record(ActivityCategory.VILLAGER_TRADE, PLAYER, 0L);

        assertTrue(tracker.canEarn(ActivityCategory.HARVEST, PLAYER, 100L));
        assertFalse(tracker.canEarn(ActivityCategory.VILLAGER_TRADE, PLAYER, 4_999L));
        assertTrue(tracker.canEarn(ActivityCategory.VILLAGER_TRADE, PLAYER, 5_000L));
    }

    @Test
    void playerTradeCooldownBlocksUntilElapsed() {
        tracker.record(ActivityCategory.PLAYER_TRADE, PLAYER, 0L);

        assertFalse(tracker.canEarn(ActivityCategory.PLAYER_TRADE, PLAYER, 9_999L));
        assertTrue(tracker.canEarn(ActivityCategory.PLAYER_TRADE, PLAYER, 10_000L));
    }

    @Test
    void cooldownsArePerPlayer() {
        UUID other = UUID.randomUUID();
        tracker.record(ActivityCategory.HARVEST, PLAYER, 1_000L);

        assertFalse(tracker.canEarn(ActivityCategory.HARVEST, PLAYER, 2_000L));
        assertTrue(tracker.canEarn(ActivityCategory.HARVEST, other, 2_000L));
    }
}
