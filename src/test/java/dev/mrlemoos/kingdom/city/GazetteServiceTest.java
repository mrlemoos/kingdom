package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GazetteServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final CapitalLocation CAPITAL = new CapitalLocation("world", 1, 64, 1, 0f, 0f);

    private KingdomService kingdomService;
    private CityService cityService;
    private GazetteService gazetteService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        cityService = new CityService(kingdomService, id -> false);
        gazetteService = new GazetteService(kingdomService);
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(CITIZEN, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        cityService.setCapital("northmarch", NobleRank.KING, CAPITAL);
    }

    @Test
    void publishAnnouncementStoresPostWithoutHansardOrCurfew() {
        CityResult result = gazetteService.publishAnnouncement(
                "northmarch", KING, "Market day", "Stalls open at noon.", 12L);

        assertInstanceOf(CityResult.Success.class, result);
        KingdomCityState city = kingdomService.getKingdom("northmarch").orElseThrow().getCityState();
        List<GazettePost> posts = city.gazettePostsView();
        assertEquals(1, posts.size());
        assertEquals(GazettePostKind.ANNOUNCEMENT, posts.get(0).kind());
        assertEquals("Market day", posts.get(0).title());
        assertTrue(posts.get(0).curfew().isEmpty());
        assertTrue(kingdomService.getKingdom("northmarch").orElseThrow()
                .getParliamentState().hansardView().isEmpty());
        assertTrue(city.decreeCurfew().isEmpty());
    }

    @Test
    void publishDecreeRecordsHansardAndAppliesCurfew() {
        CityResult result = gazetteService.publishDecree(
                "northmarch",
                KING,
                "Night watch",
                "Subjects indoors after dusk.",
                20L,
                Optional.of(CurfewPresets.duskDawn()));

        assertInstanceOf(CityResult.Success.class, result);
        var kingdom = kingdomService.getKingdom("northmarch").orElseThrow();
        GazettePost post = kingdom.getCityState().gazettePostsView().get(0);
        assertEquals(GazettePostKind.DECREE, post.kind());
        assertTrue(post.curfew().isPresent());
        assertEquals(13_000L, post.curfew().get().startTick());

        List<HansardRecord> hansard = kingdom.getParliamentState().hansardView();
        assertEquals(1, hansard.size());
        assertEquals("decree", hansard.get(0).business());
        assertEquals("Night watch", hansard.get(0).title());
        assertTrue(hansard.get(0).carried());

        Optional<CurfewEnforcementConfig> curfew = kingdom.getCityState().decreeCurfew();
        assertTrue(curfew.isPresent());
        assertTrue(curfew.get().enabled());
        assertEquals(23_000L, curfew.get().windowEndTick());
    }

    @Test
    void liftCurfewDecreeDisablesEnforcement() {
        gazetteService.publishDecree(
                "northmarch", KING, "Watch", "body", 1L, Optional.of(CurfewPresets.duskDawn()));
        gazetteService.publishDecree(
                "northmarch", KING, "Lift", "body", 2L, Optional.of(CurfewPresets.lifted()));

        Optional<CurfewEnforcementConfig> curfew =
                kingdomService.getKingdom("northmarch").orElseThrow().getCityState().decreeCurfew();
        assertTrue(curfew.isPresent());
        assertFalse(curfew.get().enabled());
    }

    @Test
    void decreeWithNoCurfewChoiceLeavesExistingCurfewUnchanged() {
        gazetteService.publishDecree(
                "northmarch", KING, "Watch", "body", 1L, Optional.of(CurfewPresets.nightfallMidnight()));
        gazetteService.publishDecree(
                "northmarch", KING, "Honours", "body", 2L, Optional.empty());

        Optional<CurfewEnforcementConfig> curfew =
                kingdomService.getKingdom("northmarch").orElseThrow().getCityState().decreeCurfew();
        assertTrue(curfew.isPresent());
        assertEquals(18_000L, curfew.get().windowEndTick());
    }

    @Test
    void announcementRequiresCapital() {
        cityService.clearCapital("northmarch", NobleRank.KING);
        CityResult result = gazetteService.publishAnnouncement(
                "northmarch", KING, "Title", "Body", 1L);
        assertInstanceOf(CityResult.Failure.class, result);
    }

    @Test
    void onlyCrownMayPublish() {
        CityResult result = gazetteService.publishAnnouncement(
                "northmarch", CITIZEN, "Title", "Body", 1L);
        assertInstanceOf(CityResult.Failure.class, result);
    }
}
