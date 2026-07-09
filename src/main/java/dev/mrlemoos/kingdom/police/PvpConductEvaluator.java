package dev.mrlemoos.kingdom.police;

import java.util.Objects;

/**
 * Stub for future war-combat / friendly-fire / siege-neutral gating.
 * Under open PvP this never cancels damage, even if the feature flag is on.
 */
public final class PvpConductEvaluator {

    private final PvpEnforcementConfig config;

    public PvpConductEvaluator(PvpEnforcementConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Always false while open PvP policy holds. Real gating lands when the policy lifts.
     */
    public boolean shouldCancelDamage(DamageFacts facts) {
        return false;
    }

    public boolean isDeferredUnderOpenPvp() {
        return true;
    }

    public PvpEnforcementConfig config() {
        return config;
    }
}
