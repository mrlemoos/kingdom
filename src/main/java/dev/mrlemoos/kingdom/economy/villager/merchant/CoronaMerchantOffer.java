package dev.mrlemoos.kingdom.economy.villager.merchant;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * A single Corona-priced merchant offer. {@code enchantment} is optional; when present the result is
 * enchanted (stored enchantment for an enchanted book, applied enchantment otherwise).
 */
public record CoronaMerchantOffer(
        Material material, int amount, int coronaPrice, int maxUses, Enchantment enchantment, int enchantmentLevel) {

    public CoronaMerchantOffer {
        if (amount <= 0) {
            throw new IllegalArgumentException("Offer amount must be positive.");
        }
        if (coronaPrice <= 0) {
            throw new IllegalArgumentException("Corona price must be positive.");
        }
        if (maxUses <= 0) {
            throw new IllegalArgumentException("Max uses must be positive.");
        }
        if (enchantment != null && enchantmentLevel <= 0) {
            throw new IllegalArgumentException("Enchantment level must be positive.");
        }
    }

    public CoronaMerchantOffer(Material material, int coronaPrice, int maxUses) {
        this(material, 1, coronaPrice, maxUses, null, 0);
    }

    public CoronaMerchantOffer(Material material, int amount, int coronaPrice, int maxUses) {
        this(material, amount, coronaPrice, maxUses, null, 0);
    }
}
