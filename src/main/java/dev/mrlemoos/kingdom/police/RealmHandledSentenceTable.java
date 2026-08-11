package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.SentenceType;
import java.util.Objects;
import java.util.Random;

/**
 * Weighted random sentence table used by the villager judge (realm-handled trial) and by a guilty
 * trial-jury verdict.
 */
public final class RealmHandledSentenceTable {

    public record DrawnSentence(SentenceType type, double fineAmount, int prisonMinutes) {}

    private RealmHandledSentenceTable() {}

    /**
     * Weights: acquittal 20, warning 25, fine 30, prison 25 (out of 100).
     */
    public static DrawnSentence draw(Random random) {
        Objects.requireNonNull(random, "random");
        int roll = random.nextInt(100);
        if (roll < 20) {
            return new DrawnSentence(SentenceType.ACQUITTAL, 0, 0);
        }
        if (roll < 45) {
            return new DrawnSentence(SentenceType.WARNING, 0, 0);
        }
        if (roll < 75) {
            return new DrawnSentence(SentenceType.FINE, 10.0, 0);
        }
        return new DrawnSentence(SentenceType.PRISON, 0, 15);
    }
}
