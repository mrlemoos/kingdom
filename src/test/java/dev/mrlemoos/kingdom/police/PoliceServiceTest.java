package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoliceServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUEEN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KNIGHT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE_CANDIDATE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE_CANDIDATE = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PATROL_GOLEM = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID GUARD_GOLEM = UUID.fromString("00000000-0000-0000-0000-000000000011");

    private KingdomService kingdomService;
    private PoliceService policeService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(QUEEN, "northmarch");
        kingdomService.joinKingdom(KNIGHT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE_CANDIDATE, "northmarch");
        kingdomService.joinKingdom(JUDGE_CANDIDATE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KNIGHT, NobleRank.KNIGHT, TitleStyle.MASCULINE);
    }

    @Test
    void monarchAppointsConstable() {
        PoliceResult result = policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE_CANDIDATE);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(policeService.isConstable("northmarch", CONSTABLE_CANDIDATE));
    }

    @Test
    void nonMonarchCannotAppointConstable() {
        PoliceResult result = policeService.appointConstable("northmarch", NobleRank.KNIGHT, CONSTABLE_CANDIDATE);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertFalse(policeService.isConstable("northmarch", CONSTABLE_CANDIDATE));
    }

    @Test
    void cannotAppointJudgeWhenPlayerIsConstable() {
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE_CANDIDATE);

        PoliceResult result = policeService.appointJudge("northmarch", NobleRank.KING, CONSTABLE_CANDIDATE);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertFalse(policeService.isJudge("northmarch", CONSTABLE_CANDIDATE));
        assertTrue(policeService.isConstable("northmarch", CONSTABLE_CANDIDATE));
    }

    @Test
    void cannotAppointConstableWhenPlayerIsJudge() {
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE_CANDIDATE);

        PoliceResult result = policeService.appointConstable("northmarch", NobleRank.KING, JUDGE_CANDIDATE);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(policeService.isJudge("northmarch", JUDGE_CANDIDATE));
        assertFalse(policeService.isConstable("northmarch", JUDGE_CANDIDATE));
    }

    @Test
    void monarchDismissesConstable() {
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE_CANDIDATE);

        PoliceResult result = policeService.dismissConstable("northmarch", NobleRank.KING, CONSTABLE_CANDIDATE);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertFalse(policeService.isConstable("northmarch", CONSTABLE_CANDIDATE));
    }

    @Test
    void operatorCanSetCellWithoutBeingCrown() {
        PrisonCellLocation location = new PrisonCellLocation("world", 10, 64, 20);

        PoliceResult result = policeService.setCell("northmarch", NobleRank.KNIGHT, true, 1, location);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(location, policeService.cell("northmarch", 1).orElseThrow());
    }

    @Test
    void nonOperatorNonCrownCannotSetCell() {
        PrisonCellLocation location = new PrisonCellLocation("world", 10, 64, 20);

        PoliceResult result = policeService.setCell("northmarch", NobleRank.KNIGHT, false, 1, location);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(policeService.cell("northmarch", 1).isEmpty());
    }

    @Test
    void rejectsCellSlotAboveMax() {
        PrisonCellLocation location = new PrisonCellLocation("world", 10, 64, 20);

        PoliceResult result = policeService.setCell("northmarch", NobleRank.KING, false, 5, location);

        assertInstanceOf(PoliceResult.Failure.class, result);
    }

    @Test
    void lowestFreeCellSlotFindsFirstEmpty() {
        policeService.setCell(
                "northmarch",
                NobleRank.KING,
                false,
                1,
                new PrisonCellLocation("world", 1, 64, 1));
        policeService.setCell(
                "northmarch",
                NobleRank.KING,
                false,
                2,
                new PrisonCellLocation("world", 2, 64, 2));

        OptionalInt slot = policeService.lowestFreeCellSlot("northmarch");

        assertTrue(slot.isPresent());
        assertEquals(3, slot.getAsInt());
    }

    @Test
    void isPoliceReadyFalseWithoutCourt() {
        policeService.setCell(
                "northmarch",
                NobleRank.KING,
                false,
                1,
                new PrisonCellLocation("world", 10, 64, 20));

        assertFalse(policeService.isPoliceReady("northmarch"));
    }

    @Test
    void isPoliceReadyFalseWithoutCell() {
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 5, 64, 5));

        assertFalse(policeService.isPoliceReady("northmarch"));
    }

    @Test
    void isPoliceReadyTrueWithCellAndCourt() {
        policeService.setCell(
                "northmarch",
                NobleRank.KING,
                false,
                1,
                new PrisonCellLocation("world", 10, 64, 20));
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 5, 64, 5));

        assertTrue(policeService.isPoliceReady("northmarch"));
    }

    @Test
    void patrolGolemCapEnforced() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        assertInstanceOf(PoliceResult.Success.class, policeService.registerPatrolGolem("northmarch", first));
        assertInstanceOf(PoliceResult.Success.class, policeService.registerPatrolGolem("northmarch", second));
        PoliceResult thirdResult = policeService.registerPatrolGolem("northmarch", third);

        assertInstanceOf(PoliceResult.Failure.class, thirdResult);
    }

    @Test
    void guardGolemCapEnforced() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        assertInstanceOf(PoliceResult.Success.class, policeService.registerGuardGolem("northmarch", first));
        assertInstanceOf(PoliceResult.Success.class, policeService.registerGuardGolem("northmarch", second));
        PoliceResult thirdResult = policeService.registerGuardGolem("northmarch", third);

        assertInstanceOf(PoliceResult.Failure.class, thirdResult);
    }

    @Test
    void deregisterGolemRemovesPatrolOfficer() {
        policeService.registerPatrolGolem("northmarch", PATROL_GOLEM);

        PoliceResult result = policeService.deregisterGolem("northmarch", PATROL_GOLEM);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertFalse(policeService.isRegisteredGolem("northmarch", PATROL_GOLEM));
    }

    @Test
    void queenCanAppointJudge() {
        kingdomService.assignTitle(QUEEN, NobleRank.QUEEN, TitleStyle.FEMININE);

        PoliceResult result = policeService.appointJudge("northmarch", NobleRank.QUEEN, JUDGE_CANDIDATE);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(policeService.isJudge("northmarch", JUDGE_CANDIDATE));
    }

    @Test
    void clearCellRemovesConfiguredSlot() {
        policeService.setCell(
                "northmarch",
                NobleRank.KING,
                false,
                1,
                new PrisonCellLocation("world", 10, 64, 20));

        PoliceResult result = policeService.clearCell("northmarch", NobleRank.KING, false, 1);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(policeService.cell("northmarch", 1).isEmpty());
    }

    @Test
    void hasCourtReflectsCourtConfiguration() {
        assertFalse(policeService.hasCourt("northmarch"));

        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 5, 64, 5));

        assertTrue(policeService.hasCourt("northmarch"));
    }
}
