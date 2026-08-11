package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.ArrestReward;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArrestRewardServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID POSTER = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID TOPPER = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private EconomyService economyService;
    private ArrestRewardService rewardService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        economyService = new EconomyService(100.0);
        rewardService = new ArrestRewardService(kingdomService, justice, economyService);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.joinKingdom(POSTER, "northmarch");
        kingdomService.joinKingdom(TOPPER, "northmarch");
        kingdomService.joinKingdom(OUTSIDER, "southreach");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(CONSTABLE, NobleRank.KNIGHT, TitleStyle.MASCULINE);

        policeService.setCell("northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);

        economyService.creditWalletDirect(POSTER, 50.0);
        economyService.creditWalletDirect(TOPPER, 20.0);
    }

    @Test
    void postDebitsPosterWalletIntoEscrowOnActiveWarrant() {
        Warrant warrant = openAndApprove();

        PoliceResult result = rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 25.0);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(25.0, economyService.getWalletBalance(POSTER), 1e-9);
        ArrestReward reward = warrant.arrestReward().orElseThrow();
        assertEquals(POSTER, reward.posterId());
        assertEquals(25.0, reward.amount(), 1e-9);
    }

    @Test
    void topUpIncreasesSamePurseFromOriginalPoster() {
        Warrant warrant = openAndApprove();
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 10.0);

        PoliceResult result = rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 5.0);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(15.0, warrant.arrestReward().orElseThrow().amount(), 1e-9);
        assertEquals(POSTER, warrant.arrestReward().orElseThrow().posterId());
        assertEquals(35.0, economyService.getWalletBalance(POSTER), 1e-9);
    }

    @Test
    void otherMemberCannotTopUpExistingPurse() {
        Warrant warrant = openAndApprove();
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 10.0);

        PoliceResult result = rewardService.postOrTopUp("northmarch", TOPPER, SUSPECT, 5.0);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertEquals(10.0, warrant.arrestReward().orElseThrow().amount(), 1e-9);
        assertEquals(20.0, economyService.getWalletBalance(TOPPER), 1e-9);
        assertEquals(40.0, economyService.getWalletBalance(POSTER), 1e-9);
    }

    @Test
    void payOnConstableArrestCreditsConstableAndClearsPurse() {
        Warrant warrant = openAndApprove();
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 25.0);

        rewardService.payOnConstableArrest(warrant, CONSTABLE);

        assertEquals(25.0, economyService.getWalletBalance(CONSTABLE), 1e-9);
        assertTrue(warrant.arrestReward().isEmpty());
        assertEquals(25.0, economyService.getWalletBalance(POSTER), 1e-9);
    }

    @Test
    void refundOnGolemArrestReturnsPurseToPoster() {
        Warrant warrant = openAndApprove();
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 25.0);

        rewardService.refundPoster(warrant);

        assertEquals(50.0, economyService.getWalletBalance(POSTER), 1e-9);
        assertTrue(warrant.arrestReward().isEmpty());
        assertEquals(0.0, economyService.getWalletBalance(CONSTABLE), 1e-9);
    }

    @Test
    void refundOnCancelReturnsPurseToPoster() {
        Warrant warrant = openAndApprove();
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 25.0);

        PoliceResult cancelled = justice.cancelActiveWarrant("northmarch", KING, warrant.id());
        assertInstanceOf(PoliceResult.Success.class, cancelled);
        rewardService.refundPoster(warrant);

        assertEquals(WarrantStatus.CANCELLED, warrant.status());
        assertEquals(50.0, economyService.getWalletBalance(POSTER), 1e-9);
        assertTrue(warrant.arrestReward().isEmpty());
    }

    @Test
    void nonMemberCannotPost() {
        openAndApprove();

        PoliceResult result = rewardService.postOrTopUp("northmarch", OUTSIDER, SUSPECT, 10.0);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertEquals(50.0, economyService.getWalletBalance(POSTER), 1e-9);
    }

    @Test
    void insufficientFundsRefusesPost() {
        openAndApprove();

        PoliceResult result = rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 100.0);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertEquals(50.0, economyService.getWalletBalance(POSTER), 1e-9);
    }

    private Warrant openAndApprove() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
        return justice.findActiveForSuspect("northmarch", SUSPECT).orElseThrow();
    }
}
