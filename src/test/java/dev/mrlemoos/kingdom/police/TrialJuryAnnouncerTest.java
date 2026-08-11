package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrialJuryAnnouncerTest {

    @Test
    void noticesNeverRevealIndividualBallots() {
        String seated = TrialJuryAnnouncer.kingdomSeatedNotice("Bob");
        String guilty = TrialJuryAnnouncer.kingdomGuilty("Bob", "prison sentence of 15 minutes.");
        String acquit = TrialJuryAnnouncer.kingdomAcquittal("Bob");
        assertTrue(seated.contains("Bob"));
        assertTrue(guilty.toLowerCase().contains("guilty"));
        assertTrue(acquit.toLowerCase().contains("acquits"));
        assertFalse(guilty.toLowerCase().contains("juror"));
        assertFalse(acquit.toLowerCase().contains("voted"));
    }
}
