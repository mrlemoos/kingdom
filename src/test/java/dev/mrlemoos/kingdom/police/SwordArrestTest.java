package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwordArrestTest {

    @Test
    void aSwornConstableStrikingAWantedSubjectInJurisdictionWithAnIronSwordArrests() {
        assertEquals(SwordArrest.ARREST, SwordArrest.decide(true, true, true, true));
    }

    @Test
    void aStrikeByOneWhoIsNotAConstableIsOrdinaryCombat() {
        assertEquals(SwordArrest.PASS, SwordArrest.decide(false, true, true, true));
    }

    @Test
    void aConstableWithoutAnIronSwordFightsAsAnyoneElse() {
        assertEquals(SwordArrest.PASS, SwordArrest.decide(true, false, true, true));
    }

    @Test
    void aSubjectWithNoActiveWarrantIsNotArrested() {
        assertEquals(SwordArrest.PASS, SwordArrest.decide(true, true, false, true));
    }

    @Test
    void aWantedSubjectOutsideTheRealmsJurisdictionIsNotArrested() {
        assertEquals(SwordArrest.PASS, SwordArrest.decide(true, true, true, false));
    }
}
