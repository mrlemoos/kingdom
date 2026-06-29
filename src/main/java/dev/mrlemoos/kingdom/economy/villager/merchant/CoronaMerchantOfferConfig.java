package dev.mrlemoos.kingdom.economy.villager.merchant;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record CoronaMerchantOfferConfig(Map<String, List<CoronaMerchantOffer>> offersByProfession) {

    public List<CoronaMerchantOffer> offersFor(String profession) {
        if (profession == null) {
            return List.of();
        }
        return offersByProfession.getOrDefault(profession.toLowerCase(), List.of());
    }

    public static CoronaMerchantOfferConfig defaults() {
        return new CoronaMerchantOfferConfig(Map.of(
                "farmer", List.of(new CoronaMerchantOffer(Material.BREAD, 3, 12)),
                "librarian", List.of(new CoronaMerchantOffer(Material.BOOK, 5, 12))));
    }

    public static CoronaMerchantOfferConfig fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        CoronaMerchantOfferConfig defaults = defaults();
        if (config == null || !config.isConfigurationSection("economy")) {
            return defaults;
        }
        ConfigurationSection economy = config.getConfigurationSection("economy");
        if (economy == null || !economy.isConfigurationSection("corona-merchant-offers")) {
            return defaults;
        }
        ConfigurationSection offersSection = economy.getConfigurationSection("corona-merchant-offers");
        if (offersSection == null) {
            return defaults;
        }

        Map<String, List<CoronaMerchantOffer>> offers = new java.util.LinkedHashMap<>();
        for (String profession : offersSection.getKeys(false)) {
            List<Map<?, ?>> offerMaps = offersSection.getMapList(profession);
            if (offerMaps.isEmpty()) {
                continue;
            }
            offers.put(
                    profession.toLowerCase(),
                    offerMaps.stream().map(CoronaMerchantOfferConfig::readOffer).toList());
        }
        return offers.isEmpty() ? defaults : new CoronaMerchantOfferConfig(Map.copyOf(offers));
    }

    private static CoronaMerchantOffer readOffer(Map<?, ?> map) {
        Material material = Material.matchMaterial(String.valueOf(map.get("item")));
        if (material == null || material.isAir()) {
            throw new IllegalArgumentException("Unknown Corona merchant offer item: " + map.get("item"));
        }
        int price = toInt(map.get("corona-price"));
        int maxUses = map.containsKey("max-uses") ? toInt(map.get("max-uses")) : 12;
        return new CoronaMerchantOffer(material, price, maxUses);
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
