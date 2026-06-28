package dev.mrlemoos.kingdom.economy.territory;

import java.util.Optional;

public interface TerritoryResolver {

    TerritoryLocation resolve(String worldName, int x, int y, int z, String playerKingdomId);

    default Optional<String> owningKingdomId(String worldName, int x, int y, int z) {
        return Optional.empty();
    }
}
