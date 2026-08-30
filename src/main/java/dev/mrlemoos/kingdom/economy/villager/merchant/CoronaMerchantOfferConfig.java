package dev.mrlemoos.kingdom.economy.villager.merchant;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

public record CoronaMerchantOfferConfig(Map<String, List<CoronaMerchantOffer>> offersByProfession) {

    public List<CoronaMerchantOffer> offersFor(String profession) {
        if (profession == null) {
            return List.of();
        }
        return offersByProfession.getOrDefault(profession.toLowerCase(), List.of());
    }

    public static CoronaMerchantOfferConfig defaults() {
        Map<String, List<CoronaMerchantOffer>> offers = new java.util.LinkedHashMap<>();
        offers.put(
                "farmer",
                List.of(
                        new CoronaMerchantOffer(Material.BREAD, 4, 3, 12),
                        new CoronaMerchantOffer(Material.GOLDEN_CARROT, 2, 8, 8),
                        new CoronaMerchantOffer(Material.CAKE, 1, 12, 4)));
        offers.put(
                "librarian",
                List.of(
                        new CoronaMerchantOffer(Material.BOOK, 4, 5, 12),
                        new CoronaMerchantOffer(Material.BOOKSHELF, 2, 12, 8),
                        new CoronaMerchantOffer(Material.NAME_TAG, 1, 24, 4),
                        book(Enchantment.MENDING, 1, 96, 1),
                        book(Enchantment.UNBREAKING, 3, 72, 2),
                        book(Enchantment.EFFICIENCY, 4, 64, 2),
                        book(Enchantment.FORTUNE, 3, 88, 1),
                        book(Enchantment.SILK_TOUCH, 1, 80, 1),
                        book(Enchantment.PROTECTION, 4, 64, 2),
                        book(Enchantment.SHARPNESS, 4, 60, 2),
                        book(Enchantment.LOOTING, 3, 72, 1),
                        book(Enchantment.FEATHER_FALLING, 4, 48, 2),
                        book(Enchantment.INFINITY, 1, 80, 1)));
        offers.put(
                "cleric",
                List.of(
                        new CoronaMerchantOffer(Material.EXPERIENCE_BOTTLE, 4, 16, 8),
                        new CoronaMerchantOffer(Material.ENDER_PEARL, 1, 20, 6),
                        new CoronaMerchantOffer(Material.GLOWSTONE, 4, 14, 8)));
        offers.put(
                "toolsmith",
                List.of(
                        new CoronaMerchantOffer(Material.IRON_PICKAXE, 1, 24, 6),
                        new CoronaMerchantOffer(Material.DIAMOND_PICKAXE, 1, 96, 2)));
        offers.put(
                "weaponsmith",
                List.of(
                        new CoronaMerchantOffer(Material.IRON_SWORD, 1, 20, 6),
                        new CoronaMerchantOffer(Material.DIAMOND_SWORD, 1, 96, 2)));
        offers.put(
                "armorer",
                List.of(
                        new CoronaMerchantOffer(Material.IRON_CHESTPLATE, 1, 32, 4),
                        new CoronaMerchantOffer(Material.SHIELD, 1, 18, 6)));
        offers.put(
                "cartographer",
                List.of(
                        new CoronaMerchantOffer(Material.MAP, 2, 6, 8),
                        new CoronaMerchantOffer(Material.COMPASS, 1, 18, 4)));
        offers.put(
                "fletcher",
                List.of(
                        new CoronaMerchantOffer(Material.ARROW, 16, 6, 12),
                        new CoronaMerchantOffer(Material.BOW, 1, 22, 4)));
        offers.put("butcher", List.of(new CoronaMerchantOffer(Material.COOKED_BEEF, 6, 6, 12)));
        offers.put("shepherd", List.of(new CoronaMerchantOffer(Material.WHITE_WOOL, 8, 6, 12)));
        offers.put("mason", List.of(new CoronaMerchantOffer(Material.STONE_BRICKS, 16, 8, 12)));
        offers.put("fisherman", List.of(new CoronaMerchantOffer(Material.COOKED_SALMON, 6, 6, 12)));
        offers.put("leatherworker", List.of(new CoronaMerchantOffer(Material.LEATHER, 6, 8, 12)));
        return new CoronaMerchantOfferConfig(Map.copyOf(offers));
    }

    private static CoronaMerchantOffer book(Enchantment enchantment, int level, int coronaPrice, int maxUses) {
        return new CoronaMerchantOffer(Material.ENCHANTED_BOOK, 1, coronaPrice, maxUses, enchantment, level);
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
        int amount = map.containsKey("amount") ? toInt(map.get("amount")) : 1;
        int price = toInt(map.get("corona-price"));
        int maxUses = map.containsKey("max-uses") ? toInt(map.get("max-uses")) : 12;
        Enchantment enchantment = readEnchantment(map.get("enchantment"));
        int level = enchantment == null ? 0 : Math.max(1, toInt(map.get("enchantment-level")));
        return new CoronaMerchantOffer(material, amount, price, maxUses, enchantment, level);
    }

    private static Enchantment readEnchantment(Object value) {
        if (value == null) {
            return null;
        }
        String name = String.valueOf(value).toLowerCase();
        NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name) : NamespacedKey.minecraft(name);
        Enchantment enchantment = key == null ? null : Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown Corona merchant offer enchantment: " + value);
        }
        return enchantment;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
