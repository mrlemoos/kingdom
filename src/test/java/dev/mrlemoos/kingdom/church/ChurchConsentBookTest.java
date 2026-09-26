package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChurchConsentBookTest {

    private static final UUID GROOM = UUID.randomUUID();
    private static final UUID BRIDE = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final long NOW = 1_000_000L;

    @Test
    void aProposalWaitsOnTheAnswerForAMinute() {
        ChurchConsentBook book = new ChurchConsentBook();

        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW).isEmpty());

        Optional<ChurchConsentBook.Request> pending = book.pendingFor(BRIDE, NOW + 59_000L);
        assertTrue(pending.isPresent());
        assertEquals(GROOM, pending.get().proposer());
        assertEquals(ChurchConsentBook.Kind.MARRIAGE, pending.get().kind());
        assertEquals(1_000L, pending.get().remainingMs(NOW + 59_000L));
        assertEquals(60_000L, ChurchConsentBook.WINDOW_MS);
    }

    @Test
    void anAnswerInTimeClosesTheRequest() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW);

        Optional<ChurchConsentBook.Request> answered = book.answer(BRIDE, NOW + 30_000L);

        assertTrue(answered.isPresent());
        assertEquals(GROOM, answered.get().proposer());
        assertTrue(book.pendingFor(BRIDE, NOW + 30_000L).isEmpty());
    }

    @Test
    void anAnswerTooLateIsNoAnswer() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW);

        assertTrue(book.answer(BRIDE, NOW + ChurchConsentBook.WINDOW_MS).isEmpty());
    }

    @Test
    void lapsedRequestsAreSweptOnceAndReturned() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.DIVORCE, "north", GROOM, BRIDE, NOW);

        assertTrue(book.expire(NOW + 10_000L).isEmpty());
        assertEquals(1, book.open(NOW + 10_000L).size());

        List<ChurchConsentBook.Request> lapsed = book.expire(NOW + ChurchConsentBook.WINDOW_MS);
        assertEquals(1, lapsed.size());
        assertEquals(ChurchConsentBook.Kind.DIVORCE, lapsed.get(0).kind());
        assertTrue(book.expire(NOW + ChurchConsentBook.WINDOW_MS).isEmpty());
        assertTrue(book.open(NOW + ChurchConsentBook.WINDOW_MS).isEmpty());
    }

    @Test
    void nobodyIsAskedTwiceAtOnce() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW);

        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", STRANGER, BRIDE, NOW).isPresent());
        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", STRANGER, GROOM, NOW).isPresent());
        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", BRIDE, STRANGER, NOW).isPresent());
    }

    @Test
    void aLapsedRequestFreesBothPartiesToBeAskedAgain() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW);

        long later = NOW + ChurchConsentBook.WINDOW_MS;
        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", STRANGER, BRIDE, later).isEmpty());
    }

    @Test
    void nobodyMayAskThemselves() {
        ChurchConsentBook book = new ChurchConsentBook();

        assertTrue(book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, GROOM, NOW).isPresent());
    }

    @Test
    void forgettingAPlayerDropsEitherSideOfTheRequest() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.propose(ChurchConsentBook.Kind.MARRIAGE, "north", GROOM, BRIDE, NOW);

        book.forget(GROOM);

        assertTrue(book.pendingFor(BRIDE, NOW).isEmpty());
    }
}
