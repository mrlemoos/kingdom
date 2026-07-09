package dev.mrlemoos.kingdom.war.capture;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Chunk-capture feature flag and consecutive-tick flip threshold for {@link
 * ChunkCaptureService}. Deliberately reads the shared {@code war.siege} config block rather than
 * a separate {@code war.capture} namespace, so it lines up with Slice 6.1's siege zone flag
 * ({@code war.siege.enabled}); the two slices may later consolidate into a single {@code
 * SiegeConfig} without a config migration.
 */
public record CaptureConfig(boolean enabled, int flipThresholdTicks) {

    public static final int DEFAULT_FLIP_THRESHOLD_TICKS = 3;

    public CaptureConfig {
        if (flipThresholdTicks <= 0) {
            throw new IllegalArgumentException("flipThresholdTicks must be positive");
        }
    }

    public static CaptureConfig on() {
        return new CaptureConfig(true, DEFAULT_FLIP_THRESHOLD_TICKS);
    }

    public static CaptureConfig off() {
        return new CaptureConfig(false, DEFAULT_FLIP_THRESHOLD_TICKS);
    }

    public static CaptureConfig fromPluginConfig(FileConfiguration config) {
        CaptureConfig defaults = on();
        return new CaptureConfig(
                config.getBoolean("war.siege.enabled", defaults.enabled()),
                config.getInt("war.siege.flip-threshold-ticks", defaults.flipThresholdTicks()));
    }
}
