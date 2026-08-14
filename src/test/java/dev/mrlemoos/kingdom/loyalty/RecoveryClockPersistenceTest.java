package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Slice 4.6: the recovery clock lives in the store, so a restart no longer silently resets the
 * wait. Each test rebuilds the service over the same store to stand in for a restart.
 */
class RecoveryClockPersistenceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void loyaltyClockSurvivesAServiceRestart() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 0L);

        LoyaltyService restarted = new LoyaltyService(store, LoyaltyConfig.enabled());
        LoyaltyResult result = restarted.tickRecovery(PLAYER, 3L);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.FAITHFUL, ((LoyaltyResult.Success) result).tier());
    }

    @Test
    void moraleClockSurvivesAServiceRestart() {
        InMemoryMoraleStore store = new InMemoryMoraleStore();
        MoraleService service = new MoraleService(store, MoraleConfig.enabled());
        service.recordSiegeHostileAction(PLAYER, true);
        service.tickRecovery(PLAYER, 0L);

        MoraleService restarted = new MoraleService(store, MoraleConfig.enabled());
        MoraleResult result = restarted.tickRecovery(PLAYER, 3L);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, ((MoraleResult.Success) result).tier());
    }

    @Test
    void freshOffenceRestartsTheStoredClock() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 0L);

        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 3L);

        // The clock restarted at day 3 for Disloyal, so day 5 is still short of the three-day wait.
        service.tickRecovery(PLAYER, 5L);
        assertEquals(LoyaltyTier.DISLOYAL, service.tierOf(PLAYER));

        service.tickRecovery(PLAYER, 6L);
        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
    }

    @Test
    void markForAStaleTierIsIgnoredAndRestarted() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        store.putTier(PLAYER, LoyaltyTier.DOUBTFUL);
        // A mark left behind for a tier the subject no longer holds.
        store.putMark(PLAYER, new RecoveryMark<>(LoyaltyTier.DISLOYAL, 0L));

        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.tickRecovery(PLAYER, 10L);

        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
        assertEquals(Optional.of(new RecoveryMark<>(LoyaltyTier.DOUBTFUL, 10L)), store.findMark(PLAYER));
    }

    @Test
    void reachingTopTierClearsTheStoredMark() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 0L);
        service.tickRecovery(PLAYER, 3L);

        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
        assertTrue(store.findMark(PLAYER).isEmpty());
    }

    @Test
    void pardonClearsTheStoredMark() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 0L);

        service.pardon(PLAYER, dev.mrlemoos.kingdom.model.NobleRank.KING, false);

        assertTrue(store.findMark(PLAYER).isEmpty());
    }

    @Test
    void traitorKeepsNoClock() {
        InMemoryLoyaltyStore store = new InMemoryLoyaltyStore();
        LoyaltyService service = new LoyaltyService(store, LoyaltyConfig.enabled());
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 0L);
        assertFalse(store.findMark(PLAYER).isEmpty());

        service.convictTreason(PLAYER);
        service.tickRecovery(PLAYER, 1L);

        assertTrue(store.findMark(PLAYER).isEmpty());
    }
}
