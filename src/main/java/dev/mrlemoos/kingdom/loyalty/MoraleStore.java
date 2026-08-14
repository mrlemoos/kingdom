package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the military morale track. Unlike {@link LoyaltyStore}, the absence of an
 * entry means the track is closed (never opened by oath of service or a siege hostile action) —
 * there is no implicit default tier such as political's Faithful.
 */
public interface MoraleStore {

    Optional<MoraleTier> findTier(UUID playerId);

    void putTier(UUID playerId, MoraleTier tier);

    Map<UUID, MoraleTier> allTiersView();

    void replaceAll(Map<UUID, MoraleTier> tiers);

    /** The persisted recovery clock for this subject, absent when no wait is running. */
    Optional<RecoveryMark<MoraleTier>> findMark(UUID playerId);

    void putMark(UUID playerId, RecoveryMark<MoraleTier> mark);

    void clearMark(UUID playerId);

    Map<UUID, RecoveryMark<MoraleTier>> allMarksView();

    void replaceAllMarks(Map<UUID, RecoveryMark<MoraleTier>> marks);
}
