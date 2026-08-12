package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlmanacBookTest {

    private static final PollingDay HARVEST_FIRST = new PollingDay(RealmMonth.HARVEST, 1);

    @Test
    void opensWithTheRealmDateOfTheDayItWasDrawnUp() {
        List<String> pages = AlmanacBook.pages("Avalon", 100L, List.of(), HARVEST_FIRST);
        assertTrue(pages.get(0).contains("Avalon"));
        assertTrue(pages.get(0).contains("11th of Blossoming"), pages.get(0));
    }

    @Test
    void listsAllTwelveMonthsInOrder() {
        String months = String.join("\n", AlmanacBook.pages("Avalon", 0L, List.of(), HARVEST_FIRST));
        for (RealmMonth month : RealmMonth.values()) {
            assertTrue(months.contains(month.displayName()), month.displayName());
        }
        assertTrue(months.indexOf("Frostwane") < months.indexOf("Yulewatch"));
    }

    @Test
    void namesTheNextPollingDay() {
        String almanac = String.join("\n", AlmanacBook.pages("Avalon", 0L, List.of(), HARVEST_FIRST));
        assertTrue(almanac.contains("Next polling day"), almanac);
        assertTrue(almanac.contains("1st of Harvest"), almanac);
    }

    @Test
    void rollsTheNextPollingDayIntoTheFollowingYearOncePast() {
        String almanac = String.join(
                "\n", AlmanacBook.pages("Avalon", 6L * 30L + 5L, List.of(), HARVEST_FIRST));
        assertTrue(almanac.contains("Realm Year 2"), almanac);
    }

    @Test
    void recordsTheRollOfMonarchs() {
        List<ReignRecord> history = List.of(
                new ReignRecord("id-leo", "Leo", "King", 1, 0L, 300L),
                new ReignRecord("id-leo-2", "Leo", "King", 2, 300L, ReignRecord.OPEN));
        String almanac = String.join("\n", AlmanacBook.pages("Avalon", 400L, history, HARVEST_FIRST));
        assertTrue(almanac.contains("Roll of monarchs"), almanac);
        assertTrue(almanac.contains("King Leo II"), almanac);
        assertTrue(almanac.contains("reigning"), almanac);
    }

    @Test
    void everyPageFitsAWrittenBook() {
        List<ReignRecord> history = List.of(new ReignRecord("id", "Leo", "King", 1, 0L, ReignRecord.OPEN));
        for (String page : AlmanacBook.pages("Avalon", 400L, history, HARVEST_FIRST)) {
            assertTrue(page.length() <= 256, "page too long: " + page.length());
        }
    }

    @Test
    void anEmptyRollStillBinds() {
        assertEquals(false, AlmanacBook.pages("Avalon", 0L, List.of(), HARVEST_FIRST).isEmpty());
    }
}
