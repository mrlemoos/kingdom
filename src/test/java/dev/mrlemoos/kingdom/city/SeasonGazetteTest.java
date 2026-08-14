package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeasonGazetteTest {

    @Test
    @DisplayName("the crier cries the season by name")
    void theCrierCriesTheSeasonByName() {
        GazettePost post = SeasonGazette.post(Season.WINTER, 42L);

        assertEquals("Winter is come upon the realm", post.title());
        assertEquals(42L, post.mcDay());
    }

    @Test
    @DisplayName("the post carries the season's own proclamation as its body")
    void thePostCarriesTheSeasonsProclamation() {
        for (Season season : Season.values()) {
            assertEquals(season.proclamation(), SeasonGazette.post(season, 1L).body());
        }
    }

    @Test
    @DisplayName("a season turn is an announcement, never a decree, and binds nobody")
    void aSeasonTurnIsAnAnnouncement() {
        GazettePost post = SeasonGazette.post(Season.SUMMER, 1L);

        assertEquals(GazettePostKind.ANNOUNCEMENT, post.kind());
        assertTrue(post.curfew().isEmpty());
    }

    @Test
    @DisplayName("the realm itself is the author, no Crown hand required")
    void theRealmItselfIsTheAuthor() {
        assertEquals(SeasonGazette.REALM_AUTHOR, SeasonGazette.post(Season.AUTUMN, 1L).authorUuid());
    }
}
