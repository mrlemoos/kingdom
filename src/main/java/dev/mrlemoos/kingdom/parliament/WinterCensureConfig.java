package dev.mrlemoos.kingdom.parliament;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * What a Premier answers for when they war or dissolve in winter: how far their political standing
 * falls, counted in steps down the loyalty ladder.
 *
 * <p>Never a motion of no confidence: the House alone tables that, and two seated Members must
 * choose it between them.
 */
public record WinterCensureConfig(boolean enabled, int tierSteps) {

    /** One step down the ladder — Faithful to Doubtful — for an ill-timed winter decision. */
    private static final int DEFAULT_TIER_STEPS = 1;

    public WinterCensureConfig {
        tierSteps = Math.max(0, tierSteps);
    }

    public static WinterCensureConfig defaults() {
        return new WinterCensureConfig(true, DEFAULT_TIER_STEPS);
    }

    public static WinterCensureConfig fromPluginConfig(FileConfiguration config) {
        return new WinterCensureConfig(
                config.getBoolean("parliament.winter-censure.enabled", true),
                config.getInt("parliament.winter-censure.tier-steps", DEFAULT_TIER_STEPS));
    }
}
