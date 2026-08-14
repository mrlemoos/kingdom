package dev.mrlemoos.kingdom.feedback;

import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionType;

/**
 * Legend and remaining fraction for the election boss bar (real-time, matching {@code endsAtMs}).
 */
public final class ElectionBarText {

    private ElectionBarText() {}

    public static String label(ElectionType type, ElectionPhase phase, long remainingMs) {
        String name = typeName(type);
        if (phase == ElectionPhase.AWAITING_SPEAKER_TIE) {
            return name + " — awaiting Speaker";
        }
        return name + " — " + remaining(remainingMs);
    }

    public static float progress(long remainingMs, long windowMs) {
        if (windowMs <= 0L) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float) remainingMs / (float) windowMs));
    }

    private static String typeName(ElectionType type) {
        if (type == null) {
            return "Election";
        }
        return switch (type) {
            case GENERAL -> "General election";
            case BY_ELECTION_PLAYER, BY_ELECTION_VILLAGER -> "By-election";
            case PREMIER -> "Premier election";
        };
    }

    private static String remaining(long remainingMs) {
        long ms = Math.max(0L, remainingMs);
        if (ms < 60_000L) {
            return (ms / 1000L) + "s";
        }
        long minutes = ms / 60_000L;
        if (minutes < 60L) {
            return minutes + "m";
        }
        long hours = minutes / 60L;
        long remMinutes = minutes % 60L;
        return hours + "h " + remMinutes + "m";
    }
}
