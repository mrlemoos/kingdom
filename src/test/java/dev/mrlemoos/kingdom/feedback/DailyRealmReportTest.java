package dev.mrlemoos.kingdom.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DailyRealmReportTest {

    @Test
    void readsTheDayOutWithASurplus() {
        DailyRealmReport report =
                new DailyRealmReport("Northmarch", 12.5, 3.2, 15.7, "Farmer", 8.4);
        assertEquals(
                "&6[Realm] &fNorthmarch&7: treasury &a+12.50&7, tax &e3.20&7, GDP &f15.70&7,"
                        + " top earner &fFarmer &7(8.40)",
                report.line());
    }

    @Test
    void aDeficitIsReadOutInRed() {
        DailyRealmReport report = new DailyRealmReport("Northmarch", -4.0, 0.0, 0.0, null, 0.0);
        assertEquals("&6[Realm] &fNorthmarch&7: treasury &c-4&7, tax &e0&7, GDP &f0", report.line());
    }

    @Test
    void wholeAmountsAreReadWithoutPence() {
        DailyRealmReport report = new DailyRealmReport("Northmarch", 12.0, 3.0, 15.0, "Cleric", 9.0);
        assertEquals(
                "&6[Realm] &fNorthmarch&7: treasury &a+12&7, tax &e3&7, GDP &f15&7,"
                        + " top earner &fCleric &7(9)",
                report.line());
    }

    @Test
    void theTreasuryDeltaIsTheDifferenceAcrossTheTick() {
        assertEquals(7.5, DailyRealmReport.delta(20.0, 12.5), 0.0001);
        assertEquals(-7.5, DailyRealmReport.delta(12.5, 20.0), 0.0001);
    }
}
