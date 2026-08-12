package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStorePoliceTest {

    @Test
    void roundTripPreservesPoliceState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID constable = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID judge = UUID.fromString("00000000-0000-0000-0000-000000000005");
        UUID patrolGolem = UUID.fromString("00000000-0000-0000-0000-000000000010");

        var police = kingdom.getPoliceState();
        police.appointConstable(constable);
        police.appointJudge(judge);
        police.setCell(1, new PrisonCellLocation("world", 10, 64, 20));
        police.setCourt(new CourtLocation("world", 5, 64, 5));
        police.registerPatrolGolem(patrolGolem);
        police.setJudgeEntityId(UUID.fromString("00000000-0000-0000-0000-000000000012"));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writePolice(config, "kingdoms.northmarch.police", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readPolice(config.getConfigurationSection("kingdoms.northmarch.police"), loaded);

        var loadedPolice = loaded.getPoliceState();
        assertTrue(loadedPolice.isConstable(constable));
        assertTrue(loadedPolice.isJudge(judge));
        assertEquals("world", loadedPolice.cell(1).orElseThrow().worldName());
        assertEquals(10, loadedPolice.cell(1).orElseThrow().x());
        assertTrue(loadedPolice.hasCourt());
        assertEquals("world", loadedPolice.court().orElseThrow().worldName());
        assertTrue(loadedPolice.isPatrolGolem(patrolGolem));
        assertEquals(
                UUID.fromString("00000000-0000-0000-0000-000000000012"),
                loadedPolice.judgeEntityId().orElseThrow());
    }

    @Test
    void roundTripPreservesTheBenchYaw() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getPoliceState().setCourt(new CourtLocation("world", 5, 64, 5, -90f));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writePolice(config, "kingdoms.northmarch.police", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readPolice(config.getConfigurationSection("kingdoms.northmarch.police"), loaded);

        assertEquals(-90f, loaded.getPoliceState().court().orElseThrow().yaw(), 0.001f);
    }

    @Test
    void aCourtSitedBeforeTheBenchHadAYawLooksDueSouth() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getPoliceState().setCourt(new CourtLocation("world", 5, 64, 5));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writePolice(config, "kingdoms.northmarch.police", kingdom);
        config.set("kingdoms.northmarch.police.court.yaw", null);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readPolice(config.getConfigurationSection("kingdoms.northmarch.police"), loaded);

        assertEquals(0f, loaded.getPoliceState().court().orElseThrow().yaw(), 0.001f);
    }
}
