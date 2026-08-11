package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Real-time window for a trial jury to cast all votes before falling back to a realm-handled trial.
 */
public record TrialJuryConfig(long windowMs) {

    public static final long DEFAULT_WINDOW_MS = 5 * 60_000L;

    public static TrialJuryConfig defaults() {
        return new TrialJuryConfig(DEFAULT_WINDOW_MS);
    }

    public static TrialJuryConfig fromPluginConfig(FileConfiguration config) {
        long seconds = config.getLong("police.trial-jury.window-seconds", DEFAULT_WINDOW_MS / 1000L);
        return new TrialJuryConfig(Math.max(1L, seconds) * 1000L);
    }
}
