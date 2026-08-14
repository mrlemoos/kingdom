package dev.mrlemoos.kingdom.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeasonalHostileSpawnListenerTest {

    @Test
    void aNeutralSeasonNeverReinforcesTheDark() {
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.0, 0.0));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.0, 0.5));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.0, 0.999));
    }

    @Test
    void aKindSeasonNeverThinsTheDarkEither() {
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(0.9, 0.0));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(0.9, 0.999));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(0.0, 0.0));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(-1.0, 0.5));
    }

    @Test
    void winterReinforcesInProportionToTheSurplus() {
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(1.35, 0.0));
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(1.35, 0.349));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.35, 0.351));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.35, 0.99));
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(1.5, 0.499));
        assertFalse(SeasonalHostileSpawnListener.shouldReinforce(1.5, 0.5));
    }

    @Test
    void aDoubledFactorAlwaysReinforces() {
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(2.0, 0.0));
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(2.0, 0.999));
        assertTrue(SeasonalHostileSpawnListener.shouldReinforce(3.0, 0.999));
    }
}
