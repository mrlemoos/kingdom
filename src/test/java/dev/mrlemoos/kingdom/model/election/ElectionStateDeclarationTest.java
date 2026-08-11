package dev.mrlemoos.kingdom.model.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElectionStateDeclarationTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000d2");

    private ElectionState election;

    @BeforeEach
    void setUp() {
        election = new ElectionState();
        election.openGeneral(1_000L);
    }

    @Test
    void nominationCarriesTheDeclaration() {
        election.nominate(ALICE, 1L, CandidateDeclaration.of("Cheaper bread for the realm", "Reform", "red"));

        CandidateDeclaration declared = election.declaration(ALICE);
        assertEquals("Cheaper bread for the realm", declared.manifesto());
        assertEquals("Reform", declared.partyName());
        assertEquals("&c", declared.partyColour());
    }

    @Test
    void nominationWithoutADeclarationIsBlank() {
        election.nominate(BOB, 2L);

        assertTrue(election.declaration(BOB).isBlank());
        assertEquals(CandidateDeclaration.INDEPENDENT_LABEL, election.declaration(BOB).partyLabel());
    }

    @Test
    void declarationsSurviveRestore() {
        CandidateDeclaration alice = CandidateDeclaration.of("Cheaper bread", "Reform", "red");
        election.nominate(ALICE, 1L, alice);

        ElectionState restored = new ElectionState();
        restored.restore(
                ElectionType.GENERAL,
                ElectionPhase.OPEN,
                1_000L,
                null,
                election.nominationsView(),
                Map.of(ALICE, 1L),
                Map.of(),
                Set.of(),
                null,
                election.declarationsView());

        assertEquals(List.of(ALICE), restored.nominationsView());
        assertEquals(alice, restored.declaration(ALICE));
    }

    @Test
    void closingAnElectionClearsDeclarations() {
        election.nominate(ALICE, 1L, CandidateDeclaration.of("Cheaper bread", "Reform", "red"));
        election.close();

        assertTrue(election.declaration(ALICE).isBlank());
    }
}
