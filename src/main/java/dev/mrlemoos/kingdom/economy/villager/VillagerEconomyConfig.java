package dev.mrlemoos.kingdom.economy.villager;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record VillagerEconomyConfig(
        int frozenWalletEscheatMcDays,
        double villagerCommerceTaxRate,
        List<VillagerTradeEdge> tradeEdges) {

    public static VillagerEconomyConfig defaults() {
        return new VillagerEconomyConfig(
                30,
                0.05,
                List.of(
                        new VillagerTradeEdge("farmer", "butcher", 0.15),
                        new VillagerTradeEdge("fisherman", "farmer", 0.10),
                        new VillagerTradeEdge("librarian", "cartographer", 0.20),
                        new VillagerTradeEdge("armorer", "weaponsmith", 0.12),
                        new VillagerTradeEdge("shepherd", "leatherworker", 0.10)));
    }

    public static VillagerEconomyConfig fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        VillagerEconomyConfig defaults = defaults();
        if (config == null || !config.isConfigurationSection("economy")) {
            return defaults;
        }
        ConfigurationSection economy = config.getConfigurationSection("economy");
        if (economy == null) {
            return defaults;
        }

        int escheatDays = economy.getInt("frozen-wallet-escheat-mc-days", defaults.frozenWalletEscheatMcDays());
        double commerceTax = economy.getDouble("villager-commerce-tax-rate", defaults.villagerCommerceTaxRate());
        List<VillagerTradeEdge> edges = readTradeEdges(economy.getConfigurationSection("villager-trades"), defaults.tradeEdges());
        return new VillagerEconomyConfig(escheatDays, commerceTax, edges);
    }

    private static List<VillagerTradeEdge> readTradeEdges(ConfigurationSection section, List<VillagerTradeEdge> defaults) {
        if (section == null) {
            return defaults;
        }
        List<Map<?, ?>> edgeMaps = section.getMapList("edges");
        if (edgeMaps.isEmpty()) {
            return defaults;
        }
        return edgeMaps.stream()
                .map(map -> new VillagerTradeEdge(
                        String.valueOf(map.get("buyer")),
                        String.valueOf(map.get("seller")),
                        toDouble(map.get("spend-percent"))))
                .toList();
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
