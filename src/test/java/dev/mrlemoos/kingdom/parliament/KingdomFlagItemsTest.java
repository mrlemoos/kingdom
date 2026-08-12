package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import java.util.Optional;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class KingdomFlagItemsTest {

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
    void readsBaseColourAndLoomLayersFromHeldBanner() {
        ItemStack stack = new ItemStack(Material.RED_BANNER);
        BannerMeta meta = (BannerMeta) stack.getItemMeta();
        meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
        meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
        stack.setItemMeta(meta);

        Optional<KingdomFlag> flag = KingdomFlagItems.fromItem(stack);

        assertTrue(flag.isPresent());
        assertEquals("RED_BANNER", flag.get().baseMaterial());
        assertEquals(2, flag.get().layers().size());
        assertEquals(PatternType.STRIPE_MIDDLE.getKey().toString(), flag.get().layers().get(0).patternId());
        assertEquals("WHITE", flag.get().layers().get(0).colour());
        assertEquals(PatternType.BORDER.getKey().toString(), flag.get().layers().get(1).patternId());
        assertEquals("BLACK", flag.get().layers().get(1).colour());
    }

    @Test
    void ignoresNonBannerItems() {
        assertTrue(KingdomFlagItems.fromItem(new ItemStack(Material.STONE)).isEmpty());
        assertTrue(KingdomFlagItems.fromItem(null).isEmpty());
    }
}
