package dev.mrlemoos.kingdom.war.roster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Crown-maintained standing roster: permanent military core on explicit roster, auto-on-duty at
 * Steadfast with hardened service on war bill enactment. Knight title alone never implies roster
 * membership, and non-members (the closest stand-in for sworn outsiders until that model exists)
 * are never rostered.
 */
class StandingRosterServiceTest {

    private static final UUID ROSTERED_MEMBER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID KNIGHT_NOT_ROSTERED = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NON_MEMBER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private KingdomService kingdomService;
    private InMemoryStandingRosterStore store;
    private StandingRosterService rosterService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(ROSTERED_MEMBER, "northmarch");
        kingdomService.joinKingdom(KNIGHT_NOT_ROSTERED, "northmarch");
        kingdomService.assignTitle(KNIGHT_NOT_ROSTERED, NobleRank.KNIGHT, TitleStyle.MASCULINE);

        store = new InMemoryStandingRosterStore();
        rosterService = new StandingRosterService(kingdomService, store, StandingRosterConfig.defaults());
    }

    @Test
    void crownCanAppointMemberToStandingRoster() {
        WarResult result = rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Success.class, result);
        assertTrue(rosterService.rosterView("northmarch").contains(ROSTERED_MEMBER));
    }

    @Test
    void queenCanAppointMemberToStandingRoster() {
        WarResult result = rosterService.appoint("northmarch", NobleRank.QUEEN, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Success.class, result);
        assertTrue(rosterService.rosterView("northmarch").contains(ROSTERED_MEMBER));
    }

    @Test
    void nonCrownRankCannotAppointToStandingRoster() {
        WarResult result = rosterService.appoint("northmarch", NobleRank.DUKE, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().contains("King or Queen"));
        assertFalse(rosterService.rosterView("northmarch").contains(ROSTERED_MEMBER));
    }

    @Test
    void nonMemberCannotBeRostered() {
        WarResult result = rosterService.appoint("northmarch", NobleRank.KING, NON_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
        assertFalse(rosterService.rosterView("northmarch").contains(NON_MEMBER));
    }

    @Test
    void swornOutsiderIsNeverRostered() {
        // No sworn-outsider model exists yet; a non-member stands in for a sworn outsider and is
        // rejected by the same membership guard. Document this until Slice 3.x introduces a
        // dedicated sworn-outsider concept.
        WarResult result = rosterService.appoint("northmarch", NobleRank.KING, NON_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void knightTitleAloneDoesNotAutoRoster() {
        assertFalse(rosterService.rosterView("northmarch").contains(KNIGHT_NOT_ROSTERED));
    }

    @Test
    void cannotAppointSamePlayerTwice() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        WarResult result = rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void rosterCapIsEnforced() {
        StandingRosterService smallCapService =
                new StandingRosterService(kingdomService, store, new StandingRosterConfig(1));
        smallCapService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        WarResult result = smallCapService.appoint("northmarch", NobleRank.KING, KNIGHT_NOT_ROSTERED);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(((WarResult.Failure) result).message().toLowerCase().contains("full"));
    }

    @Test
    void crownCanRemoveMemberFromStandingRoster() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        WarResult result = rosterService.remove("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(rosterService.rosterView("northmarch").contains(ROSTERED_MEMBER));
    }

    @Test
    void nonCrownRankCannotRemoveFromStandingRoster() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        WarResult result = rosterService.remove("northmarch", NobleRank.DUKE, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(rosterService.rosterView("northmarch").contains(ROSTERED_MEMBER));
    }

    @Test
    void removingPlayerNotOnRosterFails() {
        WarResult result = rosterService.remove("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void rosteredMembersBecomeOnDutyAtSteadfastWithHardenedServiceOnMobilisation() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        rosterService.mobiliseOnWarEnactment("northmarch");

        assertTrue(rosterService.isOnDuty(ROSTERED_MEMBER));
        assertTrue(rosterService.hasHardenedService(ROSTERED_MEMBER));
        assertEquals(MoraleTier.STEADFAST, rosterService.moraleTier(ROSTERED_MEMBER).orElseThrow());
    }

    @Test
    void nonRosterKnightIsNotAutoOnDutyAfterMobilisation() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        rosterService.mobiliseOnWarEnactment("northmarch");

        assertFalse(rosterService.isOnDuty(KNIGHT_NOT_ROSTERED));
        assertFalse(rosterService.hasHardenedService(KNIGHT_NOT_ROSTERED));
        assertTrue(rosterService.moraleTier(KNIGHT_NOT_ROSTERED).isEmpty());
    }

    @Test
    void mobilisationForUnknownKingdomIsNoOp() {
        rosterService.mobiliseOnWarEnactment("no_such_kingdom");

        assertFalse(rosterService.isOnDuty(ROSTERED_MEMBER));
    }

    @Test
    void removingFromRosterClearsOnDutyState() {
        rosterService.appoint("northmarch", NobleRank.KING, ROSTERED_MEMBER);
        rosterService.mobiliseOnWarEnactment("northmarch");
        assertTrue(rosterService.isOnDuty(ROSTERED_MEMBER));

        rosterService.remove("northmarch", NobleRank.KING, ROSTERED_MEMBER);

        assertFalse(rosterService.isOnDuty(ROSTERED_MEMBER));
    }
}
