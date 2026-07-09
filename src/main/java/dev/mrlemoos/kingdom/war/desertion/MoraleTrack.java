package dev.mrlemoos.kingdom.war.desertion;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;

/**
 * Small military-morale-track port that {@link DesertionEvaluator} depends on, so desertion logic
 * does not couple to a concrete morale service implementation. Slice 4.1's persisted MoraleService
 * should implement this (or sit behind an adapter that does) once merged; until then
 * {@code dev.mrlemoos.kingdom.loyalty.MoraleService} is Slice 4.2's minimal stand-in.
 */
public interface MoraleTrack {

    /** Current morale tier for the player, defaulting to Steadfast if never tracked. */
    MoraleTier tierOf(UUID playerId);

    /** Opens the track at Steadfast if not already tracked. No-op if already opened. */
    void openTrack(UUID playerId);

    /**
     * Drops the player's morale towards {@code tier} — but only if {@code tier} is worse (further
     * down the Steadfast&lt;Shaken&lt;Breaking&lt;Rout ladder) than their current tier. Never
     * improves morale.
     */
    void dropTo(UUID playerId, MoraleTier tier);

    /**
     * Applies the military-track breach table for {@code kind} (see {@link MoraleBreachKind}),
     * honouring the standing force's stricter {@code hardenedService} siege-absence rule where it
     * applies, and returns the resulting tier. Never sets Traitor — this is the military track
     * only.
     */
    MoraleTier applyBreach(UUID playerId, MoraleBreachKind kind, boolean hardenedService);
}
