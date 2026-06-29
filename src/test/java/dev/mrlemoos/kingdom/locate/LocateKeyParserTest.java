package dev.mrlemoos.kingdom.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.block.Biome;
import org.bukkit.generator.structure.Structure;
import org.junit.jupiter.api.Test;

class LocateKeyParserTest {

    @Test
    void parsesStructureNames() {
        assertEquals(Structure.STRONGHOLD, LocateKeyParser.parseStructure("stronghold").orElseThrow());
        assertEquals(Structure.VILLAGE_PLAINS, LocateKeyParser.parseStructure("village").orElseThrow());
    }

    @Test
    void parsesBiomeKeys() {
        assertEquals(Biome.DESERT, LocateKeyParser.parseBiome("desert").orElseThrow());
        assertEquals(Biome.PLAINS, LocateKeyParser.parseBiome("plains").orElseThrow());
    }

    @Test
    void rejectsUnknownKeys() {
        assertTrue(LocateKeyParser.parseStructure("not_a_structure").isEmpty());
        assertTrue(LocateKeyParser.parseBiome("not_a_biome").isEmpty());
    }
}
