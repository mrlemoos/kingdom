package dev.mrlemoos.kingdom.model.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConductProvisionTest {

    @Test
    void fiscalOnlyAssentedActHasNoConductProvisions() {
        AssentedAct act = new AssentedAct(
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
                List.of());

        assertTrue(act.conductProvisions().isEmpty());
    }

    @Test
    void supplyActCanEmbedConductProvisions() {
        List<ConductProvision> provisions = List.of(
                new ConductProvision(ConductKind.BUILD_BAN),
                new ConductProvision(ConductKind.CURFEW));

        AssentedAct act = new AssentedAct(
                "northmarch-2",
                "Supply Act with conduct",
                BillType.BUDGET,
                2_000L,
                List.of("Supply Act"),
                Map.of(),
                null,
                "world",
                1,
                64,
                1,
                0,
                provisions);

        assertEquals(2, act.conductProvisions().size());
        assertEquals(ConductKind.BUILD_BAN, act.conductProvisions().get(0).kind());
        assertEquals(ConductKind.CURFEW, act.conductProvisions().get(1).kind());
    }

    @Test
    void billCarriesConductProvisionsSeparatelyFromFiscalPayload() {
        Bill bill = new Bill(
                "northmarch-3",
                "northmarch",
                BillType.FISCAL,
                "Finance Act",
                BillState.TABLED,
                UUID.randomUUID(),
                new BillPayload.Fiscal(dev.mrlemoos.kingdom.economy.model.FiscalRates.defaults()),
                3_000L,
                List.of(new ConductProvision(ConductKind.WAR_LIMIT)));

        assertEquals(1, bill.conductProvisions().size());
        assertEquals(ConductKind.WAR_LIMIT, bill.conductProvisions().get(0).kind());
    }
}
