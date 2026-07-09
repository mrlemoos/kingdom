package dev.mrlemoos.kingdom.war.desertion;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for {@link TreasonReviewFlag}s. Slice 4.2 keeps a single latest flag per
 * player; a future slice may route this to the police warrant pipeline and/or persist it
 * alongside political loyalty and military morale state.
 */
public interface TreasonReviewStore {

    void raise(TreasonReviewFlag flag);

    boolean isFlagged(UUID playerId);

    Optional<TreasonReviewFlag> findFlag(UUID playerId);

    Collection<TreasonReviewFlag> allFlagsView();

    /** Clears a player's flag, e.g. once the court/warrant pipeline has resolved the review. */
    void clear(UUID playerId);
}
