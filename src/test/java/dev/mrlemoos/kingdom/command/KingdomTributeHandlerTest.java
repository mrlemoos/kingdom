package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.tribute.InMemoryWarDebtStore;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/** Status copy for {@code /kingdom info} comes from the same tribute ledger the Crown pays against. */
class KingdomTributeHandlerTest {

    @Test
    void infoLineNamesOutstandingDebt() {
        ServerMock server = MockBukkit.mock();
        try {
            EconomyService economy = new EconomyService();
            WarTributeService tribute = new WarTributeService(economy, new InMemoryWarDebtStore());
            tribute.applyTribute("northmarch", "southreach", 60.0);
            KingdomTributeHandler handler = new KingdomTributeHandler(
                    new KingdomService(),
                    economy,
                    tribute,
                    new dev.mrlemoos.kingdom.storage.YamlEconomyStore(MockBukkit.createMockPlugin()));

            assertEquals("War debt: owes 60 Corona", handler.infoLine("southreach"));
            assertEquals("War debt: is owed 60 Corona", handler.infoLine("northmarch"));
        } finally {
            MockBukkit.unmock();
        }
    }
}
