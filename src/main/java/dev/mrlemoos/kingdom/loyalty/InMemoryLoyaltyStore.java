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

    private final Map<UUID, RecoveryMark<LoyaltyTier>> marks = new HashMap<>();

    @Override
    public Optional<RecoveryMark<LoyaltyTier>> findMark(UUID playerId) {
        return Optional.ofNullable(marks.get(playerId));
    }

    @Override
    public void putMark(UUID playerId, RecoveryMark<LoyaltyTier> mark) {
        if (mark == null || mark.tier() == null) {
            marks.remove(playerId);
            return;
        }
        marks.put(playerId, mark);
    }

    @Override
    public void clearMark(UUID playerId) {
        marks.remove(playerId);
    }

    @Override
    public Map<UUID, RecoveryMark<LoyaltyTier>> allMarksView() {
        return Map.copyOf(marks);
    }

    @Override
    public void replaceAllMarks(Map<UUID, RecoveryMark<LoyaltyTier>> loaded) {
        marks.clear();
        if (loaded != null) {
            for (Map.Entry<UUID, RecoveryMark<LoyaltyTier>> entry : loaded.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue().tier() != null) {
                    marks.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
