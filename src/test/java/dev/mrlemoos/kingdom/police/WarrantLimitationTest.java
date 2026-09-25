package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An active warrant nobody serves lapses after the statute of limitations. */
class WarrantLimitationTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID POSTER = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final int LIMIT_DAYS = 7;

    private final AtomicLong realmDay = new AtomicLong(10);
    private MechanicalJusticeService justice;
    private EconomyService economyService;
    private ArrestRewardService rewardService;

    @BeforeEach
    void setUp() {
        KingdomService kingdomService = new KingdomService();
        PoliceService policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        justice.setRealmDaySupplier(realmDay::get);
        economyService = new EconomyService(100.0);
        rewardService = new ArrestRewardService(kingdomService, justice, economyService);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(POSTER, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        policeService.setCell("northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        economyService.creditWalletDirect(POSTER, 50.0);
    }

    @Test
    void anApprovedWarrantLapsesSevenRealmDaysAfterApproval() {
        justice.openFromActBreach(new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN), SUSPECT);
        Warrant warrant = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        realmDay.set(12); // the application waits two days for the Crown; that time does not count
        justice.approveWarrant("northmarch", KING, warrant.id());

        assertTrue(rewardService.lapseDueWarrants(18, LIMIT_DAYS).isEmpty());
        assertTrue(justice.hasActiveWarrant("northmarch", SUSPECT));

        List<Warrant> lapsed = rewardService.lapseDueWarrants(19, LIMIT_DAYS);

        assertEquals(List.of(warrant), lapsed);
        assertEquals(WarrantStatus.LAPSED, warrant.status());
        assertFalse(justice.hasActiveWarrant("northmarch", SUSPECT));
    }

    @Test
    void aWarrantAwaitingTheCrownNeverLapses() {
        justice.openFromActBreach(new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN), SUSPECT);

        assertTrue(rewardService.lapseDueWarrants(1_000, LIMIT_DAYS).isEmpty());
        assertTrue(justice.findPendingForSuspect("northmarch", SUSPECT).isPresent());
    }

    @Test
    void aFlagrantWarrantLapsesFromTheDayItWasIssued() {
        justice.openFlagrantTreason("northmarch", SUSPECT);

        assertEquals(1, rewardService.lapseDueWarrants(17, LIMIT_DAYS).size());
    }

    @Test
    void lapsingRefundsTheArrestRewardToItsPoster() {
        justice.openFlagrantTreason("northmarch", SUSPECT);
        rewardService.postOrTopUp("northmarch", POSTER, SUSPECT, 30.0);
        assertEquals(20.0, economyService.getWalletBalance(POSTER), 1e-9);

        Warrant lapsed = rewardService.lapseDueWarrants(17, LIMIT_DAYS).get(0);

        assertEquals(50.0, economyService.getWalletBalance(POSTER), 1e-9);
        assertTrue(lapsed.arrestReward().isEmpty());
    }

    @Test
    void anActiveWarrantFromBeforeTheStatuteStartsItsClockAtTheFirstSweep() {
        Warrant legacy = new Warrant(
                "northmarch-warrant-1", "northmarch", SUSPECT, "northmarch-build",
                ConductKind.BUILD_BAN, WarrantStatus.ACTIVE, 1L);
        justice.replaceWarrants(List.of(legacy));

        assertTrue(rewardService.lapseDueWarrants(40, LIMIT_DAYS).isEmpty());
        assertEquals(40L, legacy.activeSinceDay().orElseThrow());
        assertEquals(1, rewardService.lapseDueWarrants(47, LIMIT_DAYS).size());
    }
}
