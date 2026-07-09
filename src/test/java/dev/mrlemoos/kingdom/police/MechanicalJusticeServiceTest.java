package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MechanicalJusticeServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRINCE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        kingdomService.assignTitle(CONSTABLE, NobleRank.KNIGHT, TitleStyle.MASCULINE);

        policeService.setCell("northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt("northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
    }

    @Test
    void actBreachCreatesPendingWarrantAwaitingCrown() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);

        PoliceResult result = justice.openFromActBreach(breach, SUSPECT);

        assertInstanceOf(PoliceResult.Success.class, result);
        Optional<Warrant> warrant = justice.findPendingForSuspect("northmarch", SUSPECT);
        assertTrue(warrant.isPresent());
        assertEquals(WarrantStatus.PENDING_CROWN, warrant.get().status());
        assertEquals("northmarch-build", warrant.get().actBillId());
        assertEquals(ConductKind.BUILD_BAN, warrant.get().provisionKind());
        assertFalse(justice.hasActiveWarrant("northmarch", SUSPECT));
    }

    @Test
    void crownApprovalActivatesWarrant() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();

        PoliceResult result = justice.approveWarrant("northmarch", KING, pending.id());

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(justice.hasActiveWarrant("northmarch", SUSPECT));
        assertTrue(justice.findPendingForSuspect("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void crownRejectionLeavesNoActiveWarrant() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();

        PoliceResult result = justice.rejectWarrant("northmarch", KING, pending.id());

        assertInstanceOf(PoliceResult.Success.class, result);
        assertFalse(justice.hasActiveWarrant("northmarch", SUSPECT));
        assertTrue(justice.findPendingForSuspect("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void immunityBlocksWarrantAgainstKingQueenOrPrince() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);

        PoliceResult againstKing = justice.openFromActBreach(breach, KING);
        PoliceResult againstPrince = justice.openFromActBreach(breach, PRINCE);

        assertInstanceOf(PoliceResult.Failure.class, againstKing);
        assertInstanceOf(PoliceResult.Failure.class, againstPrince);
        assertFalse(justice.hasActiveWarrant("northmarch", KING));
        assertTrue(justice.findPendingForSuspect("northmarch", KING).isEmpty());
        assertTrue(justice.findPendingForSuspect("northmarch", PRINCE).isEmpty());
    }

    @Test
    void infrastructureGateBlocksWarrantWhenPoliceNotReady() {
        kingdomService.createKingdom("southreach", "Southreach");
        UUID visitor = UUID.fromString("00000000-0000-0000-0000-000000000099");
        kingdomService.joinKingdom(visitor, "southreach");
        ActBreach breach = new ActBreach("southreach", "southreach-build", ConductKind.BUILD_BAN);

        PoliceResult result = justice.openFromActBreach(breach, visitor);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(justice.findPendingForSuspect("southreach", visitor).isEmpty());
    }

    @Test
    void disabledFlagDoesNotOpenWarrant() {
        MechanicalJusticeService disabled = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.disabled());
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);

        PoliceResult result = disabled.openFromActBreach(breach, SUSPECT);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(disabled.findPendingForSuspect("northmarch", SUSPECT).isEmpty());
    }
}
