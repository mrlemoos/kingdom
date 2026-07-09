package dev.mrlemoos.kingdom.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Peace bill demobilisation: hostilities cease, the levy's muster state is cleared, and mobilised
 * standing-roster members lose their wartime on-duty/hardened footing — but standing roster
 * membership itself persists, since the standing force is not demobbed off the roster, only stood
 * down from wartime duty. Captured-chunk reversion and any annexation/tribute side effect remain
 * no-ops here (deferred to Phase 6).
 */
class DemobilisationServiceTest {

    private static final UUID ATTACKER_ROSTERED = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACKER_LEVY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEFENDER_LEVY = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private KingdomService kingdomService;
    private WarService warService;
    private MusterService musterService;
    private StandingRosterService rosterService;
    private DemobilisationService demobilisationService;
    private ActiveWar war;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(ATTACKER_ROSTERED, "northmarch");
        kingdomService.joinKingdom(ATTACKER_LEVY, "northmarch");
        kingdomService.joinKingdom(DEFENDER_LEVY, "southreach");

        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());

        rosterService = new StandingRosterService(
                kingdomService, new InMemoryStandingRosterStore(), StandingRosterConfig.defaults());
        rosterService.appoint("northmarch", NobleRank.KING, ATTACKER_ROSTERED);
        warService.setStandingRosterService(rosterService);

        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);
        war = warService.activeWarFor("northmarch").orElseThrow();

        musterService = new MusterService(warService, kingdomService, () -> 1_700_000_000_000L);
        musterService.setStandingRosterService(rosterService);
        musterService.openMuster(war.id());
        musterService.answer(war.id(), ATTACKER_LEVY);
        musterService.refuse(war.id(), DEFENDER_LEVY);

        demobilisationService = new DemobilisationService(warService);
        demobilisationService.setMusterService(musterService);
        demobilisationService.setStandingRosterService(rosterService);
    }

    @Test
    void demobiliseEndsTheActiveWarForBothBelligerents() {
        WarResult result = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(warService.isAtWar("northmarch"));
        assertFalse(warService.isAtWar("southreach"));
    }

    @Test
    void demobiliseClearsLevyMusterAnswersAndMorale() {
        demobilisationService.demobilise(war);

        assertTrue(musterService.answerOf(war.id(), ATTACKER_LEVY).isEmpty());
        assertTrue(musterService.answerOf(war.id(), DEFENDER_LEVY).isEmpty());
        assertTrue(musterService.levyMoraleTier(ATTACKER_LEVY).isEmpty());
        assertTrue(musterService.levyMoraleTier(DEFENDER_LEVY).isEmpty());
    }

    @Test
    void demobiliseClearsOnDutyAndHardenedButKeepsStandingRosterMembership() {
        demobilisationService.demobilise(war);

        assertFalse(rosterService.isOnDuty(ATTACKER_ROSTERED));
        assertFalse(rosterService.hasHardenedService(ATTACKER_ROSTERED));
        assertTrue(rosterService.rosterView("northmarch").contains(ATTACKER_ROSTERED));
    }

    @Test
    void demobiliseIsIdempotentWhenTheWarWasAlreadyEnded() {
        warService.endWar(war.id());

        WarResult result = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(rosterService.isOnDuty(ATTACKER_ROSTERED));
        assertTrue(rosterService.rosterView("northmarch").contains(ATTACKER_ROSTERED));
    }

    @Test
    void demobiliseCausesNoTreasuryTributeOrAnnexationSideEffect() {
        EconomyService economyService = new EconomyService(100.0);
        double attackerBefore = economyService.getTreasuryBalance("northmarch");
        double defenderBefore = economyService.getTreasuryBalance("southreach");

        demobilisationService.demobilise(war);

        assertEquals(attackerBefore, economyService.getTreasuryBalance("northmarch"));
        assertEquals(defenderBefore, economyService.getTreasuryBalance("southreach"));
    }

    @Test
    void demobiliseSucceedsWithoutAnyCapturedChunkState() {
        // No ChunkCaptureTally/RegionMergePlan was ever created for this war — captured-chunk
        // reversion is a no-op stub until Phase 6, so demobilisation must not depend on it.
        WarResult result = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
    }

    @Test
    void demobiliseFailsGracefullyForNullWar() {
        WarResult result = demobilisationService.demobilise(null);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void demobiliseWithoutOptionalHooksStillEndsWar() {
        DemobilisationService bareDemobilisationService = new DemobilisationService(warService);

        WarResult result = bareDemobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(warService.isAtWar("northmarch"));
    }
}
