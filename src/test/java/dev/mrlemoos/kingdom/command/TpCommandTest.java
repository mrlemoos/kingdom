package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class TpCommandTest {

    private static final UUID SENDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private TpCommand tpCommand;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        KingdomService kingdomService = new KingdomService();
        TeleportService teleportService = new TeleportService(kingdomService);
        YamlKingdomStore store = new YamlKingdomStore(MockBukkit.createMockPlugin());
        KingdomTerritoryResolver territoryResolver = new KingdomTerritoryResolver(kingdomService);
        tpCommand = new TpCommand(teleportService, kingdomService, store, territoryResolver);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void hereTeleportsTargetToSenderLocation() {
        PlayerMock sender = playerWithPerms("Sender", TpCommandSuggestions.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Target");
        World world = sender.getWorld();
        sender.setLocation(new Location(world, 10, 64, 20, 90f, 0f));
        target.setLocation(new Location(world, 0, 64, 0, 0f, 0f));

        tpCommand.execute(sender, new String[] {"here", "Target"});

        target.assertTeleported(sender.getLocation(), 0.01);
    }

    @Test
    void atSelfAliasTeleportsTargetToSenderLocation() {
        PlayerMock sender = playerWithPerms("Sender", TpCommandSuggestions.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Target");
        World world = sender.getWorld();
        sender.setLocation(new Location(world, 5, 70, 15, 45f, 10f));
        target.setLocation(new Location(world, 100, 64, 100, 0f, 0f));

        tpCommand.execute(sender, new String[] {"@s", "Target"});

        target.assertTeleported(sender.getLocation(), 0.01);
    }

    @Test
    void hereRequiresTeleportPermission() {
        PlayerMock sender = new PlayerMock(server, "Sender", SENDER_ID);
        server.addPlayer("Target");

        tpCommand.execute(sender, new String[] {"here", "Target"});

        assertTrue(sender.nextMessage().contains("permission"));
    }

    @Test
    void hereRequiresPlayerSender() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        server.addPlayer("Target");

        tpCommand.execute(console, new String[] {"here", "Target"});

        assertTrue(console.nextMessage().contains("player"));
    }

    @Test
    void hereRejectsUnknownTarget() {
        PlayerMock sender = playerWithPerms("Sender", TpCommandSuggestions.PERM_TELEPORT);

        tpCommand.execute(sender, new String[] {"here", "Nobody"});

        assertTrue(sender.nextMessage().contains("Unknown"));
    }

    @Test
    void hereNotifiesOtherStaffWithTeleportPermission() {
        PlayerMock actor = addPlayerWithPerms("Alice", SENDER_ID, TpCommandSuggestions.PERM_TELEPORT);
        PlayerMock observer = addPlayerWithPerms(
                "Staff", UUID.fromString("00000000-0000-0000-0000-000000000002"), TpCommandSuggestions.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Target");
        World world = actor.getWorld();
        actor.setLocation(new Location(world, 10, 64, 20, 90f, 0f));
        target.setLocation(new Location(world, 0, 64, 0, 0f, 0f));

        tpCommand.execute(actor, new String[] {"here", "Target"});

        target.assertTeleported(actor.getLocation(), 0.01);
        assertTrue(actor.nextMessage().contains("Teleported Target here."));
        assertTrue(observer.nextMessage().contains("Alice teleported Target to 10 64 20."));
    }

    private PlayerMock addPlayerWithPerms(String name, UUID id, String... permissions) {
        PlayerMock player = new PlayerMock(server, name, id) {
            @Override
            public boolean hasPermission(String permission) {
                for (String granted : permissions) {
                    if (granted.equals(permission)) {
                        return true;
                    }
                }
                return super.hasPermission(permission);
            }
        };
        server.addPlayer(player);
        return player;
    }

    private PlayerMock playerWithPerms(String name, String... permissions) {
        return new PlayerMock(server, name, SENDER_ID) {
            @Override
            public boolean hasPermission(String permission) {
                for (String granted : permissions) {
                    if (granted.equals(permission)) {
                        return true;
                    }
                }
                return super.hasPermission(permission);
            }
        };
    }
}
