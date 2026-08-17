package dev.mrlemoos.kingdom.granary;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * What the granary asks of config: the wheat a farmer brings in over a day before the season is
 * reckoned in, how much wheat goes to a bale of hay, how many villagers one bale feeds through a
 * winter day, and how hard hunger presses the villagers a ration never reached.
 *
 * <p>Capacity is deliberately absent — a granary holds whatever air the builders left it.
 *
 * <p>Tunable under the {@code granary.*} key space.
 *
 * @param hungerYieldFactor what a hungry villager yields, as a share of a fed one's
 * @param hungerYieldDays   hungry days before the yield is cut
 * @param hungerStrikeDays  hungry days before the villager downs tools
 * @param hungerStarveDays  hungry days before the villager may be taken by the lot
 * @param famineTierSteps   steps a subject's political loyalty falls while the realm starves
 */
public record GranaryConfig(
        double wheatPerFarmerDay,
        int wheatPerBale,
        int headsPerHay,
        double hungerYieldFactor,
        int hungerYieldDays,
        int hungerStrikeDays,
        int hungerStarveDays,
        int famineTierSteps) {

    /** Half a day's work out of a hungry villager, as the cold takes half out of a frozen one. */
    private static final double DEFAULT_HUNGER_YIELD_FACTOR = 0.5;

    private static final int DEFAULT_HUNGER_YIELD_DAYS = 1;
    private static final int DEFAULT_HUNGER_STRIKE_DAYS = 3;
    private static final int DEFAULT_HUNGER_STARVE_DAYS = 7;

    /** One step down the ladder — Faithful to Doubtful — for leaving the realm to starve. */
    private static final int DEFAULT_FAMINE_TIER_STEPS = 1;

    public GranaryConfig {
        wheatPerFarmerDay = Math.max(0.0, wheatPerFarmerDay);
        wheatPerBale = Math.max(1, wheatPerBale);
        headsPerHay = Math.max(1, headsPerHay);
        hungerYieldFactor = Math.max(0.0, hungerYieldFactor);
        hungerYieldDays = Math.max(0, hungerYieldDays);
        hungerStrikeDays = Math.max(0, hungerStrikeDays);
        hungerStarveDays = Math.max(0, hungerStarveDays);
        famineTierSteps = Math.max(0, famineTierSteps);
    }

    /** The grain figures alone, with the hunger ramp left at the defaults. */
    public GranaryConfig(double wheatPerFarmerDay, int wheatPerBale, int headsPerHay) {
        this(
                wheatPerFarmerDay,
                wheatPerBale,
                headsPerHay,
                DEFAULT_HUNGER_YIELD_FACTOR,
                DEFAULT_HUNGER_YIELD_DAYS,
                DEFAULT_HUNGER_STRIKE_DAYS,
                DEFAULT_HUNGER_STARVE_DAYS,
                DEFAULT_FAMINE_TIER_STEPS);
    }

    /**
     * Three wheat to the farmer a day and nine wheat to the bale: three farmers fill a bale. Four
     * heads to the bale through winter, so those same three farmers feed a hamlet of twelve. Hunger
     * halves a villager's yield from the first unfed day, downs tools on the third, and from the
     * seventh puts it in the lot.
     */
    public static GranaryConfig defaults() {
        return new GranaryConfig(3.0, 9, 4);
    }

    public static GranaryConfig fromPluginConfig(FileConfiguration config) {
        GranaryConfig defaults = defaults();
        if (config == null) {
            return defaults;
        }
        ConfigurationSection granary = config.getConfigurationSection("granary");
        if (granary == null) {
            return defaults;
        }
        return new GranaryConfig(
                granary.getDouble("wheat-per-farmer-day", defaults.wheatPerFarmerDay()),
                granary.getInt("wheat-per-bale", defaults.wheatPerBale()),
                granary.getInt("heads-per-hay", defaults.headsPerHay()),
                granary.getDouble("hunger-yield-factor", defaults.hungerYieldFactor()),
                granary.getInt("hunger-yield-days", defaults.hungerYieldDays()),
                granary.getInt("hunger-strike-days", defaults.hungerStrikeDays()),
                granary.getInt("hunger-starve-days", defaults.hungerStarveDays()),
                granary.getInt("famine-tier-steps", defaults.famineTierSteps()));
    }
}
