package dev.mrlemoos.kingdom.economy.villager;

import java.util.Optional;

/**
 * Domain helper for the villager-strike pressure state on a frozen wallet.
 * Strike threshold must be strictly shorter than escheatment.
 */
public final class VillagerStrike {

    public static final String NAMETAG = "[on strike]";

    private VillagerStrike() {}

    public static boolean isOnStrike(
            Optional<Long> frozenSinceEpochDay, long currentEpochDay, int strikeMcDays, int escheatMcDays) {
        requireStrikeShorterThanEscheat(strikeMcDays, escheatMcDays);
        if (frozenSinceEpochDay.isEmpty() || strikeMcDays <= 0) {
            return false;
        }
        long frozenSince = frozenSinceEpochDay.get();
        return currentEpochDay - frozenSince >= strikeMcDays;
    }

    public static void requireStrikeShorterThanEscheat(int strikeMcDays, int escheatMcDays) {
        if (strikeMcDays > 0 && strikeMcDays >= escheatMcDays) {
            throw new IllegalArgumentException(
                    "Frozen-wallet strike threshold must be shorter than escheatment days.");
        }
    }
}
