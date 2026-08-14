package dev.mrlemoos.kingdom.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import org.junit.jupiter.api.Test;

class ElectionBarTextTest {

    @Test
    void generalElectionReadsTypeAndRemainingMinutes() {
        assertEquals(
                "General election — 45m",
                ElectionBarText.label(ElectionType.GENERAL, ElectionPhase.OPEN, 45L * 60_000L));
    }

    @Test
    void byElectionAndPremierUseTheirOwnNames() {
        assertEquals(
                "By-election — 10m",
                ElectionBarText.label(ElectionType.BY_ELECTION_PLAYER, ElectionPhase.OPEN, 10L * 60_000L));
        assertEquals(
                "By-election — 10m",
                ElectionBarText.label(ElectionType.BY_ELECTION_VILLAGER, ElectionPhase.OPEN, 10L * 60_000L));
        assertEquals(
                "Premier election — 1h 5m",
                ElectionBarText.label(ElectionType.PREMIER, ElectionPhase.OPEN, 3_900_000L));
    }

    @Test
    void underAMinuteReadsSeconds() {
        assertEquals(
                "General election — 30s",
                ElectionBarText.label(ElectionType.GENERAL, ElectionPhase.OPEN, 30_000L));
    }

    @Test
    void awaitingSpeakerStopsTheClock() {
        assertEquals(
                "General election — awaiting Speaker",
                ElectionBarText.label(ElectionType.GENERAL, ElectionPhase.AWAITING_SPEAKER_TIE, 0L));
    }

    @Test
    void progressDrainsWithThePollingWindow() {
        assertEquals(1.0f, ElectionBarText.progress(3_600_000L, 3_600_000L), 0.0001f);
        assertEquals(0.5f, ElectionBarText.progress(1_800_000L, 3_600_000L), 0.0001f);
        assertEquals(0.0f, ElectionBarText.progress(0L, 3_600_000L), 0.0001f);
    }
}
