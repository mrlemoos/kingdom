package dev.mrlemoos.kingdom.economy.villager;

import java.util.List;
import java.util.UUID;

public record VillagerTradeSettlement(
        VillagerTradeEdge edge, UUID buyerId, UUID sellerId, double payment, double commerceTax) {}
