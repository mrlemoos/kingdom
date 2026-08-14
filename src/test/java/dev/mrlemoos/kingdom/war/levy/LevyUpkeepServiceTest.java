package dev.mrlemoos.kingdom.war.levy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Levy upkeep, arrears and desertion: the day's wage bill charged on the treasury, one public
 * warning the first day it goes unpaid, morale lost each unpaid day thereafter, and desertion off
 * the standing roster for the soldier whose morale sinks to the floor.
 */
class LevyUpkeepServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID SOLDIER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LEVYMAN = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final SeasonProfile SPRING = SeasonProfile.defaults(Season.SPRING);
    private static final LevyUpkeepConfig CONFIG = new LevyUpkeepConfig(true, 2.0, 5.0, MoraleTier.ROUT);

    private FakeTreasury treasury;
    private FakeRoster roster;
    private InMemoryLevyArrearsStore arrearsStore;
    private MoraleService moraleService;
    private LevyUpkeepService service;

    @BeforeEach
    void setUp() {
        treasury = new FakeTreasury();
        roster = new FakeRoster();
        arrearsStore = new InMemoryLevyArrearsStore();
        moraleService = new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled());
        service = new LevyUpkeepService(arrearsStore, CONFIG, moraleService, treasury, roster);
        roster.roster.add(SOLDIER);
    }

    @Test
    void aFullTreasuryPaysTheBillOutrightAndLeavesNoArrears() {
        treasury.balances.put(KINGDOM, 100.0);

        LevyDayOutcome outcome = service.settleDay(KINGDOM, Set.of(LEVYMAN), SPRING, 10L);

        assertEquals(2.0 + 7.5, outcome.billed(), 1.0e-9);
        assertEquals(2.0 + 7.5, outcome.paid(), 1.0e-9);
        assertEquals(0.0, outcome.arrears(), 1.0e-9);
        assertFalse(outcome.warningIssued());
        assertTrue(arrearsStore.find(KINGDOM).isEmpty());
        assertEquals(100.0 - 9.5, treasury.balances.get(KINGDOM), 1.0e-9);
    }

    @Test
    void exactPaymentClearsTheBillAndDrainsTheTreasury() {
        treasury.balances.put(KINGDOM, 2.0);

        LevyDayOutcome outcome = service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        assertEquals(2.0, outcome.paid(), 1.0e-9);
        assertEquals(0.0, outcome.arrears(), 1.0e-9);
        assertEquals(0.0, treasury.balances.get(KINGDOM), 1.0e-9);
        assertTrue(arrearsStore.find(KINGDOM).isEmpty());
    }

    @Test
    void aStandingRosterManWhoAnsweredTheMusterIsChargedOnceAtTheStandingRate() {
        treasury.balances.put(KINGDOM, 100.0);

        LevyDayOutcome outcome = service.settleDay(KINGDOM, Set.of(SOLDIER), SPRING, 10L);

        assertEquals(2.0, outcome.billed(), 1.0e-9);
    }

    @Test
    void aPartialPaymentLeavesTheShortfallAsArrears() {
        treasury.balances.put(KINGDOM, 1.0);

        LevyDayOutcome outcome = service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        assertEquals(1.0, outcome.paid(), 1.0e-9);
        assertEquals(1.0, outcome.arrears(), 1.0e-9);
        Optional<LevyArrears> stored = arrearsStore.find(KINGDOM);
        assertTrue(stored.isPresent());
        assertEquals(1.0, stored.get().amount(), 1.0e-9);
        assertEquals(10L, stored.get().lastPaidDay());
    }

    @Test
    void yesterdaysArrearsAreCarriedOntoTodaysBill() {
        treasury.balances.put(KINGDOM, 0.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        LevyDayOutcome second = service.settleDay(KINGDOM, Set.of(), SPRING, 11L);

        assertEquals(4.0, second.arrears(), 1.0e-9);
    }

    @Test
    void theFirstUnpaidDayWarnsTheRealmOnceAndCostsNoMorale() {
        treasury.balances.put(KINGDOM, 0.0);

        LevyDayOutcome first = service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        assertTrue(first.warningIssued());
        assertFalse(first.announcements().isEmpty());
        assertTrue(first.demoralised().isEmpty(), "nobody loses morale on the warning day");
        assertTrue(moraleService.tierOf(SOLDIER).isEmpty());

        LevyDayOutcome second = service.settleDay(KINGDOM, Set.of(), SPRING, 11L);
        assertFalse(second.warningIssued(), "the realm is warned once, not every unpaid day");
    }

    @Test
    void moraleStepsDownOncePerUnpaidDayAfterTheWarning() {
        treasury.balances.put(KINGDOM, 0.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        service.settleDay(KINGDOM, Set.of(), SPRING, 11L);
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER));

        service.settleDay(KINGDOM, Set.of(), SPRING, 12L);
        assertEquals(Optional.of(MoraleTier.BREAKING), moraleService.tierOf(SOLDIER));
    }

    @Test
    void aSoldierWhoseMoraleReachesTheFloorDesertsTheStandingRoster() {
        treasury.balances.put(KINGDOM, 0.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 10L);
        service.settleDay(KINGDOM, Set.of(), SPRING, 11L);
        service.settleDay(KINGDOM, Set.of(), SPRING, 12L);

        LevyDayOutcome outcome = service.settleDay(KINGDOM, Set.of(), SPRING, 13L);

        assertEquals(Optional.of(MoraleTier.ROUT), moraleService.tierOf(SOLDIER));
        assertTrue(outcome.deserters().contains(SOLDIER));
        assertFalse(roster.roster.contains(SOLDIER), "a deserter is off the standing roster outright");
    }

    @Test
    void payingTheArrearsOffStopsFurtherDecayButRestoresNobody() {
        treasury.balances.put(KINGDOM, 0.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 10L);
        service.settleDay(KINGDOM, Set.of(), SPRING, 11L);
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER));

        treasury.balances.put(KINGDOM, 500.0);
        LevyDayOutcome paid = service.settleDay(KINGDOM, Set.of(), SPRING, 12L);
        assertEquals(0.0, paid.arrears(), 1.0e-9);
        assertTrue(paid.demoralised().isEmpty());
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER), "paying up restores nobody");

        service.settleDay(KINGDOM, Set.of(), SPRING, 13L);
        assertEquals(Optional.of(MoraleTier.SHAKEN), moraleService.tierOf(SOLDIER));
        assertTrue(roster.roster.contains(SOLDIER));
    }

    @Test
    void payingUpAndFallingBehindAgainWarnsTheRealmAfresh() {
        treasury.balances.put(KINGDOM, 0.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 10L);

        treasury.balances.put(KINGDOM, 500.0);
        service.settleDay(KINGDOM, Set.of(), SPRING, 11L);

        treasury.balances.put(KINGDOM, 0.0);
        assertTrue(service.settleDay(KINGDOM, Set.of(), SPRING, 12L).warningIssued());
    }

    @Test
    void disabledUpkeepChargesNothing() {
        LevyUpkeepService off = new LevyUpkeepService(
                arrearsStore, new LevyUpkeepConfig(false, 2.0, 5.0, MoraleTier.ROUT), moraleService, treasury, roster);
        treasury.balances.put(KINGDOM, 100.0);

        LevyDayOutcome outcome = off.settleDay(KINGDOM, Set.of(LEVYMAN), SPRING, 10L);

        assertEquals(0.0, outcome.billed(), 1.0e-9);
        assertEquals(100.0, treasury.balances.get(KINGDOM), 1.0e-9);
    }

    private static final class FakeTreasury implements LevyTreasury {
        private final Map<String, Double> balances = new LinkedHashMap<>();

        @Override
        public double debit(String kingdomId, double amount) {
            double available = balances.getOrDefault(kingdomId, 0.0);
            double taken = Math.min(available, amount);
            balances.put(kingdomId, available - taken);
            return taken;
        }
    }

    private static final class FakeRoster implements LevyRoster {
        private final Set<UUID> roster = new LinkedHashSet<>();

        @Override
        public Set<UUID> standingRoster(String kingdomId) {
            return Set.copyOf(roster);
        }

        @Override
        public void desert(String kingdomId, UUID playerId) {
            roster.remove(playerId);
        }
    }
}
