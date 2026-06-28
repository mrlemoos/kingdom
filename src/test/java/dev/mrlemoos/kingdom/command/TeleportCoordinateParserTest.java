package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TeleportCoordinateParserTest {

    @Test
    void parsesAbsoluteDouble() {
        assertEquals(100.5, TeleportCoordinateParser.parseComponent("100.5", 10.0), 1e-9);
    }

    @Test
    void tildeReturnsBase() {
        assertEquals(64.0, TeleportCoordinateParser.parseComponent("~", 64.0), 1e-9);
    }

    @Test
    void tildeOffsetAddsToBase() {
        assertEquals(74.0, TeleportCoordinateParser.parseComponent("~10", 64.0), 1e-9);
        assertEquals(54.0, TeleportCoordinateParser.parseComponent("~-10", 64.0), 1e-9);
    }

    @Test
    void invalidTokenFails() {
        assertThrows(IllegalArgumentException.class, () -> TeleportCoordinateParser.parseComponent("abc", 0));
        assertThrows(IllegalArgumentException.class, () -> TeleportCoordinateParser.parseComponent("", 0));
    }
}
