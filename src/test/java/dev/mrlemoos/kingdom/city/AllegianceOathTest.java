package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AllegianceOathTest {

    @Test
    void vacantThroneIsSwornToTheCrownAsOffice() {
        assertEquals("the Crown of Northmarch", AllegianceOath.addressee(null, "Northmarch"));
    }

    @Test
    void seatedMonarchIsNamed() {
        assertEquals("Queen Alice of Northmarch", AllegianceOath.addressee("Queen Alice", "Northmarch"));
    }

    @Test
    void oathNamesTheSwearerAndTheCrown() {
        String words = AllegianceOath.words("Bob", "Queen Alice of Northmarch");

        assertEquals(
                "I, Bob, do swear that I will be faithful and bear true allegiance to Queen Alice of Northmarch, and that I will uphold the laws and peace of this realm.",
                words);
        assertFalse(words.toLowerCase().contains("god"));
        assertFalse(words.toLowerCase().contains("heir"));
    }

    @Test
    void commandRefusalSendsThemToCityHall() {
        assertEquals(
                "This realm receives new members at city hall. Swear the oath of allegiance before the Lord Mayor of Northmarch.",
                AllegianceOath.hallJoinRefusal("Northmarch"));
    }

    @Test
    void theRealmHearsWhoSworeAndToWhom() {
        assertEquals(
                "Bob has sworn allegiance to Queen Alice of Northmarch.",
                AllegianceOath.realmAnnouncement("Bob", "Queen Alice of Northmarch"));
    }

    @Test
    void theSwearerIsToldTheyAreAMember() {
        String line = AllegianceOath.swearerMessage("the Crown of Northmarch", "Northmarch");
        assertTrue(line.contains("the Crown of Northmarch"));
        assertTrue(line.contains("Northmarch"));
        assertTrue(line.toLowerCase().contains("sworn"));
    }
}
