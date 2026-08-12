package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoyalStandardTest {

    @Test
    void fliesTheStandardOneBlockEastOfTheLordsPointSoItDoesNotBlockTheLanding() {
        Optional<RoyalStandard.StandardPosition> position =
                RoyalStandard.positionFor(ChamberSite.of("world", 10.7, 65.0, -3.2));

        assertTrue(position.isPresent());
        assertEquals(new RoyalStandard.StandardPosition("world", 11, 65, -4), position.get());
    }

    @Test
    void hasNowhereToFlyWithoutALordsPoint() {
        assertTrue(RoyalStandard.positionFor(null).isEmpty());
    }

    @Test
    void hasNowhereToFlyWithoutAWorld() {
        assertTrue(RoyalStandard.positionFor(ChamberSite.of("", 0, 65, 0)).isEmpty());
    }

    @Test
    void raisesTheCrownsGoldAsAnOrangeBanner() {
        assertEquals("ORANGE_BANNER", RoyalStandard.bannerMaterialFor("&6"));
    }

    @Test
    void readsColoursAlreadyEncodedWithTheSectionSign() {
        assertEquals("ORANGE_BANNER", RoyalStandard.bannerMaterialFor("§6"));
    }

    @Test
    void translatesEveryColourCodeToItsNearestBanner() {
        assertEquals("BLACK_BANNER", RoyalStandard.bannerMaterialFor("&0"));
        assertEquals("LIME_BANNER", RoyalStandard.bannerMaterialFor("&a"));
        assertEquals("LIGHT_BLUE_BANNER", RoyalStandard.bannerMaterialFor("&b"));
        assertEquals("MAGENTA_BANNER", RoyalStandard.bannerMaterialFor("&d"));
        assertEquals("YELLOW_BANNER", RoyalStandard.bannerMaterialFor("&e"));
        assertEquals("WHITE_BANNER", RoyalStandard.bannerMaterialFor("&f"));
    }

    @Test
    void fallsBackToWhiteForAnythingItCannotRead() {
        assertEquals("WHITE_BANNER", RoyalStandard.bannerMaterialFor("not a colour"));
        assertEquals("WHITE_BANNER", RoyalStandard.bannerMaterialFor(null));
    }

    @Test
    void fliesTheCrownsOwnColourAsTheRealmHasNoneOfItsOwn() {
        assertEquals("ORANGE_BANNER", RoyalStandard.crownBannerMaterial());
    }
}
