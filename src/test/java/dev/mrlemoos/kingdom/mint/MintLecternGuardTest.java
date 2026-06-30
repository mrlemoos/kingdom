package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class MintLecternGuardTest {

    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void releasesClaimWhenJobSiteMatchesMintLectern() {
        Location jobSite = new Location(world, 10, 64, 20);

        assertTrue(MintLecternGuard.shouldReleaseClaim(
                false, jobSite, null, List.of(new MintLocation("world", 10, 64, 20))));
    }

    @Test
    void releasesClaimWhenPotentialJobSiteMatchesMintLectern() {
        Location potential = new Location(world, 5, 70, 5);

        assertTrue(MintLecternGuard.shouldReleaseClaim(
                false, null, potential, List.of(new MintLocation("world", 5, 70, 5))));
    }

    @Test
    void retainsClaimForTreasuryLord() {
        Location jobSite = new Location(world, 10, 64, 20);

        assertFalse(MintLecternGuard.shouldReleaseClaim(
                true, jobSite, null, List.of(new MintLocation("world", 10, 64, 20))));
    }

    @Test
    void ignoresUnrelatedJobSite() {
        Location jobSite = new Location(world, 1, 64, 1);

        assertFalse(MintLecternGuard.shouldReleaseClaim(
                false, jobSite, null, List.of(new MintLocation("world", 10, 64, 20))));
    }

    @Test
    void releaseClaim_clearsMemoriesAndResetsLibrarianProfession() {
        Villager villager = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        Location lectern = new Location(world, 10, 64, 20);
        villager.setMemory(MemoryKey.JOB_SITE, lectern);
        villager.setMemory(MemoryKey.POTENTIAL_JOB_SITE, lectern);
        villager.setProfession(Villager.Profession.LIBRARIAN);

        MintLecternGuard.releaseClaim(villager, false);

        assertNull(villager.getMemory(MemoryKey.JOB_SITE));
        assertNull(villager.getMemory(MemoryKey.POTENTIAL_JOB_SITE));
        assertEquals(Villager.Profession.NONE, villager.getProfession());
    }

    @Test
    void releaseClaim_preservesCartographerProfessionForTreasuryLord() {
        Villager villager = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        Location lectern = new Location(world, 10, 64, 20);
        villager.setMemory(MemoryKey.JOB_SITE, lectern);
        TreasuryLordAppearance.apply(villager);

        MintLecternGuard.releaseClaim(villager, true);

        assertNull(villager.getMemory(MemoryKey.JOB_SITE));
        assertEquals(TreasuryLordAppearance.PROFESSION, villager.getProfession());
    }

    @Test
    void releaseClaim_preservesUnrelatedCartographerProfession() {
        Villager villager = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        Location lectern = new Location(world, 10, 64, 20);
        villager.setMemory(MemoryKey.POTENTIAL_JOB_SITE, lectern);
        villager.setProfession(Villager.Profession.CARTOGRAPHER);

        MintLecternGuard.releaseClaim(villager, false);

        assertNull(villager.getMemory(MemoryKey.POTENTIAL_JOB_SITE));
        assertEquals(Villager.Profession.CARTOGRAPHER, villager.getProfession());
    }

    @Test
    void releaseClaim_resetsCartographerEmployedAtLecternWhenNotTreasuryLord() {
        Villager villager = (Villager) world.spawnEntity(world.getSpawnLocation(), EntityType.VILLAGER);
        Location lectern = new Location(world, 10, 64, 20);
        villager.setMemory(MemoryKey.JOB_SITE, lectern);
        villager.setProfession(Villager.Profession.CARTOGRAPHER);

        MintLecternGuard.releaseClaim(villager, false);

        assertNull(villager.getMemory(MemoryKey.JOB_SITE));
        assertEquals(Villager.Profession.NONE, villager.getProfession());
    }
}
