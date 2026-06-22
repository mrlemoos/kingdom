package dev.leo.kingdom.economy.territory;

import java.util.Optional;

public record TerritoryLocation(Optional<String> kingdomId, IncomeLocation type) {

    public enum IncomeLocation {
        WILDERNESS,
        OWN_KINGDOM,
        FOREIGN_KINGDOM
    }

    public static TerritoryLocation wilderness() {
        return new TerritoryLocation(Optional.empty(), IncomeLocation.WILDERNESS);
    }

    public static TerritoryLocation ownKingdom() {
        return new TerritoryLocation(Optional.empty(), IncomeLocation.OWN_KINGDOM);
    }

    public static TerritoryLocation foreignKingdom(String kingdomId) {
        return new TerritoryLocation(Optional.of(kingdomId), IncomeLocation.FOREIGN_KINGDOM);
    }
}
