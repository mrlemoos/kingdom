package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Word that the granary is full: hung on the Gazette by the realm, and binding nobody. */
class GranaryGazetteTest {

    @Test
    @DisplayName("the post names the grain that went to waste")
    void thePostNamesTheGrainThatWentToWaste() {
        GazettePost post = GranaryGazette.overflowPost(18, 42L);

        assertEquals("The granary is full", post.title());
        assertTrue(post.body().contains("18 wheat"), post.body());
        assertEquals(42L, post.mcDay());
    }

    @Test
    @DisplayName("a full granary is an announcement, never a decree, and the realm is its author")
    void aFullGranaryIsAnAnnouncement() {
        GazettePost post = GranaryGazette.overflowPost(9, 1L);

        assertEquals(GazettePostKind.ANNOUNCEMENT, post.kind());
        assertTrue(post.curfew().isEmpty());
        assertEquals(SeasonGazette.REALM_AUTHOR, post.authorUuid());
    }

    @Test
    @DisplayName("the shortfall post names the bales the realm stands short")
    void theShortfallPostNamesTheBales() {
        GazettePost post = GranaryGazette.shortfallPost("north_granary", 34, 7L);

        assertTrue(post.title().contains("34"), post.title());
        assertTrue(post.body().contains("34 bales"), post.body());
        assertEquals(GazettePostKind.ANNOUNCEMENT, post.kind());
        assertEquals(SeasonGazette.REALM_AUTHOR, post.authorUuid());
        assertEquals(7L, post.mcDay());
    }

    @Test
    @DisplayName("a realm short of a single bale is told so in the singular")
    void oneBaleShortReadsInTheSingular() {
        GazettePost post = GranaryGazette.shortfallPost("north_granary", 1, 7L);

        assertTrue(post.body().contains("1 bale "), post.body());
    }

    @Test
    @DisplayName("a granary that will see the winter through is called provisioned")
    void aProvisionedGranaryIsSaidToBe() {
        GazettePost post = GranaryGazette.shortfallPost("north_granary", 0, 7L);

        assertTrue(post.title().toLowerCase().contains("provisioned"), post.title());
        assertTrue(post.body().contains("provisioned"), post.body());
    }

    @Test
    @DisplayName("a realm that has sited no granary is warned it has none")
    void aRealmWithNoGranaryIsWarnedOfIt() {
        GazettePost post = GranaryGazette.shortfallPost(null, 120, 7L);

        assertTrue(post.title().contains("no granary"), post.title());
        assertTrue(post.body().contains("/kingdom granary setregion"), post.body());
        assertEquals(post.body(), GranaryGazette.shortfallPost("   ", 120, 7L).body());
    }
}
