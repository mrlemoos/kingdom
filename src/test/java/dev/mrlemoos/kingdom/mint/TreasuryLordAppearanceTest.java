package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class TreasuryLordAppearanceTest {

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
    void appliesCartographerMonocleAppearance() {
        World world = server.addSimpleWorld("world");
        Villager villager = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);

        TreasuryLordAppearance.apply(villager);

        assertEquals(Villager.Profession.CARTOGRAPHER, villager.getProfession());
        assertEquals(Villager.Type.PLAINS, villager.getVillagerType());
        assertEquals(1, villager.getVillagerExperience());
        assertEquals(1, villager.getVillagerLevel());
        assertEquals(0, villager.getRecipes().size());
    }

    @Test
    void professionIsCartographer() {
        assertEquals(Villager.Profession.CARTOGRAPHER, TreasuryLordAppearance.PROFESSION);
    }
}
