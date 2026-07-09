package dev.mrlemoos.kingdom.war.annexation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AnnexationConfigTest {

    @Test
    void onIsEnabled() {
        assertTrue(AnnexationConfig.on().enabled());
    }

    @Test
    void offIsDisabled() {
        assertFalse(AnnexationConfig.off().enabled());
    }

    @Test
    void fromPluginConfigReadsWarAnnexationEnabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("war.annexation.enabled", true);

        assertTrue(AnnexationConfig.fromPluginConfig(config).enabled());
    }

    @Test
    void fromPluginConfigDefaultsToDisabledForSafety() {
        YamlConfiguration config = new YamlConfiguration();

        assertFalse(AnnexationConfig.fromPluginConfig(config).enabled());
    }

    @Test
    void fromPluginConfigDefaultMatchesShippedConfigYml() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File("src/main/resources/config.yml"));

        assertFalse(AnnexationConfig.fromPluginConfig(config).enabled());
    }
}
