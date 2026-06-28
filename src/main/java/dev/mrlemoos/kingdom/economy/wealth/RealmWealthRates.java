package dev.mrlemoos.kingdom.economy.wealth;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record RealmWealthRates(Map<WealthBlockType, Double> coronaPerBlock) {

    public double coronaValue(WealthBlockType type) {
        return coronaPerBlock.getOrDefault(type, 0.0);
    }

    public static RealmWealthRates defaults() {
        Map<WealthBlockType, Double> values = new EnumMap<>(WealthBlockType.class);
        values.put(WealthBlockType.GOLD_BLOCK, 100.0);
        values.put(WealthBlockType.DIAMOND_BLOCK, 500.0);
        values.put(WealthBlockType.EMERALD_BLOCK, 250.0);
        values.put(WealthBlockType.IRON_BLOCK, 50.0);
        values.put(WealthBlockType.COPPER_BLOCK, 25.0);
        values.put(WealthBlockType.LODESTONE, 50.0);
        values.put(WealthBlockType.CONDUIT, 250.0);
        values.put(WealthBlockType.BEACON, 500.0);
        return new RealmWealthRates(values);
    }

    public static RealmWealthRates fromPluginConfig(ConfigurationSection economySection) {
        RealmWealthRates defaults = defaults();
        if (economySection == null) {
            return defaults;
        }
        ConfigurationSection wealthSection = economySection.getConfigurationSection("realm-wealth");
        if (wealthSection == null) {
            return defaults;
        }

        Map<WealthBlockType, Double> values = new EnumMap<>(defaults.coronaPerBlock());
        readSection(wealthSection.getConfigurationSection("materials"), values, defaults);
        readSection(wealthSection.getConfigurationSection("estates"), values, defaults);
        return new RealmWealthRates(values);
    }

    private static void readSection(
            ConfigurationSection section, Map<WealthBlockType, Double> values, RealmWealthRates defaults) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            WealthBlockType.fromConfigKey(key).ifPresent(type -> values.put(type, section.getDouble(key, defaults.coronaValue(type))));
        }
    }
}
