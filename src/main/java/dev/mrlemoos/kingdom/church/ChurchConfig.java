package dev.mrlemoos.kingdom.church;

/**
 * The dials on the church: how long a blessing lasts, how long the dead wait on their rites, and
 * what share of them the treasury and the priest take.
 */
public record ChurchConfig(
        int blessingSeconds,
        int blessingCooldownDays,
        int funeralWindowDays,
        double funeralExperienceShare,
        double titheShare) {

    public ChurchConfig() {
        this(120, 1, 3, 0.5d, 0.1d);
    }

    public ChurchConfig {
        blessingSeconds = Math.max(1, blessingSeconds);
        blessingCooldownDays = Math.max(1, blessingCooldownDays);
        funeralWindowDays = Math.max(1, funeralWindowDays);
        funeralExperienceShare = clampShare(funeralExperienceShare);
        titheShare = clampShare(titheShare);
    }

    private static double clampShare(double share) {
        if (share < 0.0d) {
            return 0.0d;
        }
        return Math.min(share, 1.0d);
    }
}
