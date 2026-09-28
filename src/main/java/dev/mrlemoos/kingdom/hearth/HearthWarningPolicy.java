package dev.mrlemoos.kingdom.hearth;

/** A Town Crier warning changes only when a complete scan finds a new supply state. */
public final class HearthWarningPolicy {

    public record Change(String state, boolean changed) {}

    private HearthWarningPolicy() {}

    public static Change observe(String previous, boolean complete, int hearths, int lowHearths) {
        if (!complete) return new Change(previous, false);
        String current = hearths == 0 ? "no-hearth" : lowHearths > 0 ? "low-fuel" : "ready";
        return new Change(current, !current.equals(previous));
    }
}
