package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import java.util.Optional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DecreeCurfewResolverTest {

    @Test
    void emptyDecreeUsesPluginFallback() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enforcement.curfew.enabled", true);
        config.set("enforcement.curfew.window-start-tick", 13_000L);
        config.set("enforcement.curfew.window-end-tick", 23_000L);

        CurfewEnforcementConfig resolved =
                DecreeCurfewResolver.resolve(Optional.empty(), config);

        assertTrue(resolved.enabled());
        assertEquals(23_000L, resolved.windowEndTick());
    }

    @Test
    void presentDecreeWinsOverPlugin() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enforcement.curfew.enabled", true);
        config.set("enforcement.curfew.window-start-tick", 13_000L);
        config.set("enforcement.curfew.window-end-tick", 23_000L);

        CurfewEnforcementConfig resolved = DecreeCurfewResolver.resolve(
                Optional.of(CurfewPresets.nightfallMidnight()), config);

        assertTrue(resolved.enabled());
        assertEquals(18_000L, resolved.windowEndTick());
    }

    @Test
    void liftedDecreeDisablesEvenWhenPluginEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enforcement.curfew.enabled", true);

        CurfewEnforcementConfig resolved =
                DecreeCurfewResolver.resolve(Optional.of(CurfewPresets.lifted()), config);

        assertFalse(resolved.enabled());
    }
}
