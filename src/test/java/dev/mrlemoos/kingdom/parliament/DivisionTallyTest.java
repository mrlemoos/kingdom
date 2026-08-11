package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.StableSeatUuid;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DivisionTallyTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CARA = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private KingdomElectionState state;
    private Map<UUID, VoteChoice> votes;

    @BeforeEach
    void setUp() {
        state = new KingdomElectionState();
        votes = new HashMap<>();
    }

    private MpSeat playerSeat(int index, UUID holder, String party) {
        MpSeat seat = state.seat(index).orElseThrow();
        seat.assignPlayer(holder);
        seat.setDeclaration(CandidateDeclaration.of("Cheaper bread", party, "red"));
        return seat;
    }

    private MpSeat villagerSeat(int index, String profession) {
        MpSeat seat = state.seat(index).orElseThrow();
        seat.assignVillager(profession, null);
        return seat;
    }

    private List<DivisionBloc> tally() {
        return DivisionTally.tally(KINGDOM, votes, state.seatsView().values());
    }

    private static Optional<DivisionBloc> bloc(List<DivisionBloc> blocs, String label) {
        return blocs.stream().filter(bloc -> bloc.label().equals(label)).findFirst();
    }

    @Test
    void twoPlayersOfTheSamePartyGroupTogether() {
        playerSeat(1, ALICE, "Reform");
        playerSeat(2, BOB, "Reform");
        votes.put(ALICE, VoteChoice.AYE);
        votes.put(BOB, VoteChoice.NAY);

        List<DivisionBloc> blocs = tally();

        assertEquals(1, blocs.size());
        DivisionBloc reform = blocs.getFirst();
        assertEquals(DivisionBlocKind.PARTY, reform.kind());
        assertEquals("Reform", reform.label());
        assertEquals(1, reform.aye());
        assertEquals(1, reform.nay());
        assertEquals(0, reform.abstain());
    }

    @Test
    void differentPartiesStandApart() {
        playerSeat(1, ALICE, "Reform");
        playerSeat(2, BOB, "Loyalist");
        votes.put(ALICE, VoteChoice.AYE);
        votes.put(BOB, VoteChoice.NAY);

        List<DivisionBloc> blocs = tally();

        assertEquals(2, blocs.size());
        assertEquals(1, bloc(blocs, "Reform").orElseThrow().aye());
        assertEquals(1, bloc(blocs, "Loyalist").orElseThrow().nay());
    }

    @Test
    void partylessPlayerMpGroupsAsIndependent() {
        MpSeat seat = state.seat(1).orElseThrow();
        seat.assignPlayer(ALICE);
        seat.setDeclaration(CandidateDeclaration.of("Cheaper bread", "", ""));
        votes.put(ALICE, VoteChoice.ABSTAIN);

        List<DivisionBloc> blocs = tally();

        assertEquals(1, blocs.size());
        assertEquals(DivisionBlocKind.INDEPENDENT, blocs.getFirst().kind());
        assertEquals(CandidateDeclaration.INDEPENDENT_LABEL, blocs.getFirst().label());
        assertEquals(1, blocs.getFirst().abstain());
    }

    @Test
    void villagersGroupByProfessionAndNeverByParty() {
        villagerSeat(5, "farmer");
        villagerSeat(6, "farmer");
        villagerSeat(7, "librarian");
        // A villager bench refuses a party even when one is pressed on it.
        state.seat(5).orElseThrow().setDeclaration(CandidateDeclaration.of("", "Reform", "red"));
        votes.put(StableSeatUuid.forSeat(KINGDOM, 5), VoteChoice.AYE);
        votes.put(StableSeatUuid.forSeat(KINGDOM, 6), VoteChoice.AYE);
        votes.put(StableSeatUuid.forSeat(KINGDOM, 7), VoteChoice.NAY);

        List<DivisionBloc> blocs = tally();

        assertTrue(state.seat(5).orElseThrow().declaration().isEmpty());
        assertFalse(bloc(blocs, "Reform").isPresent());
        assertEquals(2, blocs.size());
        DivisionBloc farmers = bloc(blocs, "Farmer").orElseThrow();
        assertEquals(DivisionBlocKind.PROFESSION, farmers.kind());
        assertEquals(2, farmers.aye());
        assertEquals(1, bloc(blocs, "Librarian").orElseThrow().nay());
    }

    @Test
    void partiesAreReadBeforeIndependentsAndProfessionBlocs() {
        playerSeat(1, ALICE, "Reform");
        MpSeat independent = state.seat(2).orElseThrow();
        independent.assignPlayer(BOB);
        villagerSeat(5, "farmer");
        votes.put(ALICE, VoteChoice.AYE);
        votes.put(BOB, VoteChoice.NAY);
        votes.put(StableSeatUuid.forSeat(KINGDOM, 5), VoteChoice.AYE);

        List<DivisionBloc> blocs = tally();

        assertEquals(
                List.of(DivisionBlocKind.PARTY, DivisionBlocKind.INDEPENDENT, DivisionBlocKind.PROFESSION),
                blocs.stream().map(DivisionBloc::kind).toList());
    }

    @Test
    void votesFromMembersWithoutASeatAreDisregarded() {
        playerSeat(1, ALICE, "Reform");
        votes.put(ALICE, VoteChoice.AYE);
        votes.put(CARA, VoteChoice.NAY);

        List<DivisionBloc> blocs = tally();

        assertEquals(1, blocs.size());
        assertEquals(1, blocs.getFirst().aye());
        assertEquals(0, blocs.getFirst().nay());
    }
}
