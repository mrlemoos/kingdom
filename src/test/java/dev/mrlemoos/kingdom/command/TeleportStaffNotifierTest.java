package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class TeleportStaffNotifierTest {

    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OBSERVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void notifiesOtherPlayersWithTeleportPermission() {
        PlayerMock actor = addPlayerWithPerms("Alice", ACTOR_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock observer = addPlayerWithPerms("Staff", OBSERVER_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Bob");

        TeleportStaffNotifier.notifyCrossPlayerTeleport(actor, target, "Charlie");

        assertNull(actor.nextMessage());
        assertTrue(observer.nextMessage().contains("Alice teleported Bob to Charlie."));
        assertNull(target.nextMessage());
    }

    @Test
    void skipsPlayersWithoutTeleportPermission() {
        PlayerMock actor = addPlayerWithPerms("Alice", ACTOR_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock observer = server.addPlayer("Citizen");
        server.addPlayer("Bob");

        TeleportStaffNotifier.notifyCrossPlayerTeleport(actor, server.getPlayer("Bob"), "spawn");

        assertNull(observer.nextMessage());
    }

    @Test
    void consoleActorNotifiesAllPermittedPlayers() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        PlayerMock observer = addPlayerWithPerms("Staff", OBSERVER_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Bob");

        TeleportStaffNotifier.notifyCrossPlayerTeleport(console, target, "Northmarch checkpoint spawn");

        assertTrue(observer.nextMessage().contains("Console teleported Bob to Northmarch checkpoint spawn."));
    }

    @Test
    void formatsCoordinateDestination() {
        PlayerMock actor = addPlayerWithPerms("Alice", ACTOR_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock observer = addPlayerWithPerms("Staff", OBSERVER_ID, TeleportStaffNotifier.PERM_TELEPORT);
        PlayerMock target = server.addPlayer("Bob");
        World world = actor.getWorld();
        Location location = new Location(world, 10, 64, -20, 0f, 0f);

        TeleportStaffNotifier.notifyCrossPlayerTeleport(actor, target, TeleportStaffNotifier.formatCoordinates(location));

        assertTrue(observer.nextMessage().contains("Alice teleported Bob to 10 64 -20."));
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
}
