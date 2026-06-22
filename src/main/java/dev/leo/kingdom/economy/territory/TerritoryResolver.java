package dev.leo.kingdom.economy.territory;

public interface TerritoryResolver {

    TerritoryLocation resolve(String worldName, int x, int y, int z, String playerKingdomId);
}
