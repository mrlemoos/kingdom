package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.ArrestReward;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreWarrantTest {

    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID POSTER = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void roundTripPreservesWarrantAndArrestReward() {
        Warrant warrant = new Warrant(
                "northmarch-warrant-1",
                "northmarch",
                SUSPECT,
                "northmarch-build",
                ConductKind.BUILD_BAN,
                WarrantStatus.ACTIVE,
                1_700_000_000_000L);
        warrant.setArrestReward(new ArrestReward(POSTER, 25.0));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeWarrants(config, "kingdoms.northmarch.police.warrants", List.of(warrant));

        List<Warrant> loaded = YamlKingdomStore.readWarrants(
                config.getConfigurationSection("kingdoms.northmarch.police.warrants"),
                "northmarch");

        assertEquals(1, loaded.size());
        Warrant restored = loaded.get(0);
        assertEquals("northmarch-warrant-1", restored.id());
        assertEquals(SUSPECT, restored.suspectId());
        assertEquals(WarrantStatus.ACTIVE, restored.status());
        assertEquals(ConductKind.BUILD_BAN, restored.provisionKind());
        assertTrue(restored.arrestReward().isPresent());
        assertEquals(POSTER, restored.arrestReward().orElseThrow().posterId());
        assertEquals(25.0, restored.arrestReward().orElseThrow().amount(), 1e-9);
    }
}
