package dev.mrlemoos.kingdom.model.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.city.GazetteBoard;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KingdomCityStateGazetteTest {

    private static final UUID AUTHOR = UUID.fromString("00000000-0000-0000-0000-000000000041");
    private static final UUID CRIER = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    void clearCapitalAlsoClearsTownCrierEntityId() {
        KingdomCityState city = new KingdomCityState();
        city.setCapital(new CapitalLocation("world", 0, 64, 0, 0f, 0f));
        city.setTownCrierEntityId(CRIER);

        city.clearCapital();

        assertTrue(city.capital().isEmpty());
        assertTrue(city.townCrierEntityId().isEmpty());
    }

    @Test
    void addGazettePostUsesRetentionAndKeepsNewestFirst() {
        KingdomCityState city = new KingdomCityState();
        for (int i = 0; i < GazetteBoard.ANNOUNCEMENT_CAP + 1; i++) {
            city.addGazettePost(new GazettePost(
                    "a" + i, "body", AUTHOR, i, GazettePostKind.ANNOUNCEMENT, Optional.empty()));
        }

        assertEquals(GazetteBoard.ANNOUNCEMENT_CAP, city.gazettePostsView().size());
        assertEquals("a" + GazetteBoard.ANNOUNCEMENT_CAP, city.gazettePostsView().get(0).title());
    }

    @Test
    void decreeCurfewEmptyMeansPluginFallbackSlot() {
        KingdomCityState city = new KingdomCityState();
        assertTrue(city.decreeCurfew().isEmpty());

        city.setDecreeCurfew(CurfewEnforcementConfig.enabled(13_000L, 18_000L));
        assertTrue(city.decreeCurfew().isPresent());
        assertEquals(18_000L, city.decreeCurfew().get().windowEndTick());

        city.clearDecreeCurfew();
        assertTrue(city.decreeCurfew().isEmpty());
    }
}
