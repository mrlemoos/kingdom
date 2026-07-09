package dev.mrlemoos.kingdom.war.crownsquad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Crown squad treasury purchase: spawned mobs funded from an approved war-spending budget,
 * ledgered by placeholder unit id (no Bukkit mob spawn here) and counted against a
 * crown-squad-specific per-kingdom cap. Coordination with {@code ConscriptionService}'s pressed
 * villager cap (Slice 5.1) into a single shared army cap is deferred to Slice 5.3's squad
 * assignment work — this slice's cap is crown-squad-only.
 */
class CrownSquadServiceTest {

    private static final String KINGDOM_ID = "northmarch";

    private EconomyService economyService;
    private CrownSquadService crownSquadService;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService();
        crownSquadService = new CrownSquadService(economyService, new CrownSquadConfig(true, 50.0, 2));
    }

    @Test
    void purchaseFailsWhenCrownSquadsAreDisabled() {
        crownSquadService.setConfig(new CrownSquadConfig(false, 50.0, 2));
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);

        WarResult result = crownSquadService.purchase(KINGDOM_ID);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(crownSquadService.unitsOf(KINGDOM_ID).isEmpty());
    }

    @Test
    void purchaseFailsWhenTreasuryHasInsufficientBalanceDespiteApprovedBudget() {
        economyService.enactBudget(KINGDOM_ID, 500.0);

        WarResult result = crownSquadService.purchase(KINGDOM_ID);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(crownSquadService.unitsOf(KINGDOM_ID).isEmpty());
        assertEquals(0, crownSquadService.countOf(KINGDOM_ID));
    }

    @Test
    void purchaseFailsWhenNoWarSpendingBudgetHasBeenApproved() {
        economyService.creditTreasury(KINGDOM_ID, 500.0);

        WarResult result = crownSquadService.purchase(KINGDOM_ID);

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(500.0, economyService.getTreasuryBalance(KINGDOM_ID));
    }

    @Test
    void purchaseDebitsTreasuryAndLedgersAPlaceholderUnitIdOnSuccess() {
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);

        WarResult result = crownSquadService.purchase(KINGDOM_ID);

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(450.0, economyService.getTreasuryBalance(KINGDOM_ID));
        List<CrownSquadUnit> units = crownSquadService.unitsOf(KINGDOM_ID);
        assertEquals(1, units.size());
        assertEquals(KINGDOM_ID, units.get(0).kingdomId());
    }

    @Test
    void purchaseRejectsOnceTheCrownSquadCapIsReached() {
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);
        crownSquadService.purchase(KINGDOM_ID);
        crownSquadService.purchase(KINGDOM_ID);

        WarResult result = crownSquadService.purchase(KINGDOM_ID);

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(2, crownSquadService.countOf(KINGDOM_ID));
        assertEquals(400.0, economyService.getTreasuryBalance(KINGDOM_ID));
    }

    @Test
    void capIsTrackedIndependentlyPerKingdom() {
        String otherKingdomId = "southreach";
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);
        economyService.creditTreasury(otherKingdomId, 500.0);
        economyService.enactBudget(otherKingdomId, 500.0);
        crownSquadService.purchase(KINGDOM_ID);
        crownSquadService.purchase(KINGDOM_ID);

        WarResult result = crownSquadService.purchase(otherKingdomId);

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(1, crownSquadService.countOf(otherKingdomId));
    }

    @Test
    void demobiliseClearsCrownSquadsForTheKingdom() {
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);
        crownSquadService.purchase(KINGDOM_ID);
        crownSquadService.purchase(KINGDOM_ID);

        crownSquadService.demobilise(KINGDOM_ID);

        assertTrue(crownSquadService.unitsOf(KINGDOM_ID).isEmpty());
        assertEquals(0, crownSquadService.countOf(KINGDOM_ID));
    }

    @Test
    void demobiliseIsANoOpForAKingdomWithNoCrownSquads() {
        crownSquadService.demobilise("unrelated-kingdom");

        assertTrue(crownSquadService.unitsOf("unrelated-kingdom").isEmpty());
    }

    @Test
    void demobiliseGracefullyIgnoresNullOrBlankKingdomId() {
        crownSquadService.demobilise(null);
        crownSquadService.demobilise("  ");
    }

    @Test
    void purchaseUsesTheInjectedIdGeneratorForLedgerEntries() {
        UUID fixedId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AtomicInteger callCount = new AtomicInteger();
        CrownSquadService service = new CrownSquadService(
                economyService, new CrownSquadConfig(true, 50.0, 2), () -> {
                    callCount.incrementAndGet();
                    return fixedId;
                });
        economyService.creditTreasury(KINGDOM_ID, 500.0);
        economyService.enactBudget(KINGDOM_ID, 500.0);

        service.purchase(KINGDOM_ID);

        assertEquals(1, callCount.get());
        assertEquals(fixedId, service.unitsOf(KINGDOM_ID).get(0).unitId());
    }

    @Test
    void purchaseRejectsNullOrBlankKingdomId() {
        assertInstanceOf(WarResult.Failure.class, crownSquadService.purchase(null));
        assertInstanceOf(WarResult.Failure.class, crownSquadService.purchase("  "));
    }
}
