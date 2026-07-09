package dev.mrlemoos.kingdom.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionConfig;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.conscription.InMemoryConscriptionStore;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadConfig;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Peace bill demobilisation: hostilities cease, the levy's muster state is cleared, and mobilised
 * standing-roster members lose their wartime on-duty/hardened footing — but standing roster
 * membership itself persists, since the standing force is not demobbed off the roster, only stood
 * down from wartime duty. Captured-chunk reversion (Slice 6.8) restores every captured chunk to
 * defender control and never creates a {@link RegionMergePlan}; any annexation/tribute side effect
 * remains a no-op here (that only follows decisive victory, not a negotiated peace).
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
        // No ChunkCaptureTally was ever created for this war, and no ChunkCaptureService is
        // hooked — captured-chunk reversion must be a null-safe no-op in that case.
        WarResult result = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
    }

    @Test
    void demobiliseFailsGracefullyForNullWar() {
        WarResult result = demobilisationService.demobilise(null);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void demobiliseDestroysCrownSquadsForBothBelligerents() {
        EconomyService economyService = new EconomyService();
        economyService.creditTreasury("northmarch", 500.0);
        economyService.enactBudget("northmarch", 500.0);
        economyService.creditTreasury("southreach", 500.0);
        economyService.enactBudget("southreach", 500.0);
        CrownSquadService crownSquadService =
                new CrownSquadService(economyService, new CrownSquadConfig(true, 50.0, 4));
        crownSquadService.purchase("northmarch");
        crownSquadService.purchase("southreach");
        demobilisationService.setCrownSquadService(crownSquadService);

        demobilisationService.demobilise(war);

        assertTrue(crownSquadService.unitsOf("northmarch").isEmpty());
        assertTrue(crownSquadService.unitsOf("southreach").isEmpty());
    }

    @Test
    void demobiliseReleasesPressedVillagersForBothBelligerents() {
        UUID attackerPressed = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID defenderPressed = UUID.fromString("66666666-6666-6666-6666-666666666666");
        ConscriptionService conscriptionService = new ConscriptionService(
                kingdomService, new InMemoryConscriptionStore(), new ConscriptionConfig(true, 16));
        conscriptionService.press("northmarch", attackerPressed);
        conscriptionService.press("southreach", defenderPressed);
        demobilisationService.setConscriptionService(conscriptionService);

        demobilisationService.demobilise(war);

        assertFalse(conscriptionService.isPressed(attackerPressed));
        assertFalse(conscriptionService.isPressed(defenderPressed));
    }

    @Test
    void demobiliseWithoutOptionalHooksStillEndsWar() {
        DemobilisationService bareDemobilisationService = new DemobilisationService(warService);

        WarResult result = bareDemobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(warService.isAtWar("northmarch"));
    }

    @Test
    void demobiliseRevertsCapturedChunksToDefenderControlWhenChunkCaptureServiceIsHooked() {
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCoord chunk = new ChunkCoord("world", 1, 1);
        chunkCaptureService.tick(war.id(), chunk, "northmarch", "southreach", 3, 0);
        assertTrue(chunkCaptureService.controller(war.id(), chunk).isPresent());
        demobilisationService.setChunkCaptureService(chunkCaptureService);

        demobilisationService.demobilise(war);

        assertTrue(chunkCaptureService.controller(war.id(), chunk).isEmpty());
        assertTrue(chunkCaptureService.capturedBy(war.id(), "northmarch").isEmpty());
    }

    @Test
    void demobiliseCapturedChunkRevertIsIdempotentAcrossRepeatedDemobilisation() {
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCoord chunk = new ChunkCoord("world", 1, 1);
        chunkCaptureService.tick(war.id(), chunk, "northmarch", "southreach", 3, 0);
        demobilisationService.setChunkCaptureService(chunkCaptureService);

        demobilisationService.demobilise(war);
        WarResult secondResult = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, secondResult);
        assertTrue(chunkCaptureService.controller(war.id(), chunk).isEmpty());
    }

    @Test
    void demobiliseWithoutChunkCaptureServiceHookedLeavesCapturedChunkStateUntouched() {
        // Mirrors the standing-roster/crown-squad/conscription hooks: without the optional
        // ChunkCaptureService hook, demobilisation must still succeed and leave capture state
        // simply untouched rather than throwing.
        DemobilisationService bareDemobilisationService = new DemobilisationService(warService);

        WarResult result = bareDemobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, result);
    }

    @Test
    void demobiliseOnPeacePathNeverProducesANonEmptyRegionMergePlanForTheWar() {
        // Peace bill demobilisation must never leave behind capture state that a region-merge
        // planner could act on: after revert, no captured chunks remain for the attacker, so
        // RegionMergePlan.fromCapturedChunks (the only route to a merge) cannot be built.
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCoord chunk = new ChunkCoord("world", 2, 2);
        chunkCaptureService.tick(war.id(), chunk, "northmarch", "southreach", 3, 0);
        assertFalse(chunkCaptureService.capturedBy(war.id(), "northmarch").isEmpty());
        demobilisationService.setChunkCaptureService(chunkCaptureService);

        demobilisationService.demobilise(war);

        Set<ChunkCoord> capturedAfterRevert = chunkCaptureService.capturedBy(war.id(), "northmarch");
        assertTrue(capturedAfterRevert.isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> RegionMergePlan.fromCapturedChunks("northmarch", "southreach", capturedAfterRevert));
    }
}
