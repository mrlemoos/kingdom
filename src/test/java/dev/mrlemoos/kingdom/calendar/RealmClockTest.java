package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RealmClockTest {

    @Test
    void countsRealmDaysFromTheStoredEpoch() {
        RealmClock clock = new RealmClock(1000L, 0L);
        assertEquals(0L, clock.advanceTo(1000L));
        assertEquals(5L, clock.advanceTo(1005L));
    }

    @Test
    void holdsSteadyWhenTheWorldClockRunsBackwards() {
        RealmClock clock = new RealmClock(1000L, 0L);
        assertEquals(40L, clock.advanceTo(1040L));
        assertEquals(40L, clock.advanceTo(1002L));
        assertEquals(40L, clock.advanceTo(1039L));
        assertEquals(41L, clock.advanceTo(1041L));
    }

    @Test
    void resumesFromARestoredLastSeenDay() {
        RealmClock clock = new RealmClock(1000L, 40L);
        assertEquals(40L, clock.advanceTo(1002L));
    }

    @Test
    void treatsWorldDaysBeforeTheEpochAsTheEpoch() {
        RealmClock clock = new RealmClock(1000L, 0L);
        assertEquals(0L, clock.advanceTo(900L));
    }

    @Test
    void reportsTheCurrentDayWithoutAdvancing() {
        RealmClock clock = new RealmClock(1000L, 7L);
        assertEquals(7L, clock.currentRealmDay());
    }
}
