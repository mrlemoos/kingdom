package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The winter day's reckoning at the table: an unfed realm lengthens every villager's run of hungry
 * days, while a fed day wipes the slate without harming villagers.
 */
class HungerDayServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SPARED = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final GranaryConfig CONFIG = GranaryConfig.defaults();

    private InMemoryHungerLedgerStore ledger;
    private HungerDayService service;

    @BeforeEach
    void setUp() {
        ledger = new InMemoryHungerLedgerStore();
        service = new HungerDayService(ledger);
    }

    private static HungerSubject subject(UUID id) {
        return new HungerSubject(id, false);
    }

    private HungerDayOutcome unfedDay(long realmDay, HungerSubject... subjects) {
        return service.settleDay(KINGDOM, List.of(subjects), true, realmDay, CONFIG);
    }

    @Test
    void anUnfedDayCostsEveryVillagerPartOfItsYield() {
        HungerDayOutcome outcome = unfedDay(270L, subject(FIRST), subject(SECOND));

        assertEquals(CONFIG.hungerYieldFactor(), outcome.yieldFactorFor(FIRST), 1e-9);
        assertEquals(CONFIG.hungerYieldFactor(), outcome.yieldFactorFor(SECOND), 1e-9);
        assertEquals(1, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(Set.of(), outcome.striking());
        assertFalse(outcome.famine());
    }

    @Test
    void theThirdUnfedDayDownsTools() {
        unfedDay(270L, subject(FIRST));
        unfedDay(271L, subject(FIRST));
        HungerDayOutcome third = unfedDay(272L, subject(FIRST));

        assertEquals(Set.of(FIRST), third.striking());
        assertEquals(3, ledger.hungryDays(KINGDOM, FIRST));
    }

    @Test
    void aFedDayWipesTheSlate() {
        unfedDay(270L, subject(FIRST));
        unfedDay(271L, subject(FIRST));
        assertEquals(2, ledger.hungryDays(KINGDOM, FIRST));
        ledger.setHungryDays(KINGDOM, SECOND, 69);

        HungerDayOutcome fed = service.settleDay(KINGDOM, List.of(subject(FIRST)), false, 272L, CONFIG);

        assertEquals(0, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(0, ledger.hungryDays(KINGDOM, SECOND));
        assertEquals(1.0, fed.yieldFactorFor(FIRST), 1e-9);
        assertEquals(Set.of(), fed.striking());
    }

    @Test
    void prolongedHungerRemainsReversible() {
        HungerDayOutcome last = null;
        for (long day = 270L; day < 300L; day++) {
            last = unfedDay(day, subject(FIRST), subject(SECOND));
        }
        assertTrue(last != null && last.famine());
        assertEquals(Set.of(FIRST, SECOND), last.starving());
        assertEquals(30, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(30, ledger.hungryDays(KINGDOM, SECOND));

        HungerDayOutcome fed = service.settleDay(
                KINGDOM, List.of(subject(FIRST), subject(SECOND)), false, 300L, CONFIG);
        assertFalse(fed.famine());
        assertEquals(0, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(0, ledger.hungryDays(KINGDOM, SECOND));
    }

    @Test
    void aLateArrivalStartsItsOwnHungerCount() {
        for (long day = 270L; day <= 276L; day++) {
            service.settleDay(KINGDOM, List.of(subject(FIRST)), true, day, CONFIG);
        }
        HungerDayOutcome outcome = service.settleDay(
                KINGDOM, List.of(subject(SECOND), subject(FIRST)), true, 277L, CONFIG);

        assertEquals(Set.of(FIRST), outcome.starving());
        assertEquals(1, ledger.hungryDays(KINGDOM, SECOND));
    }

    @Test
    void theSparedNeitherStrikeNorEnterFamineButStillGoShort() {
        HungerSubject spared = new HungerSubject(SPARED, true);
        for (long day = 270L; day <= 278L; day++) {
            HungerDayOutcome each = service.settleDay(KINGDOM, List.of(spared), true, day, CONFIG);
            assertTrue(each.striking().isEmpty());
            assertTrue(each.starving().isEmpty());
        }
        HungerDayOutcome tenth = service.settleDay(KINGDOM, List.of(spared), true, 279L, CONFIG);

        assertEquals(10, ledger.hungryDays(KINGDOM, SPARED));
        assertTrue(HungerRamp.starves(ledger.hungryDays(KINGDOM, SPARED), CONFIG));
        assertEquals(CONFIG.hungerYieldFactor(), tenth.yieldFactorFor(SPARED), 1e-9);
        assertFalse(tenth.famine());
    }

    @Test
    void aSecondSweepInTheSameRealmDayDoesNotLengthenHunger() {
        for (long day = 270L; day <= 276L; day++) {
            service.settleDay(KINGDOM, List.of(subject(FIRST), subject(SECOND)), true, day, CONFIG);
        }
        assertEquals(7, ledger.hungryDays(KINGDOM, SECOND));
        HungerDayOutcome again = unfedDay(276L, subject(FIRST), subject(SECOND));

        assertEquals(7, ledger.hungryDays(KINGDOM, SECOND));
        assertEquals(CONFIG.hungerYieldFactor(), again.yieldFactorFor(SECOND), 1e-9);
    }

    @Test
    void eachRealmKeepsItsOwnHunger() {
        unfedDay(270L, subject(FIRST));
        service.settleDay("southreach", List.of(subject(FIRST)), false, 270L, CONFIG);

        assertEquals(1, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(0, ledger.hungryDays("southreach", FIRST));
    }

}
