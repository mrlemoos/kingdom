package dev.mrlemoos.kingdom.model.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CandidateDeclarationTest {

    @Test
    void blankManifestoAndPartyAreValid() {
        assertTrue(CandidateDeclaration.rejectionReason("", "", "").isEmpty());
        CandidateDeclaration declared = CandidateDeclaration.of("", "", "");
        assertTrue(declared.isBlank());
        assertFalse(declared.hasManifesto());
        assertFalse(declared.hasParty());
    }

    @Test
    void overLengthManifestoIsRejected() {
        String tooLong = "a".repeat(CandidateDeclaration.MAX_MANIFESTO_LENGTH + 1);

        assertTrue(CandidateDeclaration.rejectionReason(tooLong, "Reform", "red").isPresent());
        assertThrows(IllegalArgumentException.class, () -> CandidateDeclaration.of(tooLong, "Reform", "red"));
    }

    @Test
    void manifestoAtTheCapIsAccepted() {
        String atCap = "a".repeat(CandidateDeclaration.MAX_MANIFESTO_LENGTH);

        assertTrue(CandidateDeclaration.rejectionReason(atCap, "", "").isEmpty());
    }

    @Test
    void overLengthPartyNameIsRejected() {
        String tooLong = "b".repeat(CandidateDeclaration.MAX_PARTY_NAME_LENGTH + 1);

        assertTrue(CandidateDeclaration.rejectionReason("Cheaper bread", tooLong, "red").isPresent());
    }

    @Test
    void partylessCandidateStandsAsIndependent() {
        assertEquals(CandidateDeclaration.INDEPENDENT_LABEL, CandidateDeclaration.of("Bread", "", "").partyLabel());
        assertEquals("Reform", CandidateDeclaration.of("Bread", "Reform", "red").partyLabel());
    }

    @Test
    void colourIsReadFromNameOrCode() {
        assertEquals("&c", CandidateDeclaration.of("", "Reform", "red").partyColour());
        assertEquals("&c", CandidateDeclaration.of("", "Reform", "&c").partyColour());
        assertEquals("&c", CandidateDeclaration.of("", "Reform", "c").partyColour());
        assertEquals(CandidateDeclaration.DEFAULT_PARTY_COLOUR, CandidateDeclaration.of("", "Reform", "").partyColour());
    }

    @Test
    void unknownColourIsRejected() {
        assertTrue(CandidateDeclaration.rejectionReason("", "Reform", "chartreuse").isPresent());
    }
}
