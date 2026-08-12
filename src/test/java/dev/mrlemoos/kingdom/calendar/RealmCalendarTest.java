package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RealmCalendarTest {

    @Test
    void firstRealmDayIsTheFirstOfFrostwaneInRealmYearOne() {
        RealmDate date = RealmCalendar.dateOf(0L);
        assertEquals(1, date.realmYear());
        assertEquals(RealmMonth.FROSTWANE, date.month());
        assertEquals(1, date.dayOfMonth());
    }

    @Test
    void aMonthIsThirtyDaysLong() {
        assertEquals(RealmMonth.FROSTWANE, RealmCalendar.dateOf(29L).month());
        assertEquals(30, RealmCalendar.dateOf(29L).dayOfMonth());
        assertEquals(RealmMonth.THAWTIDE, RealmCalendar.dateOf(30L).month());
        assertEquals(1, RealmCalendar.dateOf(30L).dayOfMonth());
    }

    @Test
    void aYearIsTwelveMonths() {
        assertEquals(12, RealmMonth.values().length);
        RealmDate lastDay = RealmCalendar.dateOf(359L);
        assertEquals(1, lastDay.realmYear());
        assertEquals(RealmMonth.YULEWATCH, lastDay.month());
        assertEquals(30, lastDay.dayOfMonth());

        RealmDate newYear = RealmCalendar.dateOf(360L);
        assertEquals(2, newYear.realmYear());
        assertEquals(RealmMonth.FROSTWANE, newYear.month());
        assertEquals(1, newYear.dayOfMonth());
    }

    @Test
    void rejectsRealmDaysBeforeTheEpoch() {
        assertThrows(IllegalArgumentException.class, () -> RealmCalendar.dateOf(-1L));
    }

    @Test
    void newYearIsOnlyTheFirstOfFrostwane() {
        assertEquals(true, RealmCalendar.dateOf(720L).isNewYear());
        assertEquals(false, RealmCalendar.dateOf(721L).isNewYear());
    }

    @Test
    void monthStartIsTheFirstOfAnyMonth() {
        assertEquals(true, RealmCalendar.dateOf(30L).isMonthStart());
        assertEquals(false, RealmCalendar.dateOf(31L).isMonthStart());
    }

    @Test
    void formatsTheDayWithAnOrdinalSuffix() {
        assertEquals("1st of Frostwane", RealmCalendar.dateOf(0L).format());
        assertEquals("2nd of Frostwane", RealmCalendar.dateOf(1L).format());
        assertEquals("3rd of Frostwane", RealmCalendar.dateOf(2L).format());
        assertEquals("4th of Frostwane", RealmCalendar.dateOf(3L).format());
        assertEquals("11th of Frostwane", RealmCalendar.dateOf(10L).format());
        assertEquals("12th of Frostwane", RealmCalendar.dateOf(11L).format());
        assertEquals("13th of Frostwane", RealmCalendar.dateOf(12L).format());
        assertEquals("21st of Frostwane", RealmCalendar.dateOf(20L).format());
        assertEquals("22nd of Frostwane", RealmCalendar.dateOf(21L).format());
        assertEquals("23rd of Frostwane", RealmCalendar.dateOf(22L).format());
        assertEquals("30th of Frostwane", RealmCalendar.dateOf(29L).format());
    }

    @Test
    void namesMonthsInRealmOrder() {
        assertEquals("Harvest", RealmMonth.values()[6].displayName());
        assertEquals("Yulewatch", RealmMonth.YULEWATCH.displayName());
    }
}
