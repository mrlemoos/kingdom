package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The winter day's reckoning at the table: an unfed realm lengthens every villager's run of hungry
 * days, a fed one wipes the slate, and from the seventh day the lot takes one of them.
 */
class HungerDayServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SPARED = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final GranaryConfig CONFIG = GranaryConfig.defaults();

    private InMemoryHungerLedgerStore ledger;
    private final List<List<UUID>> lotsDrawn = new ArrayList<>();
    private HungerDayService service;

    /** The lot as a test can see it: the first name in the hat, and every hat it was offered. */
    private StarvationLot firstOfTheLot() {
        return candidates -> {
            lotsDrawn.add(List.copyOf(candidates));
            return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
        };
    }

    /** A lot that reads the hat and takes nobody, so a run of hungry days is never cut short. */
    private StarvationLot mercifulLot() {
        return candidates -> {
            lotsDrawn.add(List.copyOf(candidates));
            return Optional.empty();
        };
    }

    @BeforeEach
    void setUp() {
        ledger = new InMemoryHungerLedgerStore();
        lotsDrawn.clear();
        service = new HungerDayService(ledger, firstOfTheLot());
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

        HungerDayOutcome fed = service.settleDay(KINGDOM, List.of(subject(FIRST)), false, 272L, CONFIG);

        assertEquals(0, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(1.0, fed.yieldFactorFor(FIRST), 1e-9);
        assertEquals(Set.of(), fed.striking());
        assertTrue(fed.starved().isEmpty());
    }

    @Test
    void theSeventhUnfedDayTakesOneVillagerAndOneOnly() {
        for (long day = 270L; day < 276L; day++) {
            HungerDayOutcome outcome = service.settleDay(
                    KINGDOM, List.of(subject(FIRST), subject(SECOND)), true, day, CONFIG);
            assertTrue(outcome.starved().isEmpty());
        }

        HungerDayOutcome seventh = unfedDay(276L, subject(FIRST), subject(SECOND));

        assertTrue(seventh.famine());
        assertEquals(Set.of(FIRST, SECOND), seventh.starving());
        assertTrue(seventh.starved().isPresent());
        assertEquals(FIRST, seventh.starved().get());
        assertEquals(0, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(7, ledger.hungryDays(KINGDOM, SECOND));
    }

    @Test
    void theLotIsDrawnOnlyFromThoseSevenDaysHungry() {
        service = new HungerDayService(ledger, mercifulLot());
        for (long day = 270L; day <= 276L; day++) {
            service.settleDay(KINGDOM, List.of(subject(FIRST)), true, day, CONFIG);
        }
        // The second villager arrives late and has one hungry day behind it, so is never in the hat.
        HungerDayOutcome outcome = service.settleDay(
                KINGDOM, List.of(subject(SECOND), subject(FIRST)), true, 277L, CONFIG);

        assertEquals(List.of(FIRST), lotsDrawn.get(lotsDrawn.size() - 1));
        assertEquals(Set.of(FIRST), outcome.starving());
        assertEquals(1, ledger.hungryDays(KINGDOM, SECOND));
    }

    @Test
    void theSparedNeitherStrikeNorStarveButStillGoShort() {
        HungerSubject spared = new HungerSubject(SPARED, true);
        for (long day = 270L; day <= 278L; day++) {
            HungerDayOutcome each = service.settleDay(KINGDOM, List.of(spared), true, day, CONFIG);
            assertTrue(each.striking().isEmpty());
            assertTrue(each.starved().isEmpty());
            assertTrue(each.starving().isEmpty());
        }
        HungerDayOutcome tenth = service.settleDay(KINGDOM, List.of(spared), true, 279L, CONFIG);

        assertEquals(10, ledger.hungryDays(KINGDOM, SPARED));
        assertTrue(HungerRamp.starves(ledger.hungryDays(KINGDOM, SPARED), CONFIG));
        assertEquals(CONFIG.hungerYieldFactor(), tenth.yieldFactorFor(SPARED), 1e-9);
        assertTrue(lotsDrawn.isEmpty());
        assertFalse(tenth.famine());
    }

    @Test
    void aSecondSweepInTheSameRealmDayNeitherLengthensTheHungerNorTakesAnother() {
        for (long day = 270L; day <= 276L; day++) {
            service.settleDay(KINGDOM, List.of(subject(FIRST), subject(SECOND)), true, day, CONFIG);
        }
        assertEquals(7, ledger.hungryDays(KINGDOM, SECOND));
        int lots = lotsDrawn.size();

        HungerDayOutcome again = unfedDay(276L, subject(FIRST), subject(SECOND));

        assertEquals(7, ledger.hungryDays(KINGDOM, SECOND));
        assertEquals(lots, lotsDrawn.size());
        assertTrue(again.starved().isEmpty());
        assertEquals(CONFIG.hungerYieldFactor(), again.yieldFactorFor(SECOND), 1e-9);
    }

    @Test
    void eachRealmKeepsItsOwnHunger() {
        unfedDay(270L, subject(FIRST));
        service.settleDay("southreach", List.of(subject(FIRST)), false, 270L, CONFIG);

        assertEquals(1, ledger.hungryDays(KINGDOM, FIRST));
        assertEquals(0, ledger.hungryDays("southreach", FIRST));
    }

    @Test
    void theRandomLotOnlyEverDrawsFromTheHat() {
        StarvationLot lot = StarvationLot.random(new Random(1234L));

        assertTrue(lot.draw(List.of()).isEmpty());
        for (int attempt = 0; attempt < 50; attempt++) {
            Optional<UUID> drawn = lot.draw(List.of(FIRST, SECOND));
            assertTrue(drawn.isPresent());
            assertTrue(drawn.get().equals(FIRST) || drawn.get().equals(SECOND));
        }
    }
}
