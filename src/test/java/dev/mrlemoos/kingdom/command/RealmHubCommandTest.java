package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.hub.RealmHubSnapshotFactory;
import dev.mrlemoos.kingdom.hub.gui.RealmHubGui;
import dev.mrlemoos.kingdom.listener.RealmHubListener;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class RealmHubCommandTest {

    private ServerMock server;
    private KingdomService kingdomService;
    private KingdomCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        YamlKingdomStore store = new YamlKingdomStore(MockBukkit.createMockPlugin());
        command = new KingdomCommand(kingdomService, store, new NoblePrefixDisplay(kingdomService));
        RealmHubListener hub =
                new RealmHubListener(kingdomService, new RealmHubSnapshotFactory(kingdomService, null));
        command.setRealmHubOpener(hub::openHub);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aBareKingdomOpensTheRealmHubForAPlayer() {
        PlayerMock subject = server.addPlayer("Subject");
        kingdomService.joinKingdom(subject.getUniqueId(), "northmarch");

        command.execute(subject, new String[0]);

        InventoryHolder holder = subject.getOpenInventory().getTopInventory().getHolder();
        assertInstanceOf(RealmHubGui.class, holder);
        assertTrue(drainedMessages(subject).isBlank(), "the hub speaks through the screen, not the chat");
    }

    @Test
    void aSubjectSwornToNoRealmStillGetsTheHub() {
        PlayerMock stranger = server.addPlayer("Stranger");

        command.execute(stranger, new String[0]);

        assertInstanceOf(RealmHubGui.class, stranger.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void theConsoleKeepsTheWrittenHelp() {
        ConsoleCommandSenderMock console = server.getConsoleSender();

        command.execute(console, new String[0]);

        StringBuilder joined = new StringBuilder();
        String next;
        while ((next = console.nextMessage()) != null) {
            joined.append(next).append('\n');
        }
        String messages = strip(joined.toString());
        assertTrue(messages.contains("Kingdom commands:"), messages);
        assertTrue(messages.contains("/kingdom list"), messages);
    }

    @Test
    void anUnknownOrderIsRefusedByNameBeforeTheHelp() {
        PlayerMock subject = server.addPlayer("Subject");

        command.execute(subject, new String[] {"parliment"});

        String messages = drainedMessages(subject);
        assertTrue(messages.contains(UnknownOrderRefusal.refusal("parliment")), messages);
        assertTrue(messages.contains(UnknownOrderRefusal.HUB_POINTER), messages);
        assertTrue(messages.contains("Kingdom commands:"), messages);
    }

    @Test
    void aKnownOrderIsNeverRefused() {
        PlayerMock subject = server.addPlayer("Subject");

        command.execute(subject, new String[] {"list"});

        assertFalse(drainedMessages(subject).contains("knows no such order"));
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
