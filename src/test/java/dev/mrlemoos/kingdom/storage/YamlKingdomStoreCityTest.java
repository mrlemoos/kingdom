package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePost.GazetteCurfewWindow;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreCityTest {

    private static final UUID AUTHOR = UUID.fromString("00000000-0000-0000-0000-000000000061");

    @Test
    void roundTripPreservesCityState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID holder = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID mayor = UUID.fromString("00000000-0000-0000-0000-000000000022");
        UUID crier = UUID.fromString("00000000-0000-0000-0000-000000000023");

        var city = kingdom.getCityState();
        city.setCapital(new CapitalLocation("world", 10.5, 64.0, -20.25, 90.5f, -12.5f));
        city.grantPermit(holder, 1_700_000_000_000L);
        city.setLordMayorEntityId(mayor);
        city.setTownCrierEntityId(crier);
        city.addGazettePost(new GazettePost(
                "Market",
                "Open stalls.",
                AUTHOR,
                5L,
                GazettePostKind.ANNOUNCEMENT,
                Optional.empty()));
        city.addGazettePost(new GazettePost(
                "Curfew",
                "Indoors after dusk.",
                AUTHOR,
                6L,
                GazettePostKind.DECREE,
                Optional.of(new GazetteCurfewWindow(13_000L, 23_000L))));
        city.setDecreeCurfew(CurfewEnforcementConfig.enabled(13_000L, 23_000L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeCity(config, "kingdoms.northmarch.city", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readCity(config.getConfigurationSection("kingdoms.northmarch.city"), loaded);

        var loadedCity = loaded.getCityState();
        assertTrue(loadedCity.hasCapital());
        CapitalLocation capital = loadedCity.capital().orElseThrow();
        assertEquals("world", capital.worldName());
        assertEquals(10.5, capital.x());
        assertEquals(64.0, capital.y());
        assertEquals(-20.25, capital.z());
        assertEquals(90.5f, capital.yaw());
        assertEquals(-12.5f, capital.pitch());
        assertTrue(loadedCity.hasPermit(holder));
        assertEquals(1_700_000_000_000L, loadedCity.permitsView().get(holder));
        assertEquals(mayor, loadedCity.lordMayorEntityId().orElseThrow());
        assertEquals(crier, loadedCity.townCrierEntityId().orElseThrow());

        assertEquals(2, loadedCity.gazettePostsView().size());
        GazettePost newest = loadedCity.gazettePostsView().get(0);
        assertEquals("Curfew", newest.title());
        assertEquals(GazettePostKind.DECREE, newest.kind());
        assertTrue(newest.curfew().isPresent());
        assertEquals(13_000L, newest.curfew().get().startTick());
        assertEquals("Market", loadedCity.gazettePostsView().get(1).title());

        Optional<CurfewEnforcementConfig> decreeCurfew = loadedCity.decreeCurfew();
        assertTrue(decreeCurfew.isPresent());
        assertTrue(decreeCurfew.get().enabled());
        assertEquals(23_000L, decreeCurfew.get().windowEndTick());
    }

    @Test
    void roundTripOfEmptyCityStateStaysEmpty() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeCity(config, "kingdoms.northmarch.city", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readCity(config.getConfigurationSection("kingdoms.northmarch.city"), loaded);

        assertFalse(loaded.getCityState().hasCapital());
        assertTrue(loaded.getCityState().permitsView().isEmpty());
        assertTrue(loaded.getCityState().gazettePostsView().isEmpty());
        assertTrue(loaded.getCityState().townCrierEntityId().isEmpty());
        assertTrue(loaded.getCityState().decreeCurfew().isEmpty());
    }

    @Test
    void roundTripPreservesLiftedDecreeCurfew() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getCityState().setDecreeCurfew(CurfewEnforcementConfig.disabled(13_000L, 18_000L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeCity(config, "kingdoms.northmarch.city", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readCity(config.getConfigurationSection("kingdoms.northmarch.city"), loaded);

        Optional<CurfewEnforcementConfig> curfew = loaded.getCityState().decreeCurfew();
        assertTrue(curfew.isPresent());
        assertFalse(curfew.get().enabled());
        assertEquals(18_000L, curfew.get().windowEndTick());
    }
}
