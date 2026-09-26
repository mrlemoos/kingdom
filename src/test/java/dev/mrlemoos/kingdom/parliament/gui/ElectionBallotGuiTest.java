package dev.mrlemoos.kingdom.parliament.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.poll.PollCardOffer;
import dev.mrlemoos.kingdom.poll.PollCardRules;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElectionBallotGuiTest {

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-0000000000d2");

    @Test
    void aCitizenIsOfferedToStandAndToVoteForEachCandidate() {
        PollCardOffer offer = new PollCardOffer(
                PollCardRules.PollKind.ELECTION, true, false, true, List.of(FIRST, SECOND), false);
        ElectionBallotGui gui = new ElectionBallotGui("northmarch", "election:GENERAL:1", offer);

        assertTrue(gui.isStandSlot(ElectionBallotGui.SLOT_STAND));
        assertTrue(gui.isStandDeclaredSlot(ElectionBallotGui.SLOT_STAND_DECLARED));
        assertFalse(gui.isStandSlot(ElectionBallotGui.SLOT_STAND_DECLARED));
        assertEquals(Optional.of(FIRST), gui.candidateForSlot(ElectionBallotGui.FIRST_CANDIDATE_SLOT));
        assertEquals(Optional.of(SECOND), gui.candidateForSlot(ElectionBallotGui.FIRST_CANDIDATE_SLOT + 1));
        assertEquals(Optional.empty(), gui.candidateForSlot(ElectionBallotGui.FIRST_CANDIDATE_SLOT + 2));
    }

    @Test
    void aStandingCandidateIsNotOfferedToStandAgain() {
        PollCardOffer offer = new PollCardOffer(
                PollCardRules.PollKind.ELECTION, false, true, true, List.of(FIRST), false);

        ElectionBallotGui gui = new ElectionBallotGui("northmarch", "p", offer);
        assertFalse(gui.isStandSlot(ElectionBallotGui.SLOT_STAND));
        assertFalse(gui.isStandDeclaredSlot(ElectionBallotGui.SLOT_STAND_DECLARED));
    }

    @Test
    void aMemberWhoMayNotVoteIsShownNoCandidatesToVoteFor() {
        PollCardOffer offer = new PollCardOffer(
                PollCardRules.PollKind.ELECTION, true, false, false, List.of(), false);
        ElectionBallotGui gui = new ElectionBallotGui("northmarch", "p", offer);

        assertEquals(Optional.empty(), gui.candidateForSlot(ElectionBallotGui.FIRST_CANDIDATE_SLOT));
        assertTrue(gui.isStandSlot(ElectionBallotGui.SLOT_STAND));
    }

    @Test
    void theSpeakersCastingVoteIsAmongTheTiedAlone() {
        PollCardOffer offer = new PollCardOffer(
                PollCardRules.PollKind.SPEAKER_TIE, false, false, true, List.of(SECOND), false);
        ElectionBallotGui gui = new ElectionBallotGui("northmarch", "p:tie", offer);

        assertTrue(gui.speakerVote());
        assertEquals(Optional.of(SECOND), gui.candidateForSlot(ElectionBallotGui.FIRST_CANDIDATE_SLOT));
        assertFalse(gui.isStandSlot(ElectionBallotGui.SLOT_STAND));
    }
}
