package dev.mrlemoos.kingdom.loyalty;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for political loyalty tiers.
 * Keys are player ids already used by membership; no Bukkit world/entity ids.
 */
public interface LoyaltyStore {

    Optional<LoyaltyTier> findTier(UUID playerId);

    void putTier(UUID playerId, LoyaltyTier tier);

    Map<UUID, LoyaltyTier> allTiersView();

    void replaceAll(Map<UUID, LoyaltyTier> tiers);
}
