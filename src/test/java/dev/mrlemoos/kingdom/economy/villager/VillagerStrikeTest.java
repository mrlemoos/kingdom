package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class VillagerStrikeTest {

    @Test
    void notOnStrikeWhenWalletIsNotFrozen() {
        assertFalse(VillagerStrike.isOnStrike(Optional.empty(), 20L, 7, 30));
    }

    @Test
    void notOnStrikeBeforeThreshold() {
        assertFalse(VillagerStrike.isOnStrike(Optional.of(10L), 16L, 7, 30));
    }

    @Test
    void onStrikeOnceFrozenForStrikeDays() {
        assertTrue(VillagerStrike.isOnStrike(Optional.of(10L), 17L, 7, 30));
    }

    @Test
    void notOnStrikeWhenStrikeDaysDisabled() {
        assertFalse(VillagerStrike.isOnStrike(Optional.of(0L), 100L, 0, 30));
    }

    @Test
    void rejectsStrikeThresholdNotShorterThanEscheat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VillagerStrike.isOnStrike(Optional.of(0L), 10L, 30, 30));
        assertThrows(
                IllegalArgumentException.class,
                () -> VillagerStrike.isOnStrike(Optional.of(0L), 10L, 31, 30));
    }

    @Test
    void allowsDisabledStrikeRegardlessOfEscheat() {
        assertFalse(VillagerStrike.isOnStrike(Optional.of(0L), 100L, 0, 0));
    }
}
