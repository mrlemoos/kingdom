package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.war.oath.SwornOutsider;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class SwornOutsiderYamlRoundTripTest {

    @Test
    void swornOutsidersRoundTripAcrossRestart() {
        SwornOutsider outsider = new SwornOutsider(
                "northmarch",
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "service to Northmarch",
                123L);
        YamlConfiguration config = new YamlConfiguration();

        YamlKingdomStore.writeSwornOutsiders(config, "sworn-outsiders", List.of(outsider));

        assertEquals(
                List.of(outsider),
                YamlKingdomStore.readSwornOutsiders(config.getConfigurationSection("sworn-outsiders")));
    }
}
