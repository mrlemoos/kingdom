package dev.mrlemoos.kingdom.raid;

/** Pure balancing rules for a city hall raid. */
public final class BanditRaidRules {
    private BanditRaidRules() {}

    public static int banditCount(int nearbyMembers) {
        return nearbyMembers <= 0 ? 0 : Math.min(8, nearbyMembers + 2);
    }

    public static double loss(double treasury) {
        return Math.min(Math.max(0.0, treasury) * 0.05, 50.0);
    }

    public static boolean warningMatchesDusk(String warnedSeason, long warnedDay, String season, long day, long worldTime) {
        return warnedSeason != null && warnedSeason.equals(season) && warnedDay == day
                && worldTime >= 12_000L && worldTime < 13_000L;
    }
}
