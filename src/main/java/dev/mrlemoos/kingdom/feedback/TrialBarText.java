package dev.mrlemoos.kingdom.feedback;

/**
 * Legend and remaining fraction for the trial boss bar (real-time seconds, not Minecraft days).
 */
public final class TrialBarText {

    private TrialBarText() {}

    public static String label(String accusedName, long remainingSeconds) {
        String name = accusedName == null || accusedName.isBlank() ? "the accused" : accusedName;
        long secs = Math.max(0L, remainingSeconds);
        return "Trial of " + name + " — " + secs + "s";
    }

    public static float progress(long remainingMs, long windowMs) {
        if (windowMs <= 0L) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float) remainingMs / (float) windowMs));
    }
}
