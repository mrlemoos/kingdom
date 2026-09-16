package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TreatyBillYamlRoundTripTest {

    @Test
    void currentTreatyBillPayloadRoundTrips() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentState().setCurrentBill(new Bill(
                "northmarch-7", "northmarch", BillType.TREATY, "Trade pact", BillState.TABLED,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                new BillPayload.Treaty("southreach", TreatyKind.TRADE_PACT, false), 9_000L));
        YamlConfiguration config = new YamlConfiguration();

        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);
        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        BillPayload.Treaty payload = (BillPayload.Treaty) loaded.getParliamentState().currentBill().orElseThrow().payload();
        assertEquals("southreach", payload.counterpartKingdomId());
        assertEquals(TreatyKind.TRADE_PACT, payload.kind());
        assertFalse(payload.repeal());
    }
}
