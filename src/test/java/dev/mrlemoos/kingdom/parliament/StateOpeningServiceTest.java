package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateOpeningServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID PRINCE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private StateOpeningService stateOpeningService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.joinKingdom(CITIZEN, "northmarch");
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService);
        stateOpeningService = new StateOpeningService(kingdomService, parliamentService, 3);
        kingdom().getParliamentSites().setLords(ChamberSite.of("world", 10, 64, 10));
        kingdom().getParliamentState().prorogue();
    }

    @Test
    void requestingWithALordsChamberAwaitsTheCrown() {
        assertEquals(StateOpeningSummons.AWAITING_CROWN, stateOpeningService.requestStateOpening("northmarch", 40L));

        assertTrue(stateOpeningService.isAwaitingStateOpening("northmarch"));
        assertFalse(parliamentService.isSessionOpen("northmarch"));
        assertEquals(40L, kingdom().getParliamentState().stateOpeningPendingSinceMcDay().orElseThrow());
    }

    @Test
    void requestingWithoutALordsChamberOpensByCommission() {
        kingdom().getParliamentSites().setLords(null);

        assertEquals(StateOpeningSummons.COMMISSIONED, stateOpeningService.requestStateOpening("northmarch", 40L));

        assertTrue(parliamentService.isSessionOpen("northmarch"));
        assertFalse(stateOpeningService.isAwaitingStateOpening("northmarch"));
    }

    @Test
    void requestingWhileAlreadyInSessionDoesNothing() {
        parliamentService.openSession("northmarch");

        assertEquals(StateOpeningSummons.NOT_NEEDED, stateOpeningService.requestStateOpening("northmarch", 40L));
    }

    @Test
    void heirOpensAsRegentWhenNoMonarchIsSeated() {
        stateOpeningService.requestStateOpening("northmarch", 40L);

        assertTrue(stateOpeningService.canOpen("northmarch", PRINCE));
        assertInstanceOf(ParliamentResult.Success.class, stateOpeningService.open("northmarch", PRINCE));
        assertTrue(parliamentService.isSessionOpen("northmarch"));
    }

    @Test
    void heirCannotOpenWhileAMonarchIsSeated() {
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        stateOpeningService.requestStateOpening("northmarch", 40L);

        assertFalse(stateOpeningService.canOpen("northmarch", PRINCE));
        assertTrue(stateOpeningService.canOpen("northmarch", KING));
        assertInstanceOf(ParliamentResult.Failure.class, stateOpeningService.open("northmarch", PRINCE));
    }

    @Test
    void citizensCannotOpenParliament() {
        stateOpeningService.requestStateOpening("northmarch", 40L);

        assertFalse(stateOpeningService.canOpen("northmarch", CITIZEN));
        assertInstanceOf(ParliamentResult.Failure.class, stateOpeningService.open("northmarch", CITIZEN));
    }

    @Test
    void commissionOpensParliamentOnceTheDelayHasPassed() {
        stateOpeningService.requestStateOpening("northmarch", 40L);

        assertTrue(stateOpeningService.commissionIfOverdue("northmarch", 42L).isEmpty());
        assertFalse(parliamentService.isSessionOpen("northmarch"));

        assertTrue(stateOpeningService.commissionIfOverdue("northmarch", 43L).isPresent());
        assertTrue(parliamentService.isSessionOpen("northmarch"));
        assertFalse(stateOpeningService.isAwaitingStateOpening("northmarch"));
    }

    @Test
    void commissionDoesNothingWhenNoOpeningIsPending() {
        assertTrue(stateOpeningService.commissionIfOverdue("northmarch", 99L).isEmpty());
    }

    @Test
    void openingTwiceFails() {
        stateOpeningService.requestStateOpening("northmarch", 40L);
        stateOpeningService.open("northmarch", PRINCE);

        assertInstanceOf(ParliamentResult.Failure.class, stateOpeningService.open("northmarch", PRINCE));
    }

    private Kingdom kingdom() {
        return kingdomService.getKingdom("northmarch").orElseThrow();
    }
}
