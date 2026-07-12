package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class KingdomInfoCommandTest {

    private ServerMock server;
    private KingdomService kingdomService;
    private WarService warService;
    private LoyaltyService loyaltyService;
    private KingdomCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("northumbria", "Northumbria");
        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.enabled());
        YamlKingdomStore store = new YamlKingdomStore(MockBukkit.createMockPlugin());
        command = new KingdomCommand(
                kingdomService,
                store,
                new NoblePrefixDisplay(kingdomService),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                warService,
                loyaltyService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void kingdomInfoShowsAtPeaceAndEmptyPoliceWhenQuiet() {
        PlayerMock viewer = server.addPlayer("Viewer");

        command.execute(viewer, new String[] {"info", "northmarch"});

        String messages = drainedMessages(viewer);
        assertTrue(messages.contains("War: at peace"), messages);
        assertTrue(messages.contains("Police: Constable none, Judge none, cells 0"), messages);
    }

    @Test
    void kingdomInfoShowsEnemyWarAndPoliceSummary() {
        warService.enactWarBill(
                "northmarch",
                new BillPayload.War("northumbria", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3));
        PlayerMock alice = server.addPlayer("Alice");
        kingdomService.getKingdom("northmarch").orElseThrow().getPoliceState().appointConstable(alice.getUniqueId());
        kingdomService.getKingdom("northmarch").orElseThrow().getPoliceState()
                .setCell(1, new PrisonCellLocation("world", 0, 64, 0));

        PlayerMock viewer = server.addPlayer("Viewer");
        command.execute(viewer, new String[] {"info", "northmarch"});

        String messages = drainedMessages(viewer);
        assertTrue(messages.contains("War: vs Northumbria (territory threshold)"), messages);
        assertTrue(messages.contains("Police: Constable Alice, Judge none, cells 1"), messages);
    }

    @Test
    void playerInfoShowsLoyaltyTier() {
        PlayerMock member = server.addPlayer("Citizen");
        kingdomService.joinKingdom(member.getUniqueId(), "northmarch");
        loyaltyService.recordActBreach(member.getUniqueId());

        PlayerMock viewer = server.addPlayer("Viewer");
        command.execute(viewer, new String[] {"info", "Citizen"});

        assertTrue(drainedMessages(viewer).contains("Loyalty: Doubtful"));
    }

    private static String drainedMessages(PlayerMock player) {
        StringBuilder joined = new StringBuilder();
        String next;
        while ((next = player.nextMessage()) != null) {
            if (joined.length() > 0) {
                joined.append('\n');
            }
            joined.append(next);
        }
        return strip(joined.toString());
    }
}
