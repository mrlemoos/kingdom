package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChurchConsentBookTest {

    private static final UUID GROOM = UUID.randomUUID();
    private static final UUID BRIDE = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    @Test
    void oneOfferIsNotConsent() {
        ChurchConsentBook book = new ChurchConsentBook();
        assertFalse(book.offerWedding(GROOM, BRIDE));
        assertTrue(book.hasWeddingOffer(GROOM, BRIDE));
    }

    @Test
    void theAnsweringOfferMakesTheMatch() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.offerWedding(GROOM, BRIDE);
        assertTrue(book.offerWedding(BRIDE, GROOM));
        assertFalse(book.hasWeddingOffer(GROOM, BRIDE));
    }

    @Test
    void anAnswerFromSomebodyElseIsNoAnswer() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.offerWedding(GROOM, BRIDE);
        assertFalse(book.offerWedding(STRANGER, GROOM));
    }

    @Test
    void weddingAndDivorceOffersAreKeptApart() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.offerWedding(GROOM, BRIDE);
        assertFalse(book.offerDivorce(BRIDE, GROOM));
    }

    @Test
    void forgettingAPlayerDropsBothSidesOfTheOffer() {
        ChurchConsentBook book = new ChurchConsentBook();
        book.offerWedding(GROOM, BRIDE);
        book.forget(GROOM);
        assertFalse(book.offerWedding(BRIDE, GROOM));
    }
}
