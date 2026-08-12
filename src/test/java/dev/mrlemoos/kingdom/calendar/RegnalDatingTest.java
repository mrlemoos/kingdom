package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegnalDatingTest {

    private static ReignRecord reign(String name, long accession, long end, int ordinal) {
        return new ReignRecord("uuid-" + name + "-" + accession, name, "King", ordinal, accession, end);
    }

    @Test
    void theFirstOfANameReignsWithoutAnOrdinal() {
        assertEquals("King Leo", reign("Leo", 0L, ReignRecord.OPEN, 1).styledName());
    }

    @Test
    void laterMonarchsOfTheSameNameAreNumbered() {
        assertEquals("King Leo II", reign("Leo", 400L, ReignRecord.OPEN, 2).styledName());
        assertEquals("King Leo IV", reign("Leo", 900L, ReignRecord.OPEN, 4).styledName());
    }

    @Test
    void ordinalsCountEarlierReignsOfTheSameNameInThatKingdom() {
        List<ReignRecord> history = List.of(
                reign("Leo", 0L, 300L, 1),
                reign("Ana", 300L, 600L, 1),
                reign("Leo", 600L, 900L, 2));
        assertEquals(3, RegnalDating.nextOrdinal(history, "Leo"));
        assertEquals(2, RegnalDating.nextOrdinal(history, "Ana"));
        assertEquals(1, RegnalDating.nextOrdinal(history, "Bea"));
    }

    @Test
    void regnalYearOneRunsUntilTheFirstAccessionAnniversary() {
        ReignRecord leo = reign("Leo", 100L, ReignRecord.OPEN, 1);
        assertEquals(1, RegnalDating.regnalYear(leo, 100L));
        assertEquals(1, RegnalDating.regnalYear(leo, 459L));
        assertEquals(2, RegnalDating.regnalYear(leo, 460L));
        assertEquals(3, RegnalDating.regnalYear(leo, 820L));
    }

    @Test
    void anAccessionAnniversaryFallsEveryThreeHundredAndSixtyDays() {
        ReignRecord leo = reign("Leo", 100L, ReignRecord.OPEN, 1);
        assertTrue(RegnalDating.isAccessionAnniversary(leo, 460L));
        assertTrue(RegnalDating.isAccessionAnniversary(leo, 820L));
        assertEquals(false, RegnalDating.isAccessionAnniversary(leo, 100L));
        assertEquals(false, RegnalDating.isAccessionAnniversary(leo, 461L));
    }

    @Test
    void anOpenReignIsTheCurrentOne() {
        List<ReignRecord> history = List.of(
                reign("Leo", 0L, 300L, 1),
                reign("Ana", 300L, ReignRecord.OPEN, 1));
        assertTrue(RegnalDating.currentReign(history).isPresent());
        assertEquals("Ana", RegnalDating.currentReign(history).get().monarchName());
    }

    @Test
    void aClosedHistoryLeavesTheRealmInInterregnum() {
        List<ReignRecord> history = List.of(reign("Leo", 0L, 300L, 1));
        assertTrue(RegnalDating.currentReign(history).isEmpty());
        assertEquals(
                "12th of Thawtide, Realm Year 2 (Interregnum)",
                RegnalDating.format(history, 401L));
    }

    @Test
    void anEmptyHistoryLeavesTheRealmInInterregnum() {
        assertEquals("1st of Frostwane, Realm Year 1 (Interregnum)", RegnalDating.format(List.of(), 0L));
    }

    @Test
    void aPastDayIsDatedByTheReignInForceThen() {
        List<ReignRecord> history = List.of(
                reign("Leo", 0L, 300L, 1),
                reign("Ana", 300L, ReignRecord.OPEN, 1));
        assertEquals("11th of Blossoming, Year 1 of King Leo (Realm Year 1)", RegnalDating.format(history, 100L));
        assertEquals("1st of Longnight, Year 1 of King Ana (Realm Year 1)", RegnalDating.format(history, 300L));
    }

    @Test
    void aDayInAPastInterregnumIsDatedByRealmYearAlone() {
        List<ReignRecord> history = List.of(
                reign("Leo", 0L, 100L, 1),
                reign("Ana", 300L, ReignRecord.OPEN, 1));
        assertEquals("21st of Blossoming, Realm Year 1 (Interregnum)", RegnalDating.format(history, 110L));
    }

    @Test
    void aReigningMonarchDatesTheRealmByRegnalYear() {
        List<ReignRecord> history = List.of(
                reign("Leo", 0L, 100L, 1),
                reign("Leo", 100L, ReignRecord.OPEN, 2));
        assertEquals(
                "12th of Blossoming, Year 2 of King Leo II (Realm Year 2)",
                RegnalDating.format(history, 461L));
    }
}
