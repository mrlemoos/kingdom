package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
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
}
