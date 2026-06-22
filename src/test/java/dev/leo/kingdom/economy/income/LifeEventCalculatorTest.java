package dev.leo.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifeEventCalculatorTest {

    private LifeEventCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LifeEventCalculator(EconomyConfig.defaults());
    }

    @Test
    void sleepRewardAppliesOwnKingdomMultiplier() {
        assertEquals(0.8, calculator.calculateSleepReward(false), 1e-9);
        assertEquals(1.2, calculator.calculateSleepReward(true), 1e-9);
    }

    @Test
    void eatRewardAppliesOwnKingdomMultiplier() {
        assertEquals(0.2, calculator.calculateEatReward(false), 1e-9);
        assertEquals(0.3, calculator.calculateEatReward(true), 1e-9);
    }

    @Test
    void buildRewardScalesPerBlock() {
        assertEquals(0.05, calculator.calculateBuildReward(5, 0.0, false), 1e-9);
        assertEquals(0.075, calculator.calculateBuildReward(5, 0.0, true), 1e-9);
    }

    @Test
    void buildRewardRespectsDailyCapBeforeMultiplier() {
        assertEquals(0.5, calculator.calculateBuildReward(100, 0.0, false), 1e-9);
        assertEquals(0.1, calculator.calculateBuildReward(50, 0.4, false), 1e-9);
        assertEquals(0.0, calculator.calculateBuildReward(10, 0.5, false), 1e-9);
    }

    @Test
    void nonPositiveBlockCountReturnsZero() {
        assertEquals(0.0, calculator.calculateBuildReward(0, 0.0, false));
        assertEquals(0.0, calculator.calculateBuildReward(-1, 0.0, false));
    }

    @Test
    void socialRewardAppliesOwnKingdomMultiplier() {
        assertEquals(0.1, calculator.calculateSocialReward(false), 1e-9);
        assertEquals(0.15, calculator.calculateSocialReward(true), 1e-9);
    }
}
