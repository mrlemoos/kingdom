package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClericAudienceTest {

    @Test
    void theCrownIsReceivedForItsCoronationEvenWhileTheOathIsDisabled() {
        assertEquals(ClericAudience.CORONATION, ClericAudience.of(true, false, true));
        assertEquals(ClericAudience.CORONATION, ClericAudience.of(true, true, true));
    }

    @Test
    void aSubjectIsToldTheClericIsAtPrayerWhenNoOathMayBeSworn() {
        assertEquals(ClericAudience.AT_PRAYER, ClericAudience.of(false, false, true));
    }

    @Test
    void aSubjectAwayFromTheAltarIsSentToIt() {
        assertEquals(ClericAudience.AWAY_FROM_CHURCH, ClericAudience.of(false, true, false));
    }

    @Test
    void aSubjectAtTheAltarIsOfferedTheOath() {
        assertEquals(ClericAudience.OATH, ClericAudience.of(false, true, true));
    }
}
