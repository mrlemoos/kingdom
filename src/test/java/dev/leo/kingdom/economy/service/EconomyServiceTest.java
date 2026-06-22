package dev.leo.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.model.CreditResult;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.IncomeLocation;
import dev.leo.kingdom.economy.model.KingdomEconomy;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.model.NobleRank;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyServiceTest {

    private static final double WILDERNESS_MULTIPLIER = 0.5;

    private EconomyService service;
    private final UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID premier = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final FiscalRates rates = new FiscalRates(0.10, 0.05, 0.03, 0.08, Map.of());

    @BeforeEach
    void setUp() {
        service = new EconomyService();
    }

    @Test
    void walletCreditNetEqualsGrossMinusTax() {
        CreditResult result = service.creditWallet(
                alice,
                100.0,
                IncomeLocation.OWN_KINGDOM,
                null,
                "northmarch",
                "northmarch",
                rates,
                WILDERNESS_MULTIPLIER);

        assertEquals(90.0, result.net());
        assertEquals(90.0, service.getWalletBalance(alice));
        assertEquals(10.0, service.getTreasuryBalance("northmarch"));
        assertEquals(10.0, service.getTotalTaxRevenue("northmarch"), 1e-9);
    }

    @Test
    void transferFeesDoNotAccumulateTaxRevenue() {
        fundWallet(alice, 100.0);

        service.transferCorona(alice, bob, 100.0, "northmarch", "northmarch", rates);

        assertEquals(3.0, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.0, service.getTotalTaxRevenue("northmarch"), 1e-9);
    }

    @Test
    void recordGdpCreditAccumulatesGdpRevenueAndTreasury() {
        service.recordGdpCredit("northmarch", 42.0);

        assertEquals(42.0, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(42.0, service.getTotalGdpRevenue("northmarch"), 1e-9);
    }

    @Test
    void setLastDailyGdpStoresPerKingdomRate() {
        service.setLastDailyGdp("northmarch", 17.5);

        assertEquals(17.5, service.getLastDailyGdp("northmarch"), 1e-9);
        assertEquals(0.0, service.getLastDailyGdp("riviera"), 1e-9);
    }

    @Test
    void foreignIncomeRoutesHigherTaxToTreasury() {
        service.creditWallet(
                alice,
                100.0,
                IncomeLocation.FOREIGN_KINGDOM,
                null,
                "northmarch",
                "riviera",
                rates,
                WILDERNESS_MULTIPLIER);

        assertEquals(85.0, service.getWalletBalance(alice), 1e-9);
        assertEquals(15.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void wildernessIncomePaysReducedAmountWithoutTax() {
        service.creditWallet(
                alice,
                100.0,
                IncomeLocation.WILDERNESS,
                null,
                "northmarch",
                null,
                rates,
                WILDERNESS_MULTIPLIER);

        assertEquals(50.0, service.getWalletBalance(alice));
        assertEquals(0.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void domesticTransferChargesLowerFeeToSenderTreasury() {
        fundWallet(alice, 100.0);

        TransferResult result = service.transferCorona(alice, bob, 100.0, "northmarch", "northmarch", rates);

        assertEquals(3.0, result.fee());
        assertEquals(97.0, result.amountReceived());
        assertEquals(0.0, service.getWalletBalance(alice));
        assertEquals(97.0, service.getWalletBalance(bob));
        assertEquals(3.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void crossKingdomTransferChargesHigherFee() {
        fundWallet(alice, 100.0);

        TransferResult result = service.transferCorona(alice, bob, 100.0, "northmarch", "riviera", rates);

        assertEquals(8.0, result.fee());
        assertEquals(92.0, result.amountReceived());
        assertEquals(8.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void premierSubmitsProposalForRoyalApproval() {
        FiscalRates proposed = new FiscalRates(0.12, 0.06, 0.04, 0.09, Map.of());

        EconomyResult submitted = service.submitProposal("northmarch", NobleRank.PREMIER, premier, proposed);

        assertInstanceOf(EconomyResult.Success.class, submitted);
        assertTrue(service.kingdomEconomies().get("northmarch").pendingProposal().isPresent());
    }

    @Test
    void onlyPremierMaySubmitFiscalProposal() {
        EconomyResult submitted = service.submitProposal("northmarch", NobleRank.DUKE, premier, rates);

        assertInstanceOf(EconomyResult.Failure.class, submitted);
    }

    @Test
    void kingApprovesPendingFiscalProposal() {
        FiscalRates proposed = new FiscalRates(0.12, 0.06, 0.04, 0.09, Map.of());
        service.submitProposal("northmarch", NobleRank.PREMIER, premier, proposed);

        EconomyResult approved = service.approveProposal("northmarch", NobleRank.KING);

        assertInstanceOf(EconomyResult.Success.class, approved);
        assertEquals(proposed, service.kingdomEconomies().get("northmarch").activeRates());
        assertTrue(service.kingdomEconomies().get("northmarch").pendingProposal().isEmpty());
    }

    @Test
    void queenMayRejectPendingFiscalProposal() {
        service.submitProposal("northmarch", NobleRank.PREMIER, premier, rates);

        EconomyResult rejected = service.rejectProposal("northmarch", NobleRank.QUEEN);

        assertInstanceOf(EconomyResult.Success.class, rejected);
        assertTrue(service.kingdomEconomies().get("northmarch").pendingProposal().isEmpty());
    }

    @Test
    void approveBudgetDoesNotRequireTreasuryFunds() {
        EconomyResult result = service.approveBudget("northmarch", 100.0);

        assertInstanceOf(EconomyResult.Success.class, result);
        assertEquals(0.0, service.getTreasuryBalance("northmarch"));
        assertEquals(100.0, service.kingdomEconomies().get("northmarch").budget().approvedAmount());
    }

    @Test
    void newKingdomStartsWithConfiguredTreasury() {
        EconomyService seeded = new EconomyService(100.0);

        assertEquals(100.0, seeded.getTreasuryBalance("northmarch"));
    }

    @Test
    void mintPlacementFailsWhenTreasuryEmptyDespiteApprovedBudget() {
        service.approveBudget("northmarch", 200.0);

        EconomyResult result = service.placeMint("northmarch", new MintLocation("world", 10, 64, 10), 50.0, 3);

        assertInstanceOf(EconomyResult.Failure.class, result);
        assertTrue(((EconomyResult.Failure) result).message().contains("treasury"));
    }

    @Test
    void adminTreasuryCreditIncreasesBalance() {
        EconomyResult result = service.creditTreasuryAdmin("northmarch", 75.0);

        assertInstanceOf(EconomyResult.Success.class, result);
        assertEquals(75.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void budgetSpendIsLimitedToApprovedAmount() {
        service.creditTreasury("northmarch", 500.0);
        service.approveBudget("northmarch", 100.0);

        EconomyResult firstSpend = service.spendFromBudget("northmarch", 60.0);
        EconomyResult secondSpend = service.spendFromBudget("northmarch", 50.0);

        assertInstanceOf(EconomyResult.Success.class, firstSpend);
        assertInstanceOf(EconomyResult.Failure.class, secondSpend);
        assertEquals(440.0, service.getTreasuryBalance("northmarch"));
        assertEquals(60.0, service.kingdomEconomies().get("northmarch").budget().spentAmount());
    }

    @Test
    void mintPlacementRespectsCapAndBudgetCost() {
        service.creditTreasury("northmarch", 500.0);
        service.approveBudget("northmarch", 200.0);
        MintLocation firstMint = new MintLocation("world", 10, 64, 10);
        MintLocation secondMint = new MintLocation("world", 20, 64, 20);

        EconomyResult firstPlacement = service.placeMint("northmarch", firstMint, 50.0, 1);
        EconomyResult secondPlacement = service.placeMint("northmarch", secondMint, 50.0, 1);

        assertInstanceOf(EconomyResult.Success.class, firstPlacement);
        assertInstanceOf(EconomyResult.Failure.class, secondPlacement);
        assertEquals(1, service.kingdomEconomies().get("northmarch").mintCount());
        assertEquals(450.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void depositAndWithdrawWholeNuggetsRoundDownLedger() {
        service.depositFromNuggets(alice, 3);
        service.creditWallet(
                alice,
                1.5,
                IncomeLocation.OWN_KINGDOM,
                null,
                "northmarch",
                "northmarch",
                rates,
                WILDERNESS_MULTIPLIER);

        int withdrawn = service.withdrawWholeNuggets(alice);

        assertEquals(4, withdrawn);
        assertEquals(0.35, service.getWalletBalance(alice), 1e-9);
    }

    @Test
    void replaceStateRestoresPersistedBalances() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setTreasuryBalance(250.0);
        service.replaceState(Map.of(alice, 42.0), Map.of("northmarch", economy));

        assertEquals(42.0, service.getWalletBalance(alice));
        assertEquals(250.0, service.getTreasuryBalance("northmarch"));
    }

    @Test
    void transferFailsWhenSenderLacksCorona() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.transferCorona(alice, bob, 10.0, "northmarch", "northmarch", rates));
    }

    private void fundWallet(UUID playerId, double amount) {
        service.creditWallet(
                playerId,
                amount,
                IncomeLocation.WILDERNESS,
                null,
                null,
                null,
                rates,
                1.0);
    }
}
