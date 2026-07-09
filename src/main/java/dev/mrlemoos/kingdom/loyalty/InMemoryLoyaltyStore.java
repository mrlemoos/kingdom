package dev.mrlemoos.kingdom.loyalty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryLoyaltyStore implements LoyaltyStore {

    private final Map<UUID, LoyaltyTier> tiers = new HashMap<>();

    @Override
    public Optional<LoyaltyTier> findTier(UUID playerId) {
        return Optional.ofNullable(tiers.get(playerId));
    }

    @Override
    public void putTier(UUID playerId, LoyaltyTier tier) {
        if (tier == null || tier == LoyaltyTier.FAITHFUL) {
            tiers.remove(playerId);
            return;
        }
        tiers.put(playerId, tier);
    }

    @Override
    public Map<UUID, LoyaltyTier> allTiersView() {
        return Map.copyOf(tiers);
    }

    @Override
    public void replaceAll(Map<UUID, LoyaltyTier> loaded) {
        tiers.clear();
        if (loaded != null) {
            for (Map.Entry<UUID, LoyaltyTier> entry : loaded.entrySet()) {
                if (entry.getKey() != null
                        && entry.getValue() != null
                        && entry.getValue() != LoyaltyTier.FAITHFUL) {
                    tiers.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
