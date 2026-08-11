package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.economy.wealth.EstateBlockPlacer;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.PreparedPublicWork;
import dev.mrlemoos.kingdom.parliament.ParliamentEnactment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicWorkTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private EconomyService economyService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(PREMIER, "northmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        parliamentService.setTerritoryResolver(ownTerritory());
        economyService = new EconomyService(0.0);
        economyService.creditTreasury("northmarch", 1_000.0);
        economyService.enactBudget("northmarch", 800.0);
    }

    @Test
    void costEqualsEstateWorthFromRealmWealthRates() {
        assertEquals(500.0, RealmWealthRates.defaults().coronaValue(WealthBlockType.BEACON), 1e-9);
        assertEquals(250.0, RealmWealthRates.defaults().coronaValue(WealthBlockType.CONDUIT), 1e-9);
        assertEquals(50.0, RealmWealthRates.defaults().coronaValue(WealthBlockType.LODESTONE), 1e-9);

        parliamentService.preparePublicWork(
                "northmarch",
                NobleRank.PREMIER,
                new PreparedPublicWork(WealthBlockType.BEACON, "world", 10, 64, 20));

        ParliamentResult tabled = parliamentService.tableSpendPublicWork(
                "northmarch", NobleRank.PREMIER, PREMIER, RealmWealthRates.defaults(), null);

        assertInstanceOf(ParliamentResult.Success.class, tabled);
        BillPayload.SpendPublicWork payload =
                (BillPayload.SpendPublicWork) parliamentService.currentBill("northmarch").orElseThrow().payload();
        assertEquals(BillType.SPEND_PUBLIC_WORK, parliamentService.currentBill("northmarch").orElseThrow().type());
        assertEquals(WealthBlockType.BEACON, payload.estateType());
        assertEquals(500.0, payload.cost(), 1e-9);
        assertEquals("world", payload.worldName());
        assertEquals(10, payload.x());
        assertEquals(64, payload.y());
        assertEquals(20, payload.z());
    }

    @Test
    void prepareRefusesNonEstateType() {
        ParliamentResult result = parliamentService.preparePublicWork(
                "northmarch",
                NobleRank.PREMIER,
                new PreparedPublicWork(WealthBlockType.GOLD_BLOCK, "world", 10, 64, 20));

        assertInstanceOf(ParliamentResult.Failure.class, result);
        assertTrue(((ParliamentResult.Failure) result).message().toLowerCase().contains("estate"));
    }

    @Test
    void prepareRefusesOutsideTerritory() {
        parliamentService.setTerritoryResolver((world, x, y, z, kingdomId) -> TerritoryLocation.wilderness());

        ParliamentResult result = parliamentService.preparePublicWork(
                "northmarch",
                NobleRank.PREMIER,
                new PreparedPublicWork(WealthBlockType.LODESTONE, "world", 10, 64, 20));

        assertInstanceOf(ParliamentResult.Failure.class, result);
        assertTrue(((ParliamentResult.Failure) result).message().toLowerCase().contains("territory"));
    }

    @Test
    void tableRequiresPreparedPublicWork() {
        ParliamentResult tabled = parliamentService.tableSpendPublicWork(
                "northmarch", NobleRank.PREMIER, PREMIER, RealmWealthRates.defaults(), null);

        assertInstanceOf(ParliamentResult.Failure.class, tabled);
    }

    @Test
    void enactmentDebitsEstateWorthAgainstBudgetAndPlacesBlock() {
        List<String> placed = new ArrayList<>();
        EstateBlockPlacer placer = (world, x, y, z, type) -> {
            placed.add(type.configKey() + "@" + world + ":" + x + "," + y + "," + z);
            return true;
        };

        parliamentService.preparePublicWork(
                "northmarch",
                NobleRank.PREMIER,
                new PreparedPublicWork(WealthBlockType.CONDUIT, "world", 5, 70, 9));
        parliamentService.tableSpendPublicWork(
                "northmarch", NobleRank.PREMIER, PREMIER, RealmWealthRates.defaults(), null);

        var bill = parliamentService.currentBill("northmarch").orElseThrow();
        var draft = new ParliamentService.AssentedActDraft(
                bill.kingdomId(),
                bill.id(),
                bill.title(),
                bill.type(),
                1_700_000_000_000L,
                List.of("page"),
                bill.votesView(),
                null,
                bill.payload(),
                List.of());

        EconomyResult enacted = ParliamentEnactment.enact(draft, economyService, 3, placer);

        assertInstanceOf(EconomyResult.Success.class, enacted);
        assertEquals(750.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(250.0, economyService.kingdomEconomies().get("northmarch").budget().spentAmount(), 1e-9);
        assertEquals(List.of("conduit@world:5,70,9"), placed);
    }

    @Test
    void placementFailureRollsBackTreasuryAndBudgetSpend() {
        EstateBlockPlacer placer = (w, x, y, z, t) -> false;

        var payload = new BillPayload.SpendPublicWork(
                WealthBlockType.BEACON, "world", 1, 64, 1, 500.0);
        var draft = new ParliamentService.AssentedActDraft(
                "northmarch",
                "northmarch-1",
                "Supply Act (Public work)",
                BillType.SPEND_PUBLIC_WORK,
                1L,
                List.of("page"),
                java.util.Map.of(),
                null,
                payload,
                List.of());

        EconomyResult enacted = ParliamentEnactment.enact(draft, economyService, 3, placer);

        assertInstanceOf(EconomyResult.Failure.class, enacted);
        assertEquals(1_000.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.0, economyService.kingdomEconomies().get("northmarch").budget().spentAmount(), 1e-9);
    }

    @Test
    void nullPlacerRefusesWithoutSpending() {
        var payload = new BillPayload.SpendPublicWork(
                WealthBlockType.LODESTONE, "world", 1, 64, 1, 50.0);
        var draft = new ParliamentService.AssentedActDraft(
                "northmarch",
                "northmarch-1",
                "Supply Act (Public work)",
                BillType.SPEND_PUBLIC_WORK,
                1L,
                List.of("page"),
                java.util.Map.of(),
                null,
                payload,
                List.of());

        EconomyResult enacted = ParliamentEnactment.enact(draft, economyService, 3, null);

        assertInstanceOf(EconomyResult.Failure.class, enacted);
        assertEquals(1_000.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.0, economyService.kingdomEconomies().get("northmarch").budget().spentAmount(), 1e-9);
    }

    @Test
    void enactmentRefusesWhenBudgetInsufficient() {
        EconomyService tight = new EconomyService(0.0);
        tight.creditTreasury("northmarch", 1_000.0);
        tight.enactBudget("northmarch", 100.0);

        AtomicReference<Boolean> placed = new AtomicReference<>(false);
        EstateBlockPlacer placer = (w, x, y, z, t) -> {
            placed.set(true);
            return true;
        };

        var payload = new BillPayload.SpendPublicWork(
                WealthBlockType.BEACON, "world", 1, 64, 1, 500.0);
        var draft = new ParliamentService.AssentedActDraft(
                "northmarch",
                "northmarch-1",
                "Supply Act (Public work)",
                BillType.SPEND_PUBLIC_WORK,
                1L,
                List.of("page"),
                java.util.Map.of(),
                null,
                payload,
                List.of());

        EconomyResult enacted = ParliamentEnactment.enact(draft, tight, 3, placer);

        assertInstanceOf(EconomyResult.Failure.class, enacted);
        assertTrue(((EconomyResult.Failure) enacted).message().toLowerCase().contains("budget")
                || ((EconomyResult.Failure) enacted).message().toLowerCase().contains("approved"));
        assertEquals(1_000.0, tight.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(false, placed.get());
    }

    private static TerritoryResolver ownTerritory() {
        return (world, x, y, z, kingdomId) -> TerritoryLocation.ownKingdom();
    }
}
