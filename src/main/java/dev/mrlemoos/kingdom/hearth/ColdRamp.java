package dev.mrlemoos.kingdom.hearth;

/**
 * How the cold bites, by degrees: the first days beyond a hearth's reach cost a villager part of
 * its yield, and by the {@link HearthConfig#coldStrikeDays()}th it downs tools altogether — the
 * same strike as any villager left unpaid, told the same way on its nametag.
 */
public final class ColdRamp {

    private ColdRamp() {}

    /** What a villager yields today for the run of cold days behind it; the whole of it when warm. */
    public static double yieldFactor(int coldDays, HearthConfig config) {
        return coldDays <= 0 ? 1.0 : config.coldYieldFactor();
    }

    /** Whether the run of cold days has reached the point of downing tools. */
    public static boolean strikes(int coldDays, HearthConfig config) {
        int threshold = config.coldStrikeDays();
        return threshold > 0 && coldDays >= threshold;
    }
}
