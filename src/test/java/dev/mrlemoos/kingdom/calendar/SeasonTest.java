package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SeasonTest {

    @Test
    void everyMonthKeepsTheSeasonTheAdrGivesIt() {
        Map<RealmMonth, Season> expected = new EnumMap<>(RealmMonth.class);
        expected.put(RealmMonth.FROSTWANE, Season.SPRING);
        expected.put(RealmMonth.THAWTIDE, Season.SPRING);
        expected.put(RealmMonth.SEEDFALL, Season.SPRING);
        expected.put(RealmMonth.BLOSSOMING, Season.SUMMER);
        expected.put(RealmMonth.HIGHMEAD, Season.SUMMER);
        expected.put(RealmMonth.SUNWAKE, Season.SUMMER);
        expected.put(RealmMonth.HARVEST, Season.AUTUMN);
        expected.put(RealmMonth.GOLDFALL, Season.AUTUMN);
        expected.put(RealmMonth.EMBERWANE, Season.AUTUMN);
        expected.put(RealmMonth.HALLOWTIDE, Season.WINTER);
        expected.put(RealmMonth.LONGNIGHT, Season.WINTER);
        expected.put(RealmMonth.YULEWATCH, Season.WINTER);

        for (RealmMonth month : RealmMonth.values()) {
            assertEquals(expected.get(month), month.season(), month.displayName());
            assertSame(month.season(), Season.of(month));
        }
    }

    @Test
    void eachSeasonHoldsThreeWholeMonths() {
        Map<Season, Integer> counts = new EnumMap<>(Season.class);
        for (RealmMonth month : RealmMonth.values()) {
            counts.merge(month.season(), 1, Integer::sum);
        }
        for (Season season : Season.values()) {
            assertEquals(3, counts.get(season), season.displayName());
        }
    }

    @Test
    void eachSeasonKnowsTheMonthItOpensWith() {
        assertEquals(RealmMonth.FROSTWANE, Season.SPRING.firstMonth());
        assertEquals(RealmMonth.BLOSSOMING, Season.SUMMER.firstMonth());
        assertEquals(RealmMonth.HARVEST, Season.AUTUMN.firstMonth());
        assertEquals(RealmMonth.HALLOWTIDE, Season.WINTER.firstMonth());
    }

    @Test
    void everySeasonCarriesABannerShortEnoughForTheScreen() {
        for (Season season : Season.values()) {
            assertEquals(season.banner(), season.banner().trim(), season.displayName());
            org.junit.jupiter.api.Assertions.assertFalse(season.banner().isEmpty(), season.displayName());
            org.junit.jupiter.api.Assertions.assertTrue(
                    season.banner().length() <= Season.MAX_BANNER_CHARS, season.displayName());
        }
    }

    @Test
    void theSeasonOfADayFollowsItsMonth() {
        assertEquals(Season.SPRING, Season.ofDay(0L));
        assertEquals(Season.SUMMER, Season.ofDay(3L * RealmCalendar.DAYS_PER_MONTH));
        assertEquals(Season.AUTUMN, Season.ofDay(6L * RealmCalendar.DAYS_PER_MONTH));
        assertEquals(Season.WINTER, Season.ofDay(9L * RealmCalendar.DAYS_PER_MONTH));
        assertEquals(Season.SPRING, Season.ofDay(RealmCalendar.DAYS_PER_YEAR));
    }
}
