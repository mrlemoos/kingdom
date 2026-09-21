package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SnowBiomeMapTest {

    private static final Function<String, Optional<String>> ALL_KNOWN = Optional::of;

    @Test
    void collidingPairIsDroppedAndTheRestKept() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("plains", "snowy_plains");
        raw.put("forest", "snowy_taiga");
        raw.put("sunflower_plains", "snowy_plains");
        List<String> warnings = new ArrayList<>();

        SnowBiomeMap map = SnowBiomeMap.parse(raw, ALL_KNOWN, warnings::add);

        assertEquals("snowy_plains", map.freeze("plains").orElseThrow());
        assertEquals("plains", map.thaw("snowy_plains").orElseThrow());
        assertEquals("snowy_taiga", map.freeze("forest").orElseThrow());
        assertEquals("forest", map.thaw("snowy_taiga").orElseThrow());
        assertTrue(map.freeze("sunflower_plains").isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void freezeAndThawRoundTrip() {
        SnowBiomeMap map = SnowBiomeMap.parse(
                Map.of("plains", "snowy_plains", "beach", "snowy_beach"),
                ALL_KNOWN,
                warning -> fail(warning));

        assertEquals("plains", map.thaw(map.freeze("plains").orElseThrow()).orElseThrow());
        assertEquals("beach", map.thaw(map.freeze("beach").orElseThrow()).orElseThrow());
    }

    @Test
    void unknownBiomeReturnsEmpty() {
        SnowBiomeMap map = SnowBiomeMap.parse(
                Map.of("plains", "snowy_plains"), ALL_KNOWN, warning -> fail(warning));

        assertTrue(map.freeze("desert").isEmpty());
        assertTrue(map.thaw("ice_spikes").isEmpty());
    }

    @Test
    void unknownRegistryKeyIsDroppedNotThrown() {
        List<String> warnings = new ArrayList<>();
        Function<String, Optional<String>> lookup =
                key -> "bogus".equals(key) ? Optional.empty() : Optional.of(key);
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("plains", "snowy_plains");
        raw.put("bogus", "snowy_taiga");
        raw.put("beach", "bogus");

        SnowBiomeMap map = SnowBiomeMap.parse(raw, lookup, warnings::add);

        assertEquals("snowy_plains", map.freeze("plains").orElseThrow());
        assertTrue(map.freeze("bogus").isEmpty());
        assertTrue(map.freeze("beach").isEmpty());
        assertEquals(2, warnings.size());
    }

    @Test
    void emptyMapIsATotalNoOp() {
        SnowBiomeMap map = SnowBiomeMap.parse(Map.of(), ALL_KNOWN, warning -> fail(warning));

        assertTrue(map.isEmpty());
        assertTrue(map.freeze("plains").isEmpty());
        assertTrue(map.thaw("snowy_plains").isEmpty());
    }

    @Test
    void fromPluginConfigReadsTheSwapTable() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("snow.biome-swap.plains", "snowy_plains");
        yaml.set("snow.biome-swap.beach", "snowy_beach");

        SnowBiomeMap map = SnowBiomeMap.fromPluginConfig(yaml, ALL_KNOWN, warning -> fail(warning));

        assertEquals("snowy_plains", map.freeze("plains").orElseThrow());
        assertEquals("beach", map.thaw("snowy_beach").orElseThrow());
    }

    @Test
    void fromPluginConfigTreatsAMissingTableAsEmpty() {
        SnowBiomeMap map =
                SnowBiomeMap.fromPluginConfig(new YamlConfiguration(), ALL_KNOWN, warning -> fail(warning));

        assertTrue(map.isEmpty());
    }
}
