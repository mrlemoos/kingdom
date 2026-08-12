package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;

/**
 * Named curfew windows offered when the Crown signs a decree. Presets only — no free-form ticks.
 */
public final class CurfewPresets {

    private CurfewPresets() {}

    /** Dusk to dawn: 13000–23000. */
    public static CurfewEnforcementConfig duskDawn() {
        return CurfewEnforcementConfig.enabled(13_000L, 23_000L);
    }

    /** Nightfall to midnight: 13000–18000. */
    public static CurfewEnforcementConfig nightfallMidnight() {
        return CurfewEnforcementConfig.enabled(13_000L, 18_000L);
    }

    /** A decree that lifts curfew; enforcement stays off until another decree sets one. */
    public static CurfewEnforcementConfig lifted() {
        return CurfewEnforcementConfig.disabled(13_000L, 23_000L);
    }
}
