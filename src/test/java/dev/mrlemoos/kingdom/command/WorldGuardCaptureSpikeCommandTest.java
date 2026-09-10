package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class WorldGuardCaptureSpikeCommandTest {

    private ServerMock server;
    private KingdomService kingdoms;
    private WorldGuardCaptureSpikeCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        kingdoms = new KingdomService();
        kingdoms.createKingdom("spike-attack", "Spike attack");
        kingdoms.createKingdom("spike-defend", "Spike defend");
        kingdoms.getKingdom("spike-attack").orElseThrow().setWorldGuardRegion("kingdom-spike-attack");
        kingdoms.getKingdom("spike-defend").orElseThrow().setWorldGuardRegion("kingdom-spike-defend");
        kingdoms.getKingdom("spike-attack").orElseThrow().setWorldName("world");
        kingdoms.getKingdom("spike-defend").orElseThrow().setWorldName("world");
        command = new WorldGuardCaptureSpikeCommand(kingdoms, ignored -> List.of("kingdom-spike-defend"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void plansThreeSandboxChunksWithoutMutatingWorldGuard() {
        PlayerMock operator = server.addPlayer("Operator");
        operator.setOp(true);

        command.execute(operator, new String[] {"capture", "spike-attack", "spike-defend"});
        operator.teleport(operator.getLocation().add(16, 0, 0));
        command.execute(operator, new String[] {"capture", "spike-attack", "spike-defend"});
        operator.teleport(operator.getLocation().add(16, 0, 0));
        command.execute(operator, new String[] {"capture", "spike-attack", "spike-defend"});
        command.execute(operator, new String[] {"plan", "spike-attack", "spike-defend"});

        assertTrue(messages(operator).contains("No WorldGuard region changed."));
    }

    @Test
    void refusesNonSandboxKingdoms() {
        kingdoms.createKingdom("northmarch", "Northmarch");
        PlayerMock operator = server.addPlayer("Operator");
        operator.setOp(true);

        command.execute(operator, new String[] {"capture", "northmarch", "spike-defend"});

        assertTrue(messages(operator).contains("Both kingdoms must be linked spike-* kingdoms"));
    }

    private static String messages(PlayerMock player) {
        StringBuilder messages = new StringBuilder();
        String message;
        while ((message = player.nextMessage()) != null) {
            messages.append(strip(message));
        }
        return messages.toString();
    }
}
