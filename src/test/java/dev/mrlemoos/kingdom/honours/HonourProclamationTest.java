package dev.mrlemoos.kingdom.honours;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HonourProclamationTest {

    @Test
    @DisplayName("the realm hears the Crown create a knight")
    void theRealmHearsTheCrownCreateAKnight() {
        assertEquals("The Crown creates Leo a Knight.", HonourProclamation.line("Leo", "Knight"));
    }

    @Test
    @DisplayName("a knight is told to arise")
    void aKnightIsToldToArise() {
        assertEquals("Arise and be recognised", HonourProclamation.screenSubheading("Knight"));
        assertEquals("Arise and be recognised", HonourProclamation.screenSubheading("Dame"));
    }

    @Test
    @DisplayName("other honours are conferred by the Crown")
    void otherHonoursAreConferredByTheCrown() {
        assertEquals("The Crown confers this honour", HonourProclamation.screenSubheading("Duke"));
        assertEquals("The Crown creates Ada a Duchess.", HonourProclamation.line("Ada", "Duchess"));
    }
}
