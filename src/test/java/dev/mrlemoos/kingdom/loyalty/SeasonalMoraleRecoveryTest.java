package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Slice 5: the season stretches or shortens the military morale recovery clock. Winter's
 * {@code moraleRecoveryFactor} of 0.5 means twice as long to mend; a factor of 1 leaves the
 * configured wait exactly as it was, and the wait never falls below a single in-game day.
 */
class SeasonalMoraleRecoveryTest {

    private static final UUID SOLDIER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private InMemoryMoraleStore store;
    private MoraleConfig config;

    @BeforeEach
    void setUp() {
        store = new InMemoryMoraleStore();
        config = MoraleConfig.enabled();
    }

    @Test
    void springLeavesTheConfiguredWaitUntouched() {
        assertEquals(
                config.recoveryMcDaysPerTier(),
                config.effectiveRecoveryMcDaysPerTier(SeasonProfile.defaults(Season.SPRING).moraleRecoveryFactor()));
    }

    @Test
    void summerNeverStretchesTheWaitBelowASingleDay() {
        assertEquals(
                1,
                config.effectiveRecoveryMcDaysPerTier(SeasonProfile.defaults(Season.SUMMER).moraleRecoveryFactor()));
    }

    @Test
    void winterDoublesTheDaysNeededPerTier() {
        assertEquals(
                2,
                config.effectiveRecoveryMcDaysPerTier(SeasonProfile.defaults(Season.WINTER).moraleRecoveryFactor()));
    }

    @Test
    void aLongerConfiguredWaitIsShortenedBySummer() {
        MoraleConfig slow = new MoraleConfig(true, 4, 1);
        assertEquals(3, slow.effectiveRecoveryMcDaysPerTier(1.25));
        assertEquals(8, slow.effectiveRecoveryMcDaysPerTier(0.5));
    }

    @Test
    void aNonsensicalFactorFallsBackToTheConfiguredWait() {
        assertEquals(1, config.effectiveRecoveryMcDaysPerTier(0.0));
        assertEquals(1, config.effectiveRecoveryMcDaysPerTier(-2.0));
    }

    @Test
    void inWinterOneDayIsNotEnoughToMend() {
        MoraleService service = new MoraleService(store, config);
        store.putTier(SOLDIER, MoraleTier.SHAKEN);
        double winter = SeasonProfile.defaults(Season.WINTER).moraleRecoveryFactor();

        service.tickRecovery(SOLDIER, 0L, winter);
        service.tickRecovery(SOLDIER, 1L, winter);
        assertEquals(MoraleTier.SHAKEN, store.findTier(SOLDIER).orElseThrow());

        service.tickRecovery(SOLDIER, 2L, winter);
        assertEquals(MoraleTier.STEADFAST, store.findTier(SOLDIER).orElseThrow());
    }

    @Test
    void inSpringTheOldBehaviourStands() {
        MoraleService service = new MoraleService(store, config);
        store.putTier(SOLDIER, MoraleTier.SHAKEN);
        double spring = SeasonProfile.defaults(Season.SPRING).moraleRecoveryFactor();

        service.tickRecovery(SOLDIER, 0L, spring);
        service.tickRecovery(SOLDIER, 1L, spring);
        assertEquals(MoraleTier.STEADFAST, store.findTier(SOLDIER).orElseThrow());
    }

    @Test
    void theSeasonlessOverloadStillMendsInASingleDay() {
        MoraleService service = new MoraleService(store, config);
        store.putTier(SOLDIER, MoraleTier.SHAKEN);

        service.tickRecovery(SOLDIER, 0L);
        service.tickRecovery(SOLDIER, 1L);
        assertEquals(MoraleTier.STEADFAST, store.findTier(SOLDIER).orElseThrow());
    }
}
