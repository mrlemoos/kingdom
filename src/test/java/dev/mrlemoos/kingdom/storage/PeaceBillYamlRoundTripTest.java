package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PeaceBillYamlRoundTripTest {

    @Test
    void currentPeaceBillPayloadRoundTrips() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        Bill bill = new Bill(
                "northmarch-7",
                "northmarch",
                BillType.PEACE,
                "Treaty of Southreach",
                BillState.TABLED,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                new BillPayload.Peace("war-1"),
                9_000L);
        kingdom.getParliamentState().setCurrentBill(bill);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        Bill loadedBill = loaded.getParliamentState().currentBill().orElseThrow();
        BillPayload.Peace payload = (BillPayload.Peace) loadedBill.payload();
        assertEquals("war-1", payload.warId());
        assertEquals(BillType.PEACE, loadedBill.type());
    }
}
