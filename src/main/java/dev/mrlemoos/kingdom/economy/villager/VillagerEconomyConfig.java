package dev.mrlemoos.kingdom.economy.villager;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record VillagerEconomyConfig(
        int frozenWalletEscheatMcDays,
        double villagerCommerceTaxRate,
        int settlementsPerEdge,
        List<VillagerTradeEdge> tradeEdges) {

    public static VillagerEconomyConfig defaults() {
        return new VillagerEconomyConfig(
                30,
                0.05,
                3,
                List.of(
                        VillagerTradeEdge.spendPercent("farmer", "butcher", 0.15),
                        VillagerTradeEdge.spendPercent("fisherman", "farmer", 0.10),
                        VillagerTradeEdge.spendPercent("librarian", "cartographer", 0.20),
                        VillagerTradeEdge.spendPercent("armorer", "weaponsmith", 0.12),
                        VillagerTradeEdge.spendPercent("shepherd", "leatherworker", 0.10),
                        VillagerTradeEdge.flatCorona("none", "farmer", 0.20)));
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
        int settlementsPerEdge = economy.getInt("villager-trades.settlements-per-edge", defaults.settlementsPerEdge());
        List<VillagerTradeEdge> edges = readTradeEdges(economy.getConfigurationSection("villager-trades"), defaults.tradeEdges());
        return new VillagerEconomyConfig(escheatDays, commerceTax, settlementsPerEdge, edges);
    }

    private static List<VillagerTradeEdge> readTradeEdges(ConfigurationSection section, List<VillagerTradeEdge> defaults) {
        if (section == null) {
            return defaults;
        }
        List<Map<?, ?>> edgeMaps = section.getMapList("edges");
        if (edgeMaps.isEmpty()) {
            return defaults;
        }
        return edgeMaps.stream().map(VillagerEconomyConfig::readTradeEdge).toList();
    }

    private static VillagerTradeEdge readTradeEdge(Map<?, ?> map) {
        String buyer = String.valueOf(map.get("buyer"));
        String seller = String.valueOf(map.get("seller"));
        if (map.containsKey("flat-corona")) {
            return VillagerTradeEdge.flatCorona(buyer, seller, toDouble(map.get("flat-corona")));
        }
        return VillagerTradeEdge.spendPercent(buyer, seller, toDouble(map.get("spend-percent")));
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
