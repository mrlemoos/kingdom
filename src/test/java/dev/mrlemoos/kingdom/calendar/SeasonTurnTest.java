package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeasonTurnTest {

    private static final long WINTER_DAY_ONE = 9L * RealmCalendar.DAYS_PER_MONTH;

    @Test
    void proclaimsTheSeasonOnItsFirstDay() {
        SeasonTurn turn = new SeasonTurn();
        assertEquals(Optional.of(Season.WINTER), turn.claim(WINTER_DAY_ONE));
    }

    @Test
    void saysNothingTwiceOnTheSameTurn() {
        SeasonTurn turn = new SeasonTurn();
        turn.claim(WINTER_DAY_ONE);
        assertTrue(turn.claim(WINTER_DAY_ONE).isEmpty());
    }

    @Test
    void saysNothingOnAnOrdinaryDay() {
        SeasonTurn turn = new SeasonTurn();
        assertTrue(turn.claim(WINTER_DAY_ONE + 1L).isEmpty());
        assertTrue(turn.claim(1L).isEmpty());
    }

    @Test
    void saysNothingOnAMonthStartInsideASeason() {
        SeasonTurn turn = new SeasonTurn();
        assertTrue(turn.claim(10L * RealmCalendar.DAYS_PER_MONTH).isEmpty());
    }

    @Test
    void holdsItsPeaceAcrossARestartOnTheSameDay() {
        SeasonTurn turn = new SeasonTurn();
        turn.restore(WINTER_DAY_ONE);
        assertEquals(WINTER_DAY_ONE, turn.lastAnnouncedDay());
        assertTrue(turn.claim(WINTER_DAY_ONE).isEmpty());
    }

    @Test
    void speaksAgainWhenTheNextSeasonComes() {
        SeasonTurn turn = new SeasonTurn();
        turn.claim(WINTER_DAY_ONE);
        assertEquals(Optional.of(Season.SPRING), turn.claim(RealmCalendar.DAYS_PER_YEAR));
    }

    @Test
    void theProclamationNamesTheSeasonInBritishProse() {
        String proclamation = Season.WINTER.proclamation();
        assertTrue(proclamation.contains("Winter"), proclamation);
        assertTrue(proclamation.matches(".*[a-z].*"), proclamation);
    }
}
