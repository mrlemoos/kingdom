package dev.mrlemoos.kingdom.economy.villager.merchant;

import org.bukkit.Material;

public record CoronaMerchantOffer(Material material, int coronaPrice, int maxUses) {

    public CoronaMerchantOffer {
        if (coronaPrice <= 0) {
            throw new IllegalArgumentException("Corona price must be positive.");
        }
        if (maxUses <= 0) {
            throw new IllegalArgumentException("Max uses must be positive.");
        }
    }
}
