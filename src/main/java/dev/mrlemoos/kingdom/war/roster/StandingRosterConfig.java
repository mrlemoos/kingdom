package dev.mrlemoos.kingdom.war.roster;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Standing roster cap configuration. Defaults to 8 permanent-core members per kingdom.
 */
public record StandingRosterConfig(int rosterCap) {

    public static final int DEFAULT_ROSTER_CAP = 8;

    public StandingRosterConfig {
        if (rosterCap < 0) {
            throw new IllegalArgumentException("rosterCap must not be negative");
        }
    }

    public static StandingRosterConfig defaults() {
        return new StandingRosterConfig(DEFAULT_ROSTER_CAP);
    }

    public static StandingRosterConfig fromPluginConfig(FileConfiguration config) {
        return new StandingRosterConfig(config.getInt("war.roster.cap", DEFAULT_ROSTER_CAP));
    }
}
