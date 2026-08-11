package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class VillagerEconomyConfigStrikeTest {

    @Test
    void defaultsStrikeShorterThanEscheat() {
        VillagerEconomyConfig defaults = VillagerEconomyConfig.defaults();
        assertEquals(30, defaults.frozenWalletEscheatMcDays());
        assertEquals(7, defaults.frozenWalletStrikeMcDays());
    }

    @Test
    void fromPluginConfigReadsStrikeDays() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("economy.frozen-wallet-escheat-mc-days", 40);
        yaml.set("economy.frozen-wallet-strike-mc-days", 10);

        VillagerEconomyConfig config = VillagerEconomyConfig.fromPluginConfig(yaml);

        assertEquals(40, config.frozenWalletEscheatMcDays());
        assertEquals(10, config.frozenWalletStrikeMcDays());
    }

    @Test
    void fromPluginConfigFallsBackWhenStrikeNotShorterThanEscheat() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("economy.frozen-wallet-escheat-mc-days", 30);
        yaml.set("economy.frozen-wallet-strike-mc-days", 30);

        VillagerEconomyConfig config = VillagerEconomyConfig.fromPluginConfig(yaml);

        assertEquals(30, config.frozenWalletEscheatMcDays());
        assertEquals(7, config.frozenWalletStrikeMcDays());
    }

    @Test
    void constructorRejectsStrikeNotShorterThanEscheat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VillagerEconomyConfig(30, 30, 0.05, 3, java.util.List.of()));
    }
}
