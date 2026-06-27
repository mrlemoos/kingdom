package dev.leo.kingdom.election;

public record ElectionConfig(
        int generalIntervalMcDays,
        int durationMcDays,
        int inauguralFiscalDelayMcDays,
        int maxPlayerSeats,
        int totalSeats,
        long msPerMcDay) {

    public static final long DEFAULT_MS_PER_MC_DAY = 1_200_000L;

    public static ElectionConfig defaults() {
        return new ElectionConfig(120, 3, 2, 4, 8, DEFAULT_MS_PER_MC_DAY);
    }

    public long durationMs() {
        return durationMcDays * msPerMcDay;
    }

    public static ElectionConfig fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        ElectionConfig defaults = defaults();
        return new ElectionConfig(
                config.getInt("election.general-interval-mc-days", defaults.generalIntervalMcDays()),
                config.getInt("election.duration-mc-days", defaults.durationMcDays()),
                config.getInt("election.inaugural-fiscal-delay-mc-days", defaults.inauguralFiscalDelayMcDays()),
                config.getInt("election.max-player-seats", defaults.maxPlayerSeats()),
                config.getInt("election.total-seats", defaults.totalSeats()),
                config.getLong("election.ms-per-mc-day", defaults.msPerMcDay()));
    }
}
