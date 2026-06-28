package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerPremierSelectorTest {

    private KingdomElectionState electionState;

    @BeforeEach
    void setUp() {
        electionState = new KingdomElectionState();
    }

    @Test
    void selectsHighestProfessionCountAmongSeatedProfessionMps() {
        assignVillager(1, "farmer");
        assignVillager(2, "librarian");
        assignVillager(3, "none");

        OptionalInt seat = VillagerPremierSelector.selectPremierSeat(
                electionState, Map.of("farmer", 10, "librarian", 8));

        assertEquals(1, seat.orElseThrow());
    }

    @Test
    void excludesCitizenBackfillSeats() {
        assignVillager(1, "none");
        assignVillager(2, "farmer");

        OptionalInt seat = VillagerPremierSelector.selectPremierSeat(
                electionState, Map.of("none", 20, "farmer", 5));

        assertEquals(2, seat.orElseThrow());
    }

    @Test
    void seatOrderBreaksProfessionCountTie() {
        assignVillager(2, "librarian");
        assignVillager(5, "farmer");

        OptionalInt seat = VillagerPremierSelector.selectPremierSeat(
                electionState, Map.of("farmer", 10, "librarian", 10));

        assertEquals(2, seat.orElseThrow());
    }

    @Test
    void seatOrderBreaksTieWithinSameProfession() {
        assignVillager(3, "farmer");
        assignVillager(7, "farmer");

        OptionalInt seat = VillagerPremierSelector.selectPremierSeat(
                electionState, Map.of("farmer", 10));

        assertEquals(3, seat.orElseThrow());
    }

    @Test
    void emptyWhenNoEligibleProfessionSeats() {
        assignVillager(1, "none");
        assignVillager(2, "none");

        assertTrue(VillagerPremierSelector.selectPremierSeat(
                electionState, Map.of("none", 50)).isEmpty());
    }

    private void assignVillager(int index, String profession) {
        MpSeat seat = electionState.seat(index).orElseThrow();
        seat.assignVillager(profession, null);
    }
}
