package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryMoraleStore implements MoraleStore {

    private final Map<UUID, MoraleTier> tiers = new HashMap<>();

    @Override
    public Optional<MoraleTier> findTier(UUID playerId) {
        return Optional.ofNullable(tiers.get(playerId));
    }

    @Override
    public void putTier(UUID playerId, MoraleTier tier) {
        if (tier == null) {
            tiers.remove(playerId);
            return;
        }
        tiers.put(playerId, tier);
    }

    @Override
    public Map<UUID, MoraleTier> allTiersView() {
        return Map.copyOf(tiers);
    }

    @Override
    public void replaceAll(Map<UUID, MoraleTier> loaded) {
        tiers.clear();
        if (loaded != null) {
            for (Map.Entry<UUID, MoraleTier> entry : loaded.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    tiers.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
