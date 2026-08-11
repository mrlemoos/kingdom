package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WantedNametagPolicyTest {

    @Test
    void showsWantedWhenPlayerHasActiveWarrantInsideJurisdiction() {
        assertTrue(WantedNametagPolicy.shouldShow(true, true, true));
    }

    @Test
    void hidesOutsideJurisdictionEvenWithActiveWarrant() {
        assertFalse(WantedNametagPolicy.shouldShow(true, true, false));
    }

    @Test
    void hidesWithoutActiveWarrantInsideJurisdiction() {
        assertFalse(WantedNametagPolicy.shouldShow(true, false, true));
    }

    @Test
    void neverShowsOnVillagers() {
        assertFalse(WantedNametagPolicy.shouldShow(false, true, true));
    }

    @Test
    void wantedPrefixReplacesNobleAndSworn() {
        String composed = WantedNametagPolicy.composePrefix(
                true,
                PoliceService.CONSTABLE_CHAT_COLOR + "[Constable] ",
                "&6[King] ");

        assertEquals(WantedNametagPolicy.colouredWantedPrefix(), composed);
        assertTrue(composed.contains("[WANTED]"));
    }

    @Test
    void withoutWantedKeepsSwornThenNoble() {
        String sworn = PoliceService.CONSTABLE_CHAT_COLOR + "[Constable] ";
        String noble = "&f[Knight] ";

        assertEquals(sworn + noble, WantedNametagPolicy.composePrefix(false, sworn, noble));
    }
}
