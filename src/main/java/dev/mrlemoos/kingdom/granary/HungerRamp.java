package dev.mrlemoos.kingdom.granary;

/**
 * How hunger bites, by degrees: a day the realm's ration could not be drawn costs a villager part of
 * its yield, by the {@link GranaryConfig#hungerStrikeDays()}th it downs tools as any unpaid villager
 * does, and from the {@link GranaryConfig#hungerStarveDays()}th it stands in the lot to be taken.
 *
 * <p>Deliberately the same shape as {@link dev.mrlemoos.kingdom.hearth.ColdRamp}, and deliberately a
 * ledger of its own: cold and hunger bite at once, and only hunger kills.
 */
public final class HungerRamp {

    /** What a starving villager is called on its nametag, shown over the strike it also stands in. */
    public static final String NAMETAG = "[starving]";

    private HungerRamp() {}

    /** What a villager yields today for the run of hungry days behind it; the whole of it when fed. */
    public static double yieldFactor(int hungryDays, GranaryConfig config) {
        int threshold = config.hungerYieldDays();
        return threshold > 0 && hungryDays >= threshold ? config.hungerYieldFactor() : 1.0;
    }

    /** Whether the run of hungry days has reached the point of downing tools. */
    public static boolean strikes(int hungryDays, GranaryConfig config) {
        int threshold = config.hungerStrikeDays();
        return threshold > 0 && hungryDays >= threshold;
    }

    /** Whether the villager has gone hungry long enough to stand in the lot. */
    public static boolean starves(int hungryDays, GranaryConfig config) {
        int threshold = config.hungerStarveDays();
        return threshold > 0 && hungryDays >= threshold;
    }
}
