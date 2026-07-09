package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PoliceCaseStatus;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoliceTrialPipelineTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private LoyaltyService loyaltyService;
    private EconomyService economyService;
    private PoliceTrialService trialService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.enabled());
        economyService = new EconomyService(100.0);
        trialService = new PoliceTrialService(
                kingdomService, policeService, justice, loyaltyService, economyService);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.joinKingdom(JUDGE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(CONSTABLE, NobleRank.KNIGHT, TitleStyle.MASCULINE);
        kingdomService.assignTitle(JUDGE, NobleRank.KNIGHT, TitleStyle.MASCULINE);

        policeService.setCell("northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);

        economyService.creditWalletDirect(SUSPECT, 50.0);
    }

    @Test
    void arrestOnActiveWarrantOpensPendingTrial() {
        openAndApproveWarrant();

        PoliceResult result = trialService.arrest("northmarch", CONSTABLE, SUSPECT);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(PoliceCaseStatus.PENDING_TRIAL, trialService.findOpenCase("northmarch", SUSPECT).orElseThrow().status());
        assertFalse(justice.hasActiveWarrant("northmarch", SUSPECT));
    }

    @Test
    void warningSentenceRecordsOffenceWithoutFurtherLoyaltyDrop() {
        loyaltyService.recordActBreach(SUSPECT);
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(SUSPECT));
        openApproveAndArrest();

        PoliceResult result = trialService.sentence(
                "northmarch", JUDGE, SUSPECT, SentenceType.WARNING, 0, 0);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(SUSPECT));
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
        assertEquals(SentenceType.WARNING, trialService.lastClosedSentence("northmarch", SUSPECT).orElseThrow());
    }

    @Test
    void fineSentenceDebitsCoronaToTreasury() {
        openApproveAndArrest();
        double treasuryBefore = economyService.getTreasuryBalance("northmarch");
        double walletBefore = economyService.getWalletBalance(SUSPECT);

        PoliceResult result = trialService.sentence(
                "northmarch", JUDGE, SUSPECT, SentenceType.FINE, 10.0, 0);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(walletBefore - 10.0, economyService.getWalletBalance(SUSPECT), 1e-9);
        assertEquals(treasuryBefore + 10.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void prisonSentenceBlocksKingdomTeleport() {
        openApproveAndArrest();

        PoliceResult result = trialService.sentence(
                "northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(trialService.isKingdomTeleportBlocked(SUSPECT));
        assertEquals(1, trialService.assignedCellSlot("northmarch", SUSPECT).orElseThrow());
    }

    @Test
    void acquittalClosesCaseWithoutFineOrPrison() {
        openApproveAndArrest();
        double walletBefore = economyService.getWalletBalance(SUSPECT);

        PoliceResult result = trialService.sentence(
                "northmarch", JUDGE, SUSPECT, SentenceType.ACQUITTAL, 0, 0);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(walletBefore, economyService.getWalletBalance(SUSPECT), 1e-9);
        assertFalse(trialService.isKingdomTeleportBlocked(SUSPECT));
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
    }

    private void openAndApproveWarrant() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
    }

    private void openApproveAndArrest() {
        openAndApproveWarrant();
        trialService.arrest("northmarch", CONSTABLE, SUSPECT);
    }
}
