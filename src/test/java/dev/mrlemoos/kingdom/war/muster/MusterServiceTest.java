package dev.mrlemoos.kingdom.war.muster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Muster call to the levy: opening a muster makes both belligerents' current members eligible;
 * answering opens/refreshes levy morale at Steadfast; refusing drops straight to Shaken; an eligible
 * member left unanswered when the muster deadline sweeps is marked an ignored muster — Shaken on the
 * military track and a political Act breach (Faithful towards Doubtful) via the optional
 * {@link LoyaltyService} hook.
 */
class MusterServiceTest {

    private static final UUID ATTACKER_MEMBER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACKER_ROSTERED = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEFENDER_MEMBER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private KingdomService kingdomService;
    private WarService warService;
    private AtomicLong musterClock;
    private MusterService musterService;
    private ActiveWar war;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(ATTACKER_MEMBER, "northmarch");
        kingdomService.joinKingdom(ATTACKER_ROSTERED, "northmarch");
        kingdomService.joinKingdom(DEFENDER_MEMBER, "southreach");

        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 1);
        warService.enactWarBill("northmarch", payload);
        war = warService.activeWarFor("northmarch").orElseThrow();

        musterClock = new AtomicLong(1_700_000_000_000L);
        musterService = new MusterService(warService, kingdomService, musterClock::get);
    }

    @Test
    void openingMusterMakesBothBelligerentsMembersEligible() {
        WarResult result = musterService.openMuster(war.id());

        assertInstanceOf(WarResult.Success.class, result);
        assertTrue(musterService.isEligible(war.id(), ATTACKER_MEMBER));
        assertTrue(musterService.isEligible(war.id(), DEFENDER_MEMBER));
    }

    @Test
    void openMusterFailsWhenDisabled() {
        musterService.setConfig(MusterConfig.off());

        WarResult result = musterService.openMuster(war.id());

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void openMusterFailsForUnknownWar() {
        WarResult result = musterService.openMuster("no-such-war");

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void answeringMusterOpensLevyMoraleAtSteadfast() {
        musterService.openMuster(war.id());

        WarResult result = musterService.answer(war.id(), ATTACKER_MEMBER);

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, musterService.levyMoraleTier(ATTACKER_MEMBER).orElseThrow());
        assertEquals(MusterAnswer.ANSWERED, musterService.answerOf(war.id(), ATTACKER_MEMBER).orElseThrow());
    }

    @Test
    void refusingMusterDropsLevyMoraleToShaken() {
        musterService.openMuster(war.id());

        WarResult result = musterService.refuse(war.id(), ATTACKER_MEMBER);

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(MoraleTier.SHAKEN, musterService.levyMoraleTier(ATTACKER_MEMBER).orElseThrow());
        assertEquals(MusterAnswer.REFUSED, musterService.answerOf(war.id(), ATTACKER_MEMBER).orElseThrow());
    }

    @Test
    void cannotAnswerBeforeMusterIsOpened() {
        WarResult result = musterService.answer(war.id(), ATTACKER_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void cannotAnswerAfterMusterDeadlinePasses() {
        musterService.openMuster(war.id());
        musterClock.set(war.musterDeadlineAtMs());

        WarResult result = musterService.answer(war.id(), ATTACKER_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().toLowerCase().contains("deadline"));
    }

    @Test
    void cannotRefuseAfterMusterDeadlinePasses() {
        musterService.openMuster(war.id());
        musterClock.set(war.musterDeadlineAtMs());

        WarResult result = musterService.refuse(war.id(), ATTACKER_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void sweepMarksUnansweredEligibleMembersAsIgnoredWithShakenMoraleAndDoubtfulLoyalty() {
        InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
        LoyaltyService loyaltyService = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        musterService.setLoyaltyService(loyaltyService);
        musterService.openMuster(war.id());

        int ignored = musterService.sweep(war.musterDeadlineAtMs());

        assertEquals(3, ignored);
        assertEquals(MusterAnswer.IGNORED, musterService.answerOf(war.id(), ATTACKER_MEMBER).orElseThrow());
        assertEquals(MoraleTier.SHAKEN, musterService.levyMoraleTier(ATTACKER_MEMBER).orElseThrow());
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(ATTACKER_MEMBER));
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(DEFENDER_MEMBER));
    }

    @Test
    void sweepBeforeDeadlineDoesNothing() {
        musterService.openMuster(war.id());

        int ignored = musterService.sweep(war.musterDeadlineAtMs() - 1);

        assertEquals(0, ignored);
        assertTrue(musterService.answerOf(war.id(), ATTACKER_MEMBER).isEmpty());
    }

    @Test
    void sweepDoesNotIgnoreMembersWhoAlreadyAnsweredOrRefused() {
        musterService.openMuster(war.id());
        musterService.answer(war.id(), ATTACKER_MEMBER);
        musterService.refuse(war.id(), DEFENDER_MEMBER);

        int ignored = musterService.sweep(war.musterDeadlineAtMs());

        assertEquals(1, ignored);
        assertEquals(MusterAnswer.ANSWERED, musterService.answerOf(war.id(), ATTACKER_MEMBER).orElseThrow());
        assertEquals(MusterAnswer.REFUSED, musterService.answerOf(war.id(), DEFENDER_MEMBER).orElseThrow());
    }

    @Test
    void sweepIsIdempotentOnceEveryoneHasAnAnswer() {
        InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
        LoyaltyService loyaltyService = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        musterService.setLoyaltyService(loyaltyService);
        musterService.openMuster(war.id());
        musterService.sweep(war.musterDeadlineAtMs());

        int secondSweepIgnored = musterService.sweep(war.musterDeadlineAtMs() + 10_000L);

        assertEquals(0, secondSweepIgnored);
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(ATTACKER_MEMBER));
    }

    @Test
    void standingRosterMemberAlreadyOnDutyIsAutoCountedAsAnsweredOnOpen() {
        InMemoryStandingRosterStore rosterStore = new InMemoryStandingRosterStore();
        StandingRosterService rosterService = new StandingRosterService(
                kingdomService, rosterStore, StandingRosterConfig.defaults());
        rosterService.appoint("northmarch", NobleRank.KING, ATTACKER_ROSTERED);
        rosterService.mobiliseOnWarEnactment("northmarch");
        musterService.setStandingRosterService(rosterService);

        musterService.openMuster(war.id());

        assertEquals(MusterAnswer.ANSWERED, musterService.answerOf(war.id(), ATTACKER_ROSTERED).orElseThrow());
    }

    @Test
    void standingRosterMemberOnDutyIsNotSweptAsIgnored() {
        InMemoryStandingRosterStore rosterStore = new InMemoryStandingRosterStore();
        StandingRosterService rosterService = new StandingRosterService(
                kingdomService, rosterStore, StandingRosterConfig.defaults());
        rosterService.appoint("northmarch", NobleRank.KING, ATTACKER_ROSTERED);
        rosterService.mobiliseOnWarEnactment("northmarch");
        musterService.setStandingRosterService(rosterService);
        musterService.openMuster(war.id());

        musterService.sweep(war.musterDeadlineAtMs());

        assertFalse(musterService.answerOf(war.id(), ATTACKER_ROSTERED).orElseThrow() == MusterAnswer.IGNORED);
    }

    @Test
    void sweepWithoutLoyaltyServiceStillMarksIgnoredMorale() {
        musterService.openMuster(war.id());

        int ignored = musterService.sweep(war.musterDeadlineAtMs());

        assertEquals(3, ignored);
        assertEquals(MoraleTier.SHAKEN, musterService.levyMoraleTier(DEFENDER_MEMBER).orElseThrow());
    }
}
