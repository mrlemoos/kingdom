package dev.mrlemoos.kingdom.model.church;

/**
 * Experience held against a member's funeral. One to a player: a later death overwrites the
 * earlier record rather than stacking with it.
 */
public record FuneralRecord(int heldExperience, long diedOnDay) {

    public FuneralRecord {
        heldExperience = Math.max(0, heldExperience);
    }

    /** True once the record has sat unclaimed for the whole window. */
    public boolean isExpired(long today, int windowDays) {
        return today - diedOnDay >= windowDays;
    }
}
