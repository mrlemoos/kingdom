package dev.mrlemoos.kingdom.calendar;

/**
 * Turns the world clock into realm days, counted from a stored epoch.
 *
 * <p>An operator may {@code /time set} and a world restore may move the world clock backwards; the realm day never
 * follows it down, so records keep their order.
 */
public final class RealmClock {

    private final long epochWorldDay;
    private long lastSeenRealmDay;

    public RealmClock(long epochWorldDay, long lastSeenRealmDay) {
        this.epochWorldDay = epochWorldDay;
        this.lastSeenRealmDay = Math.max(0L, lastSeenRealmDay);
    }

    public long epochWorldDay() {
        return epochWorldDay;
    }

    public long currentRealmDay() {
        return lastSeenRealmDay;
    }

    /** Advances the realm day to match the given world day, never backwards. */
    public long advanceTo(long worldDay) {
        long candidate = Math.max(0L, worldDay - epochWorldDay);
        lastSeenRealmDay = Math.max(lastSeenRealmDay, candidate);
        return lastSeenRealmDay;
    }
}
