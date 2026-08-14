package dev.mrlemoos.kingdom.hearth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The winter day's reckoning at the hearth: fuel burnt out of the container beside it, warmth told
 * by the distance to a hearth that burnt, and the cold ramp for everybody the warmth did not reach.
 */
class HearthDayServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID NEAR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FAR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final HearthConfig CONFIG = HearthConfig.defaults();

    private InMemoryColdLedgerStore ledger;
    private HearthDayService service;

    private static final class FakeFuel implements HearthFuelStore {

        private int stock;

        FakeFuel(int stock) {
            this.stock = stock;
        }

        @Override
        public int fuelCount() {
            return stock;
        }

        @Override
        public void consumeFuel(int amount) {
            stock -= amount;
        }
    }

    @BeforeEach
    void setUp() {
        ledger = new InMemoryColdLedgerStore();
        service = new HearthDayService(ledger);
    }

    private static ColdSubject subject(UUID id, double x) {
        return new ColdSubject(id, x, 64.0, 0.0, false);
    }

    @Test
    void aFuelledHearthWarmsTheVillagersWithinItsRadius() {
        FakeFuel fuel = new FakeFuel(64);
        ColdDayOutcome outcome = service.settleDay(
                KINGDOM,
                List.of(new HearthSite(0, 64, 0, fuel)),
                List.of(subject(NEAR, 4.0), subject(FAR, 100.0)),
                CONFIG,
                true);

        assertEquals(1, outcome.hearthsBurned());
        assertEquals(1.0, outcome.yieldFactorFor(NEAR), 1e-9);
        assertEquals(CONFIG.coldYieldFactor(), outcome.yieldFactorFor(FAR), 1e-9);
        assertEquals(0, ledger.coldDays(KINGDOM, NEAR));
        assertEquals(1, ledger.coldDays(KINGDOM, FAR));
    }

    @Test
    void fuelIsActuallyConsumed() {
        FakeFuel fuel = new FakeFuel(64);
        service.settleDay(KINGDOM, List.of(new HearthSite(0, 64, 0, fuel)), List.of(subject(NEAR, 1.0)), CONFIG, true);

        assertEquals(64 - CONFIG.fuelPerDay(), fuel.fuelCount());
    }

    @Test
    void anEmptyContainerWarmsNobody() {
        FakeFuel fuel = new FakeFuel(0);
        ColdDayOutcome outcome = service.settleDay(
                KINGDOM, List.of(new HearthSite(0, 64, 0, fuel)), List.of(subject(NEAR, 1.0)), CONFIG, true);

        assertEquals(0, outcome.hearthsBurned());
        assertEquals(CONFIG.coldYieldFactor(), outcome.yieldFactorFor(NEAR), 1e-9);
        assertEquals(1, ledger.coldDays(KINGDOM, NEAR));
    }

    @Test
    void aPartlyStockedContainerBurnsNothingAndWarmsNobody() {
        FakeFuel fuel = new FakeFuel(CONFIG.fuelPerDay() - 1);
        ColdDayOutcome outcome = service.settleDay(
                KINGDOM, List.of(new HearthSite(0, 64, 0, fuel)), List.of(subject(NEAR, 1.0)), CONFIG, true);

        assertEquals(0, outcome.hearthsBurned());
        assertEquals(CONFIG.fuelPerDay() - 1, fuel.fuelCount());
        assertEquals(1, ledger.coldDays(KINGDOM, NEAR));
    }

    @Test
    void theColdRampReducesThenStrikes() {
        List<HearthSite> none = List.of();
        for (int day = 1; day <= 2; day++) {
            ColdDayOutcome outcome = service.settleDay(KINGDOM, none, List.of(subject(FAR, 0.0)), CONFIG, true);
            assertEquals(Set.of(), outcome.striking());
            assertEquals(CONFIG.coldYieldFactor(), outcome.yieldFactorFor(FAR), 1e-9);
        }
        ColdDayOutcome third = service.settleDay(KINGDOM, none, List.of(subject(FAR, 0.0)), CONFIG, true);
        assertEquals(Set.of(FAR), third.striking());
        assertEquals(3, ledger.coldDays(KINGDOM, FAR));
    }

    @Test
    void warmingResetsTheColdCount() {
        service.settleDay(KINGDOM, List.of(), List.of(subject(NEAR, 0.0)), CONFIG, true);
        service.settleDay(KINGDOM, List.of(), List.of(subject(NEAR, 0.0)), CONFIG, true);
        assertEquals(2, ledger.coldDays(KINGDOM, NEAR));

        service.settleDay(
                KINGDOM,
                List.of(new HearthSite(0, 64, 0, new FakeFuel(64))),
                List.of(subject(NEAR, 1.0)),
                CONFIG,
                true);

        assertEquals(0, ledger.coldDays(KINGDOM, NEAR));
    }

    @Test
    void seatedVillagerMpsAndTreasuryLordsNeverStrike() {
        ColdSubject exempt = new ColdSubject(FAR, 0.0, 64.0, 0.0, true);
        for (int day = 1; day <= 5; day++) {
            ColdDayOutcome outcome = service.settleDay(KINGDOM, List.of(), List.of(exempt), CONFIG, true);
            assertFalse(outcome.striking().contains(FAR));
        }
        assertEquals(5, ledger.coldDays(KINGDOM, FAR));
        assertTrue(ColdRamp.strikes(ledger.coldDays(KINGDOM, FAR), CONFIG));
    }

    @Test
    void noHearthIsAskedForOutsideWinter() {
        service.settleDay(KINGDOM, List.of(), List.of(subject(NEAR, 0.0)), CONFIG, true);
        assertEquals(1, ledger.coldDays(KINGDOM, NEAR));

        FakeFuel fuel = new FakeFuel(64);
        ColdDayOutcome outcome = service.settleDay(
                KINGDOM, List.of(new HearthSite(0, 64, 0, fuel)), List.of(subject(NEAR, 0.0)), CONFIG, false);

        assertEquals(0, outcome.hearthsBurned());
        assertEquals(64, fuel.fuelCount());
        assertEquals(1.0, outcome.yieldFactorFor(NEAR), 1e-9);
        assertEquals(Set.of(), outcome.striking());
        assertEquals(0, ledger.coldDays(KINGDOM, NEAR));
    }
}
