package dev.mrlemoos.kingdom.war.levy;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * What the levy costs to keep, before the season moves it, and how far a soldier's morale may sink
 * before he deserts for want of pay.
 */
public record LevyUpkeepConfig(
        boolean enabled, double standingPerHead, double musteredPerHead, MoraleTier desertionFloorTier) {

    // A day's keep for a man of the standing roster, in Corona, before the season's factor.
    private static final double DEFAULT_STANDING_PER_HEAD = 2.0;

    // A mustered levyman is dearer: he is taken off his own work to serve.
    private static final double DEFAULT_MUSTERED_PER_HEAD = 5.0;

    /** The bottom of the morale ladder: a routed soldier deserts. */
    private static final MoraleTier DEFAULT_DESERTION_FLOOR = MoraleTier.ROUT;

    public static LevyUpkeepConfig defaults() {
        return new LevyUpkeepConfig(
                true, DEFAULT_STANDING_PER_HEAD, DEFAULT_MUSTERED_PER_HEAD, DEFAULT_DESERTION_FLOOR);
    }

    public static LevyUpkeepConfig fromPluginConfig(FileConfiguration config) {
        return new LevyUpkeepConfig(
                config.getBoolean("war.levy.upkeep.enabled", true),
                config.getDouble("war.levy.upkeep.standing-per-head", DEFAULT_STANDING_PER_HEAD),
                config.getDouble("war.levy.upkeep.mustered-per-head", DEFAULT_MUSTERED_PER_HEAD),
                desertionFloor(config.getString("war.levy.upkeep.desertion-floor-tier")));
    }

    private static MoraleTier desertionFloor(String configured) {
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DESERTION_FLOOR;
        }
        try {
            return MoraleTier.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DEFAULT_DESERTION_FLOOR;
        }
    }
}
