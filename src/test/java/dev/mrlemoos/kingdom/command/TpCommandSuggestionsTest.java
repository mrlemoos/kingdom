package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class TpCommandSuggestionsTest {

    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private KingdomService kingdomService;
    private TeleportService teleportService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(MEMBER_ID, "northmarch");
        teleportService = new TeleportService(kingdomService);
        teleportService.createPlace(
                "northmarch", TeleportPlace.of("mob_farm", "world", 1, 64, 1, 0, 0));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void firstArgIncludesPlayersAndMemberCheckpoints() {
        server.addPlayer("Alice");
        server.addPlayer("Bob");
        PlayerMock member = playerWithPerms(
                "Member", TpCommandSuggestions.PERM_TELEPORT, TpCommandSuggestions.PERM_CHECKPOINT);

        List<String> suggestions =
                TpCommandSuggestions.suggest(member, new String[] {""}, kingdomService, teleportService);

        Set<String> values = Set.copyOf(suggestions);
        assertTrue(values.contains("Alice"));
        assertTrue(values.contains("Bob"));
        assertTrue(values.contains("mob_farm"));
    }

    @Test
    void secondArgIncludesPlayersAndCheckpointsForTeleportTarget() {
        PlayerMock member = playerWithPerms(
                "Member", TpCommandSuggestions.PERM_TELEPORT, TpCommandSuggestions.PERM_CHECKPOINT);
        server.addPlayer("Bob");

        List<String> suggestions = TpCommandSuggestions.suggest(
                member, new String[] {"Member", ""}, kingdomService, teleportService);

        Set<String> values = Set.copyOf(suggestions);
        assertTrue(values.contains("Bob"));
        assertTrue(values.contains("mob_farm"));
    }

    @Test
    void opCheckpointSubcommandSuggestions() {
        List<String> suggestions = TpCommandSuggestions.suggest(
                opSender(), new String[] {"checkpoint", "cre"}, kingdomService, teleportService);

        assertTrue(suggestions.contains("create"));
    }

    @Test
    void filtersByPrefix() {
        server.addPlayer("Steve");
        server.addPlayer("Stella");
        PlayerMock member = playerWithPerms("Member", TpCommandSuggestions.PERM_TELEPORT);

        List<String> suggestions =
                TpCommandSuggestions.suggest(member, new String[] {"ste"}, kingdomService, teleportService);

        Set<String> names = suggestions.stream()
                .filter(name -> name.toLowerCase().startsWith("ste"))
                .collect(Collectors.toSet());
        assertTrue(names.contains("Steve"));
        assertTrue(names.contains("Stella"));
    }

    @Test
    void trailingSpaceStartsNextArgumentSuggestions() {
        List<String> suggestions = TpCommandSuggestions.suggest(
                opSender(),
                TpCommandSuggestions.argsForSuggest("checkpoint create "),
                kingdomService,
                teleportService);

        assertTrue(suggestions.contains("northmarch"));
    }

    private PlayerMock playerWithPerms(String name, String... permissions) {
        PlayerMock player = new PlayerMock(server, name, MEMBER_ID) {
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
        return player;
    }

    private CommandSender opSender() {
        return new ConsoleCommandSenderMock();
    }
}
