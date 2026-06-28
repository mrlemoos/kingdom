package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalProposal;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.model.TreasuryBudget;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.economy.model.VillagerWalletState;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlEconomyStoreTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void roundTripPreservesWalletsAndKingdomFiscalState() {
        EconomyService source = populatedService();

        YamlConfiguration config = new YamlConfiguration();
        YamlEconomyStore.writeConfiguration(config, source);

        EconomyService loaded = new EconomyService();
        YamlEconomyStore.applyConfiguration(config, loaded);

        assertEquals(42.5, loaded.getWalletBalance(ALICE));
        assertEquals(250.0, loaded.getTreasuryBalance("northmarch"));

        KingdomEconomy economy = loaded.kingdomEconomies().get("northmarch");
        FiscalRates activeRates = economy.activeRates();
        assertEquals(0.12, activeRates.baseRate(), 1e-9);
        assertEquals(0.06, activeRates.foreignSurcharge(), 1e-9);
        assertEquals(0.04, activeRates.transferFee(), 1e-9);
        assertEquals(0.09, activeRates.crossKingdomTransferFee(), 1e-9);
        assertEquals(-0.03, activeRates.rankModifier(NobleRank.PREMIER), 1e-9);

        FiscalProposal proposal = economy.pendingProposal().orElseThrow();
        assertEquals(PREMIER, proposal.proposerId());
        assertEquals(1_700_000_000_000L, proposal.timestampMillis());
        assertEquals(0.11, proposal.proposedRates().baseRate(), 1e-9);

        assertEquals(100.0, economy.budget().approvedAmount(), 1e-9);
        assertEquals(35.0, economy.budget().spentAmount(), 1e-9);
        assertEquals(12.5, economy.totalTaxRevenue(), 1e-9);
        assertEquals(88.0, economy.totalGdpRevenue(), 1e-9);
        assertEquals(6.25, economy.lastDailyGdp(), 1e-9);

        List<MintLocation> mints = economy.mintLocations();
        assertEquals(1, mints.size());
        assertEquals("world", mints.getFirst().worldName());
        assertEquals(10, mints.getFirst().x());
        assertEquals(64, mints.getFirst().y());
        assertEquals(10, mints.getFirst().z());
    }

    @Test
    void roundTripPreservesTreasuryLordUuid() {
        UUID lordId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        List<MintLocation> mints = List.of(new MintLocation("world", 10, 64, 10, lordId.toString()));
        KingdomEconomy economy = new KingdomEconomy(0.0, FiscalRates.defaults(), null, new TreasuryBudget(), mints);

        EconomyService source = new EconomyService();
        source.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        YamlConfiguration config = new YamlConfiguration();
        YamlEconomyStore.writeConfiguration(config, source);

        EconomyService loaded = new EconomyService();
        YamlEconomyStore.applyConfiguration(config, loaded);

        MintLocation mint = loaded.kingdomEconomies().get("northmarch").mintLocations().getFirst();
        assertEquals(lordId, mint.lordEntityId().orElseThrow());
    }

    @Test
    void roundTripPreservesTerritoryWealthCounts() {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        counts.set(WealthBlockType.GOLD_BLOCK, 3);
        counts.set(WealthBlockType.BEACON, 1);
        KingdomEconomy economy = new KingdomEconomy(
                10.0, 0.0, 0.0, 0.0, FiscalRates.defaults(), null, new TreasuryBudget(), List.of(), counts);

        EconomyService source = new EconomyService();
        source.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        YamlConfiguration config = new YamlConfiguration();
        YamlEconomyStore.writeConfiguration(config, source);

        EconomyService loaded = new EconomyService();
        YamlEconomyStore.applyConfiguration(config, loaded);

        TerritoryWealthCounts loadedCounts = loaded.getTerritoryWealthCounts("northmarch");
        assertEquals(3, loadedCounts.count(WealthBlockType.GOLD_BLOCK));
        assertEquals(1, loadedCounts.count(WealthBlockType.BEACON));
    }

    @Test
    void roundTripPreservesVillagerWallets() {
        UUID villagerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        VillagerWalletState wallet = new VillagerWalletState(12.5, 7L);
        EconomyService source = new EconomyService();
        source.replaceState(Map.of(), Map.of("northmarch", Map.of(villagerId, wallet)), Map.of());

        YamlConfiguration config = new YamlConfiguration();
        YamlEconomyStore.writeConfiguration(config, source);

        EconomyService loaded = new EconomyService();
        YamlEconomyStore.applyConfiguration(config, loaded);

        assertEquals(12.5, loaded.getVillagerWalletBalance("northmarch", villagerId), 1e-9);
        assertTrue(loaded.isVillagerWalletFrozen("northmarch", villagerId));
    }

    @Test
    void applyConfigurationLeavesServiceEmptyWhenSectionsMissing() {
        YamlConfiguration config = new YamlConfiguration();
        EconomyService service = new EconomyService();

        YamlEconomyStore.applyConfiguration(config, service);

        assertTrue(service.wallets().isEmpty());
        assertTrue(service.kingdomEconomies().isEmpty());
    }

    private static EconomyService populatedService() {
        Map<NobleRank, Double> rankModifiers = new EnumMap<>(NobleRank.class);
        rankModifiers.put(NobleRank.PREMIER, -0.03);
        FiscalRates activeRates = new FiscalRates(0.12, 0.06, 0.04, 0.09, rankModifiers);
        FiscalRates proposedRates = new FiscalRates(0.11, 0.05, 0.03, 0.08, Map.of());
        FiscalProposal proposal = new FiscalProposal(proposedRates, PREMIER, 1_700_000_000_000L);
        TreasuryBudget budget = new TreasuryBudget(100.0, 35.0);
        List<MintLocation> mints = List.of(new MintLocation("world", 10, 64, 10));

        KingdomEconomy economy = new KingdomEconomy(250.0, 12.5, 88.0, 6.25, activeRates, proposal, budget, mints);

        EconomyService service = new EconomyService();
        service.replaceState(Map.of(ALICE, 42.5), Map.of(), Map.of("northmarch", economy));
        return service;
    }
}
