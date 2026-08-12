package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MpSubstitutionSelectorTest {

    @Test
    void prefersSameProfessionThenAnyEligible() {
        List<MpSubstitutionSelector.Candidate> pool = List.of(
                new MpSubstitutionSelector.Candidate("a", "farmer"),
                new MpSubstitutionSelector.Candidate("b", "librarian"),
                new MpSubstitutionSelector.Candidate("c", "farmer"));

        Optional<MpSubstitutionSelector.Candidate> chosen =
                MpSubstitutionSelector.select("librarian", pool);

        assertTrue(chosen.isPresent());
        assertEquals("b", chosen.get().id());
    }

    @Test
    void fallsBackToAnyEligibleWhenProfessionMissing() {
        List<MpSubstitutionSelector.Candidate> pool = List.of(
                new MpSubstitutionSelector.Candidate("a", "farmer"),
                new MpSubstitutionSelector.Candidate("c", "shepherd"));

        Optional<MpSubstitutionSelector.Candidate> chosen =
                MpSubstitutionSelector.select("librarian", pool);

        assertTrue(chosen.isPresent());
        assertEquals("a", chosen.get().id());
    }

    @Test
    void emptyPoolYieldsEmptySeat() {
        assertTrue(MpSubstitutionSelector.select("farmer", List.of()).isEmpty());
    }
}
