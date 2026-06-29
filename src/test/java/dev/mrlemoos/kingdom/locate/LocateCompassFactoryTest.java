package dev.mrlemoos.kingdom.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class LocateCompassFactoryTest {

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
    void buildsUntrackedLodestoneCompassWithLabelAndCoords() {
        World world = server.addSimpleWorld("world");
        Location target = new Location(world, 120, 64, -30);

        ItemStack compass = LocateCompassFactory.create(target, "mob_farm");

        assertEquals(Material.COMPASS, compass.getType());
        assertTrue(compass.getItemMeta() instanceof CompassMeta meta);
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        assertTrue(compassMeta.hasLodestone());
        assertFalse(compassMeta.isLodestoneTracked());
        assertEquals(target.getBlockX(), compassMeta.getLodestone().getBlockX());
        assertEquals("Kingdom compass", plain(compassMeta.displayName()));
        assertNotNull(compassMeta.lore());
        assertEquals(2, compassMeta.lore().size());
        assertEquals("→ mob_farm", plain(compassMeta.lore().getFirst()));
        assertEquals("120, 64, -30", plain(compassMeta.lore().get(1)));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
