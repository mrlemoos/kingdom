package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AssentedActConductYamlRoundTripTest {

    @Test
    void fiscalActWithoutProvisionsRoundTripsEmptyList() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentState().addAssentedAct(new AssentedAct(
                "northmarch-1",
                "Finance Act",
                BillType.FISCAL,
                1_000L,
                List.of("Finance Act"),
                Map.of(),
                null,
                "world",
                0,
                64,
                0,
                0,
                List.of()));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        AssentedAct act = loaded.getParliamentState().assentedActsView().get(0);
        assertTrue(act.conductProvisions().isEmpty());
    }

    @Test
    void supplyActWithConductProvisionsRoundTrips() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentState().addAssentedAct(new AssentedAct(
                "northmarch-2",
                "Supply Act with conduct",
                BillType.BUDGET,
                2_000L,
                List.of("Supply Act"),
                Map.of(),
                null,
                "world",
                5,
                70,
                5,
                1,
                List.of(
                        new ConductProvision(ConductKind.BUILD_BAN),
                        new ConductProvision(ConductKind.CURFEW),
                        new ConductProvision(ConductKind.WAR_LIMIT))));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        AssentedAct act = loaded.getParliamentState().assentedActsView().get(0);
        assertEquals(3, act.conductProvisions().size());
        assertEquals(ConductKind.BUILD_BAN, act.conductProvisions().get(0).kind());
        assertEquals(ConductKind.CURFEW, act.conductProvisions().get(1).kind());
        assertEquals(ConductKind.WAR_LIMIT, act.conductProvisions().get(2).kind());
        assertEquals("Supply Act with conduct", act.title());
        assertEquals(BillType.BUDGET, act.type());
    }

    @Test
    void currentBillConductProvisionsRoundTrip() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        Bill bill = new Bill(
                "northmarch-3",
                "northmarch",
                BillType.BUDGET,
                "Budget with build ban",
                BillState.TABLED,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                new BillPayload.Budget(100.0),
                3_000L,
                List.of(new ConductProvision(ConductKind.BUILD_BAN)));
        kingdom.getParliamentState().setCurrentBill(bill);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        Bill loadedBill = loaded.getParliamentState().currentBill().orElseThrow();
        assertEquals(1, loadedBill.conductProvisions().size());
        assertEquals(ConductKind.BUILD_BAN, loadedBill.conductProvisions().get(0).kind());
        assertEquals(100.0, ((BillPayload.Budget) loadedBill.payload()).amount(), 1e-9);
    }

    @Test
    void legacyActWithoutConductSectionLoadsEmptyProvisions() {
        YamlConfiguration config = new YamlConfiguration();
        String path = "kingdoms.northmarch.parliament.acts.0";
        config.set(path + ".bill-id", "northmarch-legacy");
        config.set(path + ".title", "Legacy Finance Act");
        config.set(path + ".type", "fiscal");
        config.set(path + ".assented-at", 500L);
        config.set(path + ".pages", List.of("Legacy"));
        config.set(path + ".shelf.world", "world");
        config.set(path + ".shelf.x", 0);
        config.set(path + ".shelf.y", 64);
        config.set(path + ".shelf.z", 0);
        config.set(path + ".shelf.slot", 0);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        AssentedAct act = loaded.getParliamentState().assentedActsView().get(0);
        assertTrue(act.conductProvisions().isEmpty());
        assertEquals("Legacy Finance Act", act.title());
    }
}
