package dev.leo.kingdom.economy.villager;

import java.util.List;

public record VillagerTradeEdge(String buyerProfession, String sellerProfession, double spendPercent) {

    public VillagerTradeEdge {
        buyerProfession = buyerProfession.toLowerCase();
        sellerProfession = sellerProfession.toLowerCase();
    }
}
