package dev.mrlemoos.kingdom.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocateCoordFormatterTest {

    @Test
    void formatsBlockCoordinates() {
        assertEquals("120, 64, -30", LocateCoordFormatter.format(120, 64, -30));
    }
}
