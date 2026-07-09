package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Curfew window in Minecraft day ticks (0–23999).
 * When start &gt; end the window wraps midnight.
 */
public record CurfewEnforcementConfig(boolean enabled, long windowStartTick, long windowEndTick) {

    public static CurfewEnforcementConfig enabled(long startTick, long endTick) {
        return new CurfewEnforcementConfig(true, startTick, endTick);
    }

    public static CurfewEnforcementConfig disabled(long startTick, long endTick) {
        return new CurfewEnforcementConfig(false, startTick, endTick);
    }

    public static CurfewEnforcementConfig fromPluginConfig(FileConfiguration config) {
        return new CurfewEnforcementConfig(
                config.getBoolean("enforcement.curfew.enabled", false),
                config.getLong("enforcement.curfew.window-start-tick", 13_000L),
                config.getLong("enforcement.curfew.window-end-tick", 23_000L));
    }

    public boolean isInsideWindow(long worldTimeTick) {
        long tick = Math.floorMod(worldTimeTick, 24_000L);
        if (windowStartTick <= windowEndTick) {
            return tick >= windowStartTick && tick <= windowEndTick;
        }
        // Wraps midnight: inside if tick >= start OR tick <= end.
        return tick >= windowStartTick || tick <= windowEndTick;
    }
}
