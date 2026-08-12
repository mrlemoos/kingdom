package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreCityTest {

    @Test
    void roundTripPreservesCityState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID holder = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID mayor = UUID.fromString("00000000-0000-0000-0000-000000000022");

        var city = kingdom.getCityState();
        city.setCapital(new CapitalLocation("world", 10.5, 64.0, -20.25, 90.5f, -12.5f));
        city.grantPermit(holder, 1_700_000_000_000L);
        city.setLordMayorEntityId(mayor);

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
    }
}
