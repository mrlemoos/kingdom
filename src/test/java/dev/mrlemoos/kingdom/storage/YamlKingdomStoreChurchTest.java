package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.church.FuneralRecord;
import dev.mrlemoos.kingdom.model.church.Marriage;
import dev.mrlemoos.kingdom.model.church.VillagerFuneralRecord;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreChurchTest {

    private static final UUID PRIEST = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID CLERIC = UUID.fromString("00000000-0000-0000-0000-000000000032");
    private static final UUID GROOM = UUID.fromString("00000000-0000-0000-0000-000000000033");
    private static final UUID BRIDE = UUID.fromString("00000000-0000-0000-0000-000000000034");
    private static final UUID VILLAGER = UUID.fromString("00000000-0000-0000-0000-000000000035");
    private static final UUID MONARCH = UUID.fromString("00000000-0000-0000-0000-000000000036");

    @Test
    void roundTripPreservesChurchState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        var church = kingdom.getChurchState();
        church.setChurch(new ChurchSite("world", 8.5, 64.0, -3.25, 90.5f, -12.5f));
        church.consecrate();
        church.swearPriest(PRIEST);
        church.setClericEntityId(CLERIC);
        church.crown(MONARCH);
        church.setLastMassDay(21L);
        church.wed(new Marriage(GROOM, BRIDE, 1_700_000_000_000L));
        church.holdFuneralRecord(GROOM, new FuneralRecord(60, 12L));
        church.holdVillagerFuneralRecord(VILLAGER, new VillagerFuneralRecord(42.5d, 13L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeChurch(config, "kingdoms.northmarch.church", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readChurch(config.getConfigurationSection("kingdoms.northmarch.church"), loaded);
        var restored = loaded.getChurchState();

        assertEquals(8.5, restored.church().orElseThrow().x());
        assertEquals(90.5f, restored.church().orElseThrow().yaw());
        assertTrue(restored.isConsecrated());
        assertEquals(PRIEST, restored.priestId().orElseThrow());
        assertEquals(CLERIC, restored.clericEntityId().orElseThrow());
        assertEquals(MONARCH, restored.crownedMonarchId().orElseThrow());
        assertEquals(21L, restored.lastMassDay().orElseThrow());
        assertEquals(BRIDE, restored.spouseOf(GROOM).orElseThrow());
        assertEquals(60, restored.funeralRecord(GROOM).orElseThrow().heldExperience());
        assertEquals(12L, restored.funeralRecord(GROOM).orElseThrow().diedOnDay());
        assertEquals(42.5d, restored.villagerFuneralRecord(VILLAGER).orElseThrow().heldBalance());
    }

    @Test
    void anUnsitedChurchWritesNothing() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeChurch(config, "kingdoms.northmarch.church", kingdom);
        assertFalse(config.contains("kingdoms.northmarch.church"));
    }

    @Test
    void consecrationIsNotRestoredWithoutAChurch() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("kingdoms.northmarch.church.consecrated", true);
        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readChurch(config.getConfigurationSection("kingdoms.northmarch.church"), loaded);
        assertFalse(loaded.getChurchState().isConsecrated());
    }
}
