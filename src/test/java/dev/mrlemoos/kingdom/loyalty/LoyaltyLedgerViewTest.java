package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Slice 4.8: the ledger reports the live domain and never changes it. */
class LoyaltyLedgerViewTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryLoyaltyStore loyaltyStore;
    private InMemoryMoraleStore moraleStore;
    private LoyaltyService loyalty;
    private MoraleService morale;

    @BeforeEach
    void setUp() {
        loyaltyStore = new InMemoryLoyaltyStore();
        moraleStore = new InMemoryMoraleStore();
        loyalty = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        morale = new MoraleService(moraleStore, MoraleConfig.enabled());
    }

    private LoyaltyLedgerView view(long day) {
        return LoyaltyLedgerView.of(PLAYER, loyalty, morale, day);
    }

    @Test
    void faithfulSubjectIsToldNothingIsOwed() {
        LoyaltyLedgerView.Track political = view(0L).political();

        assertEquals("Faithful", political.tier());
        assertTrue(political.daysToNextTick().isEmpty());
        assertTrue(political.tip().contains("full civil trust"), political.tip());
    }

    @Test
    void recoveringSubjectSeesTheDaysLeftAndTheTaxTip() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        LoyaltyLedgerView.Track political = view(1L).political();

        assertEquals("Doubtful", political.tier());
        assertEquals(2L, political.daysToNextTick().orElseThrow());
        assertTrue(political.tip().contains("income tax"), political.tip());
    }

    @Test
    void creditIsReflectedInTheCountdown() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);
        loyalty.recordServiceCredit(PLAYER, 0L);

        assertEquals(1L, view(1L).political().daysToNextTick().orElseThrow());
    }

    @Test
    void aDueTickReadsAsZeroDaysNotNegative() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        assertEquals(0L, view(9L).political().daysToNextTick().orElseThrow());
    }

    @Test
    void traitorIsPointedAtTheCrownAndHasNoCountdown() {
        loyalty.convictTreason(PLAYER);

        LoyaltyLedgerView.Track political = view(0L).political();

        assertEquals("Traitor", political.tier());
        assertTrue(political.daysToNextTick().isEmpty());
        assertTrue(political.tip().toLowerCase().contains("pardon"), political.tip());
    }

    @Test
    void aClosedMilitaryTrackPointsAtTheOathOfService() {
        LoyaltyLedgerView.Track military = view(0L).military();

        assertEquals("Not open", military.tier());
        assertTrue(military.tip().contains("oath of service"), military.tip());
    }

    @Test
    void aRecoveringMilitaryTrackPointsAtTheMuster() {
        morale.recordSiegeHostileAction(PLAYER, true);
        morale.tickRecovery(PLAYER, 0L);

        LoyaltyLedgerView.Track military = view(0L).military();

        assertEquals("Shaken", military.tier());
        assertEquals(1L, military.daysToNextTick().orElseThrow());
        assertTrue(military.tip().contains("muster"), military.tip());
    }

    @Test
    void routIsPointedAtTheMoralePardon() {
        moraleStore.putTier(PLAYER, MoraleTier.ROUT);

        LoyaltyLedgerView.Track military = view(0L).military();

        assertEquals("Rout", military.tier());
        assertTrue(military.daysToNextTick().isEmpty());
        assertTrue(military.tip().toLowerCase().contains("pardon"), military.tip());
    }

    @Test
    void buildingTheViewNeverChangesTheDomain() {
        loyalty.recordActBreach(PLAYER);
        loyalty.tickRecovery(PLAYER, 0L);

        view(99L);
        view(99L);

        assertEquals(LoyaltyTier.DOUBTFUL, loyalty.tierOf(PLAYER));
        assertEquals(0L, loyaltyStore.findMark(PLAYER).orElseThrow().mcDay());
    }

    @Test
    void aDisabledTrackSaysSo() {
        LoyaltyLedgerView disabled = LoyaltyLedgerView.of(
                PLAYER,
                new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.disabled()),
                new MoraleService(new InMemoryMoraleStore(), MoraleConfig.disabled()),
                0L);

        assertEquals("Disabled", disabled.political().tier());
        assertEquals("Disabled", disabled.military().tier());
    }
}
