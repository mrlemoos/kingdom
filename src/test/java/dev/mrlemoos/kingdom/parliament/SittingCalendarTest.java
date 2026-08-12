package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SittingCalendarTest {

    @Test
    void evenRealmDayIsSittingDay() {
        assertTrue(SittingCalendar.isSittingDay(0L));
        assertTrue(SittingCalendar.isSittingDay(2L));
        assertTrue(SittingCalendar.isSittingDay(100L));
    }

    @Test
    void oddRealmDayIsRecess() {
        assertTrue(SittingCalendar.isRecessDay(1L));
        assertTrue(SittingCalendar.isRecessDay(3L));
        assertFalse(SittingCalendar.isSittingDay(1L));
    }

    @Test
    void prorogationBlocksSittingDays() {
        assertFalse(SittingCalendar.allowsDivision(0L, true));
        assertFalse(SittingCalendar.allowsDivision(2L, true));
        assertTrue(SittingCalendar.allowsDivision(0L, false));
        assertFalse(SittingCalendar.allowsDivision(1L, false));
    }

    @Test
    void villagerMpsWorkEveryDayWhileProrogued() {
        assertTrue(SittingCalendar.villagerMpsAtProfession(0L, true));
        assertTrue(SittingCalendar.villagerMpsAtProfession(1L, true));
        assertFalse(SittingCalendar.villagerMpsAtProfession(0L, false));
        assertTrue(SittingCalendar.villagerMpsAtProfession(1L, false));
    }
}
