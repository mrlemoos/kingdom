package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonsReturnTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private KingdomElectionState state;

    @BeforeEach
    void setUp() {
        state = new KingdomElectionState();
    }

    private static String nameOf(UUID playerId) {
        if (ALICE.equals(playerId)) {
            return "Alice";
        }
        if (BOB.equals(playerId)) {
            return "Bob";
        }
        return "Unknown";
    }

    private MpSeat seat(int index) {
        return state.seat(index).orElseThrow();
    }

    @Test
    void playerMpsAreRankedByVotesAheadOfVillagerMps() {
        seat(1).assignPlayer(BOB);
        seat(1).setReturnCount(9);
        seat(2).assignPlayer(ALICE);
        seat(2).setReturnCount(12);
        seat(3).assignVillager("librarian", null);
        seat(3).setReturnCount(3);
        seat(4).assignVillager("farmer", null);
        seat(4).setReturnCount(6);

        List<String> lines = CommonsReturn.rollCall(state, 8, CommonsReturnTest::nameOf);

        assertEquals(
                List.of(
                        "I report the return of the Commons.",
                        "MP Alice, returned with 12 votes.",
                        "MP Bob, returned with 9 votes.",
                        "MP Farmer, returned for Farmer, 6 villagers.",
                        "MP Librarian, returned for Librarian, 3 villagers.",
                        "4 seats stand vacant.",
                        "The Commons is duly returned."),
                lines);
    }

    @Test
    void singularCountsReadSingularly() {
        seat(1).assignPlayer(ALICE);
        seat(1).setReturnCount(1);
        seat(2).assignVillager("farmer", null);
        seat(2).setReturnCount(1);

        List<String> lines = CommonsReturn.rollCall(state, 3, CommonsReturnTest::nameOf);

        assertTrue(lines.contains("MP Alice, returned with 1 vote."), lines.toString());
        assertTrue(lines.contains("MP Farmer, returned for Farmer, 1 villager."), lines.toString());
        assertTrue(lines.contains("1 seat stands vacant."), lines.toString());
    }

    @Test
    void citizenBackfillIsReturnedUnopposedWithoutACount() {
        seat(1).assignVillager("none", null);
        seat(1).setReturnCount(4);

        List<String> lines = CommonsReturn.rollCall(state, 1, CommonsReturnTest::nameOf);

        assertEquals(
                List.of(
                        "I report the return of the Commons.",
                        "MP Citizen, returned unopposed for Citizen.",
                        "The Commons is duly returned."),
                lines);
    }

    @Test
    void seatsWithNoStoredCountReadUnopposedAndSortLast() {
        seat(1).assignPlayer(ALICE);
        seat(2).assignPlayer(BOB);
        seat(2).setReturnCount(2);
        seat(3).assignVillager("farmer", null);
        seat(4).assignVillager("librarian", null);
        seat(4).setReturnCount(5);

        List<String> lines = CommonsReturn.rollCall(state, 4, CommonsReturnTest::nameOf);

        assertEquals(
                List.of(
                        "I report the return of the Commons.",
                        "MP Bob, returned with 2 votes.",
                        "MP Alice, returned unopposed.",
                        "MP Librarian, returned for Librarian, 5 villagers.",
                        "MP Farmer, returned unopposed for Farmer.",
                        "The Commons is duly returned."),
                lines);
    }

    @Test
    void anEmptyCommonsStillReportsItsVacancies() {
        List<String> lines = CommonsReturn.rollCall(state, 8, CommonsReturnTest::nameOf);

        assertEquals(
                List.of(
                        "I report the return of the Commons.",
                        "8 seats stand vacant.",
                        "The Commons is duly returned."),
                lines);
    }

    @Test
    void aByElectionReturnsASingleSeat() {
        seat(5).assignPlayer(ALICE);
        seat(5).setReturnCount(7);

        assertEquals(
                "Seat 5 — MP Alice, returned with 7 votes.",
                CommonsReturn.seatReturn(seat(5), CommonsReturnTest::nameOf));
    }

    @Test
    void aVillagerByElectionReturnsItsConstituency() {
        seat(6).assignVillager("farmer", null);
        seat(6).setReturnCount(2);

        assertEquals(
                "Seat 6 — MP Farmer, returned for Farmer, 2 villagers.",
                CommonsReturn.seatReturn(seat(6), CommonsReturnTest::nameOf));
    }

    @Test
    void anEmptySeatHasNothingToReturn() {
        assertEquals("Seat 7 — vacant.", CommonsReturn.seatReturn(seat(7), CommonsReturnTest::nameOf));
    }
}
