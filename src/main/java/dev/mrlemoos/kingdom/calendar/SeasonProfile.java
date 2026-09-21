package dev.mrlemoos.kingdom.calendar;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * What a season asks of the realm and grants it, held as one set of figures the whole plugin reads: how fast
 * crops come on, what the fields and workshops yield, what the levy costs to keep, how thickly the hostile dark
 * spawns, whether hearths must burn, how slowly a soldier's morale mends — and how fast it decays in the field —
 * and how readily a clearing sky is turned back.
 *
 * <p>Tunable under the {@code season.<name>.*} key space; the seasons themselves are not.
 */
public record SeasonProfile(
        double cropGrowthFactor,
        double outdoorYieldFactor,
        double indoorYieldFactor,
        double standingLevyUpkeepFactor,
        double musteredLevyUpkeepFactor,
        double hostileSpawnFactor,
        boolean hearthsRequired,
        double moraleRecoveryFactor,
        int siegeMoraleDecayDays,
        double stormChance) {

    /** Spring is the neutral yardstick; summer and autumn favour the realm, winter presses it. */
    public static SeasonProfile defaults(Season season) {
        return switch (season) {
            case SPRING -> new SeasonProfile(1.0, 1.0, 1.0, 1.0, 1.5, 1.0, false, 1.0, 0, 0.1);
            case SUMMER -> new SeasonProfile(1.25, 1.25, 1.0, 0.75, 1.25, 0.9, false, 1.25, 0, 0.0);
            case AUTUMN -> new SeasonProfile(1.15, 1.25, 1.0, 1.0, 1.5, 1.0, false, 1.0, 0, 0.2);
            case WINTER -> new SeasonProfile(0.5, 0.5, 1.0, 1.5, 2.0, 1.35, true, 0.5, 3, 0.7);
        };
    }

    public static SeasonProfile fromPluginConfig(FileConfiguration config, Season season) {
        SeasonProfile fallback = defaults(season);
        String path = "season." + season.name().toLowerCase() + ".";
        return new SeasonProfile(
                config.getDouble(path + "crop-growth-factor", fallback.cropGrowthFactor()),
                config.getDouble(path + "outdoor-yield-factor", fallback.outdoorYieldFactor()),
                config.getDouble(path + "indoor-yield-factor", fallback.indoorYieldFactor()),
                config.getDouble(path + "standing-levy-upkeep-factor", fallback.standingLevyUpkeepFactor()),
                config.getDouble(path + "mustered-levy-upkeep-factor", fallback.musteredLevyUpkeepFactor()),
                config.getDouble(path + "hostile-spawn-factor", fallback.hostileSpawnFactor()),
                config.getBoolean(path + "hearths-required", fallback.hearthsRequired()),
                config.getDouble(path + "morale-recovery-factor", fallback.moraleRecoveryFactor()),
                config.getInt(path + "siege-morale-decay-days", fallback.siegeMoraleDecayDays()),
                config.getDouble(path + "storm-chance", fallback.stormChance()));
    }
}
