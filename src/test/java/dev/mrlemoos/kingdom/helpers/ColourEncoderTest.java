package dev.mrlemoos.kingdom.helpers;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ColourEncoderTest {

    @Test
    void translatesAmpersandColourCodesToSectionSigns() {
        assertEquals("§6Gold", ColourEncoder.c("&6Gold"));
        assertEquals("§a§lBold green", ColourEncoder.c("&a&lBold green"));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("Hello", ColourEncoder.c("Hello"));
    }

    @Test
    void rejectsNullText() {
        assertThrows(IllegalArgumentException.class, () -> ColourEncoder.c(null));
    }

    @Test
    void bareColourCodeSerialisesForConcatenation() {
        assertEquals("§9", c("&9"));
    }

    @Test
    void stripsLegacyColourCodes() {
        assertEquals("[MP] Farmer", ColourEncoder.strip(c("&7[MP] &fFarmer")));
    }
}
