package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

/**
 * The store is the Crown's: only a King or Queen, or a Prince or Princess, may break hay out of
 * their own realm's granary. Every other hand — member, permit holder or operator — steals it.
 */
class GrainTheftTest {

    private static final String KINGDOM = "northmarch";

    @Test
    void theCrownMayDrawOnItsOwnGranary() {
        assertFalse(GrainTheft.isTheft(NobleRank.KING, KINGDOM, KINGDOM, false));
        assertFalse(GrainTheft.isTheft(NobleRank.QUEEN, KINGDOM, KINGDOM, false));
        assertFalse(GrainTheft.isTheft(NobleRank.PRINCE, KINGDOM, KINGDOM, false));
    }

    @Test
    void theCrownOfAnotherRealmIsAThiefLikeAnyOther() {
        assertTrue(GrainTheft.isTheft(NobleRank.KING, "southreach", KINGDOM, false));
    }

    @Test
    void noSubjectBelowTheCrownMayDrawOnTheGranary() {
        assertTrue(GrainTheft.isTheft(NobleRank.PREMIER, KINGDOM, KINGDOM, false));
        assertTrue(GrainTheft.isTheft(NobleRank.DUKE, KINGDOM, KINGDOM, false));
        assertTrue(GrainTheft.isTheft(NobleRank.KNIGHT, KINGDOM, KINGDOM, false));
        assertTrue(GrainTheft.isTheft(null, KINGDOM, KINGDOM, false));
    }

    @Test
    void aForeignerStealsAsAnyoneElseDoes() {
        assertTrue(GrainTheft.isTheft(NobleRank.KNIGHT, "southreach", KINGDOM, false));
        assertTrue(GrainTheft.isTheft(null, null, KINGDOM, false));
    }

    @Test
    void anOperatorIsNotExemptAsTheyAreNotFromTheBuildPermitGate() {
        assertTrue(GrainTheft.isTheft(NobleRank.KNIGHT, KINGDOM, KINGDOM, true));
        assertTrue(GrainTheft.isTheft(null, null, KINGDOM, true));
        assertFalse(GrainTheft.isTheft(NobleRank.KING, KINGDOM, KINGDOM, true));
    }

    @Test
    void theSyntheticActIsNamedForTheCharge() {
        assertEquals("granary-grain-theft", GrainTheft.BILL_ID);
    }
}
