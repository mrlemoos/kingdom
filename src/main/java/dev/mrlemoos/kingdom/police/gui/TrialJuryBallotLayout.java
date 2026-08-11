package dev.mrlemoos.kingdom.police.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Pure layout helpers for the trial-jury ballot (no Bukkit). Secret: accused name + time left only.
 */
public final class TrialJuryBallotLayout {

    public static final int SLOT_GUILTY = 11;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_NOT_GUILTY = 15;

    private TrialJuryBallotLayout() {}

    public static TrialJuryBallotAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_GUILTY -> TrialJuryBallotAction.GUILTY;
            case SLOT_NOT_GUILTY -> TrialJuryBallotAction.NOT_GUILTY;
            default -> null;
        };
    }

    public static List<String> infoLore(String accusedName, long remainingMs) {
        List<String> lore = new ArrayList<>();
        lore.add("Accused: " + accusedName);
        lore.add("Time left: " + formatRemaining(remainingMs));
        lore.add("Your vote is secret.");
        lore.add("Reopen with /kingdom police jury");
        return lore;
    }

    public static String formatRemaining(long remainingMs) {
        long clamped = Math.max(0L, remainingMs);
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(clamped);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
