package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.VillagerMock;

class VillagerPlayerTradePolicyTest {

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
    void unemployedVillagerCannotTradeWithPlayers() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.NONE);

        assertFalse(VillagerPlayerTradePolicy.canTradeWithPlayers(villager));
    }

    @Test
    void employedVillagerCanTradeWithPlayers() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.FARMER);

        assertTrue(VillagerPlayerTradePolicy.canTradeWithPlayers(villager));
    }
}
