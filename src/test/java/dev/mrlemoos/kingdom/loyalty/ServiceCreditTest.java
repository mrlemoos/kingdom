package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Slice 4.7: an act of service brings the running recovery clock forward. It never grants a tier
 * outright, cannot bank more than the current tier's wait however often it is repeated, and never
 * touches Traitor or Rout.
 */
class ServiceCreditTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryLoyaltyStore loyaltyStore;
    private LoyaltyService loyalty;

    @BeforeEach
    void setUp() {
        loyaltyStore = new InMemoryLoyaltyStore();
        loyalty = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
    }

    @Test
    void creditBringsTheNextTickForwardByTheConfiguredDays() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        LoyaltyResult credited = loyalty.recordServiceCredit(PLAYER, 0L);
        assertInstanceOf(LoyaltyResult.Success.class, credited);

        // Three-day wait less one day of credit: day 2 now recovers where day 2 would not have.
        loyalty.tickRecovery(PLAYER, 2L);
        assertEquals(LoyaltyTier.FAITHFUL, loyalty.tierOf(PLAYER));
    }

    @Test
    void creditNeverGrantsATierOnItsOwn() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        loyalty.recordServiceCredit(PLAYER, 0L);
        loyalty.recordServiceCredit(PLAYER, 0L);
        loyalty.recordServiceCredit(PLAYER, 0L);
        loyalty.recordServiceCredit(PLAYER, 0L);

        assertEquals(LoyaltyTier.DOUBTFUL, loyalty.tierOf(PLAYER));
    }

    @Test
    void creditPastTheThresholdStillTicksAtMostOneTier() {
        loyalty.recordActBreach(PLAYER);
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        // Far more credit than the wait needs.
        for (int i = 0; i < 10; i++) {
            loyalty.recordServiceCredit(PLAYER, 0L);
        }
        loyalty.tickRecovery(PLAYER, 0L);

        assertEquals(LoyaltyTier.DOUBTFUL, loyalty.tierOf(PLAYER));
    }

    @Test
    void repeatedCreditCannotBankBeyondTheCurrentTiersWait() {
        loyalty.recordActBreach(PLAYER);
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        // A whole day's worth of tax payments on day 0.
        for (int i = 0; i < 20; i++) {
            loyalty.recordServiceCredit(PLAYER, 0L);
        }

        loyalty.tickRecovery(PLAYER, 0L);
        assertEquals(LoyaltyTier.DOUBTFUL, loyalty.tierOf(PLAYER));
        // The clamp reset the clock to day 0 for Doubtful, so the next tier still costs a full wait.
        loyalty.tickRecovery(PLAYER, 2L);
        assertEquals(LoyaltyTier.DOUBTFUL, loyalty.tierOf(PLAYER));
        loyalty.tickRecovery(PLAYER, 3L);
        assertEquals(LoyaltyTier.FAITHFUL, loyalty.tierOf(PLAYER));
    }

    @Test
    void creditWithNoClockRunningIsANoOp() {
        LoyaltyResult result = loyalty.recordServiceCredit(PLAYER, 0L);

        assertInstanceOf(LoyaltyResult.Failure.class, result);
        assertTrue(loyaltyStore.findMark(PLAYER).isEmpty());
        assertEquals(LoyaltyTier.FAITHFUL, loyalty.tierOf(PLAYER));
    }

    @Test
    void traitorTakesNoCredit() {
        loyalty.convictTreason(PLAYER);

        LoyaltyResult result = loyalty.recordServiceCredit(PLAYER, 0L);

        assertInstanceOf(LoyaltyResult.Failure.class, result);
        assertEquals(LoyaltyTier.TRAITOR, loyalty.tierOf(PLAYER));
    }

    @Test
    void faithfulTakesNoCredit() {
        assertInstanceOf(LoyaltyResult.Failure.class, loyalty.recordServiceCredit(PLAYER, 0L));
    }

    @Test
    void creditIsIgnoredWhenTheTrackIsDisabled() {
        LoyaltyService disabled = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.disabled());

        assertInstanceOf(LoyaltyResult.Disabled.class, disabled.recordServiceCredit(PLAYER, 0L));
    }

    @Test
    void pardonAfterCreditStillClearsTheClock() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);
        loyalty.recordServiceCredit(PLAYER, 0L);

        loyalty.pardon(PLAYER, NobleRank.KING, false);

        assertTrue(loyaltyStore.findMark(PLAYER).isEmpty());
    }

    @Test
    void moraleCreditShortensTheMilitaryClock() {
        InMemoryMoraleStore store = new InMemoryMoraleStore();
        // Two in-game days per tier so a one-day credit is observable.
        MoraleService morale = new MoraleService(store, new MoraleConfig(true, 2, 1));
        morale.recordSiegeHostileAction(PLAYER, true);
        morale.tickRecovery(PLAYER, 0L);

        morale.recordServiceCredit(PLAYER, 0L);
        morale.tickRecovery(PLAYER, 1L);

        assertEquals(MoraleTier.STEADFAST, store.findTier(PLAYER).orElseThrow());
    }

    @Test
    void routTakesNoCredit() {
        InMemoryMoraleStore store = new InMemoryMoraleStore();
        MoraleService morale = new MoraleService(store, MoraleConfig.enabled());
        store.putTier(PLAYER, MoraleTier.ROUT);

        assertInstanceOf(MoraleResult.Failure.class, morale.recordServiceCredit(PLAYER, 0L));
        assertEquals(MoraleTier.ROUT, store.findTier(PLAYER).orElseThrow());
    }

    @Test
    void moraleCreditWithAClosedTrackIsANoOp() {
        MoraleService morale = new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled());

        assertInstanceOf(MoraleResult.Failure.class, morale.recordServiceCredit(PLAYER, 0L));
    }
}
