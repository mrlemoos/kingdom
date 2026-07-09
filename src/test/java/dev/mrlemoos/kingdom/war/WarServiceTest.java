package dev.mrlemoos.kingdom.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarServiceTest {

    private KingdomService kingdomService;
    private WarService warService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.createKingdom("eastvale", "Eastvale");

        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());
    }

    @Test
    void cannotValidateWarBillWhenDisabled() {
        warService.setConfig(WarConfig.off());

        WarResult result = warService.validateWarBill("northmarch", "southreach");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().contains("disabled"));
    }

    @Test
    void unknownTargetKingdomRejected() {
        WarResult result = warService.validateWarBill("northmarch", "no_such_kingdom");

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void cannotTargetSelf() {
        WarResult result = warService.validateWarBill("northmarch", "northmarch");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().toLowerCase().contains("itself"));
    }

    @Test
    void coalitionMultiTargetRejectedAsUnknownKingdom() {
        WarResult result = warService.validateWarBill("northmarch", "southreach,eastvale");

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void attackerAlreadyAtWarRejectsSecondWarBill() {
        BillPayload.War firstWar = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        assertInstanceOf(WarResult.Success.class, warService.enactWarBill("northmarch", firstWar));

        WarResult second = warService.validateWarBill("northmarch", "eastvale");

        assertInstanceOf(WarResult.Failure.class, second);
        assertTrue(((WarResult.Failure) second).message().contains("already at war"));
    }

    @Test
    void enactWarBillCreatesActiveWarWithClockAndMusterDeadline() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.CAPITAL_FALL, WarOutcome.WAR_TRIBUTE, 2);

        WarResult result = warService.enactWarBill("northmarch", payload);

        assertInstanceOf(WarResult.Success.class, result);
        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();
        assertEquals("northmarch", war.attackerKingdomId());
        assertEquals("southreach", war.defenderKingdomId());
        assertEquals(WarAim.CAPITAL_FALL, war.aim());
        assertEquals(WarOutcome.WAR_TRIBUTE, war.outcome());
        assertEquals(1_700_000_000_000L, war.startedAtMs());
        assertEquals(1_700_000_000_000L + 2 * WarConfig.DEFAULT_MS_PER_MC_DAY, war.musterDeadlineAtMs());
    }

    @Test
    void bothBelligerentsAreAtWarAfterEnactment() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 5);
        warService.enactWarBill("northmarch", payload);

        assertTrue(warService.isAtWar("northmarch"));
        assertTrue(warService.isAtWar("southreach"));
        assertFalse(warService.isAtWar("eastvale"));
    }

    @Test
    void battlefieldTreasonPossibleOnlyWhileAtWar() {
        assertFalse(warService.battlefieldTreasonPossible("northmarch"));
        assertFalse(warService.battlefieldTreasonPossible("southreach"));

        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.CAPITAL_FALL, WarOutcome.WAR_TRIBUTE, 2);
        warService.enactWarBill("northmarch", payload);

        assertTrue(warService.battlefieldTreasonPossible("northmarch"));
        assertTrue(warService.battlefieldTreasonPossible("southreach"));

        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();
        warService.endWar(war.id());

        assertFalse(warService.battlefieldTreasonPossible("northmarch"));
        assertFalse(warService.battlefieldTreasonPossible("southreach"));
    }

    @Test
    void endWarClearsAtWarForBothBelligerents() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);
        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();

        WarResult result = warService.endWar(war.id());

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(warService.isAtWar("northmarch"));
        assertFalse(warService.isAtWar("southreach"));
    }

    @Test
    void cannotEndWarThatDoesNotExist() {
        WarResult result = warService.endWar("no-such-war");

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void counterWarValidBetweenFormerDefenderAndFormerAttacker() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);
        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();
        warService.endWar(war.id());

        WarResult result = warService.validateCounterWarBill("southreach", "northmarch");

        assertInstanceOf(WarResult.Success.class, result);
    }

    @Test
    void counterWarRejectedWithoutPriorDefenderRole() {
        WarResult result = warService.validateCounterWarBill("southreach", "northmarch");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().toLowerCase().contains("defender"));
    }

    @Test
    void counterWarRejectedWhenProposerWasAttackerNotDefender() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);
        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();
        warService.endWar(war.id());

        WarResult result = warService.validateCounterWarBill("northmarch", "southreach");

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void counterWarRejectedWhileOriginalWarStillActive() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);

        WarResult result = warService.validateCounterWarBill("southreach", "northmarch");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().toLowerCase().contains("already at war"));
    }

    @Test
    void declarationMessageDescribesBelligerentsAndAim() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.CAPITAL_FALL, WarOutcome.WAR_TRIBUTE, 2);
        warService.enactWarBill("northmarch", payload);
        ActiveWar war = warService.activeWarFor("northmarch").orElseThrow();

        String message = warService.declarationMessage(war);

        assertTrue(message.contains("northmarch"));
        assertTrue(message.contains("southreach"));
        assertTrue(message.toLowerCase().contains("capital fall"));
    }
}
