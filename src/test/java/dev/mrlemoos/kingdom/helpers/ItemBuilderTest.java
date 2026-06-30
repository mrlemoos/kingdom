package dev.mrlemoos.kingdom.helpers;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class ItemBuilderTest {

    private static final LegacyComponentSerializer LEGACY_SECTION =
            LegacyComponentSerializer.legacySection();

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
    void buildAppliesDisplayNameAndLore() {
        ItemStack stack = new ItemBuilder(Material.PAPER)
                .displayAs(c("&6Test"))
                .lore(c("&7Line one"), c("&7Line two"))
                .amount(3)
                .build();

        assertEquals(Material.PAPER, stack.getType());
        assertEquals(3, stack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals(LEGACY_SECTION.deserialize(c("&6Test")), meta.displayName());
        assertEquals(
                List.of(LEGACY_SECTION.deserialize(c("&7Line one")), LEGACY_SECTION.deserialize(c("&7Line two"))),
                meta.lore());
    }

    @Test
    void buildAppliesPersistentData() {
        NamespacedKey markerKey = new NamespacedKey("kingdom", "marker");
        NamespacedKey kingdomKey = new NamespacedKey("kingdom", "kingdom_id");

        ItemStack stack = new ItemBuilder(Material.PAPER)
                .pdc(markerKey, PersistentDataType.BYTE, (byte) 1)
                .pdc(kingdomKey, PersistentDataType.STRING, "northmarch")
                .build();

        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals((byte) 1, meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE));
        assertEquals("northmarch", meta.getPersistentDataContainer().get(kingdomKey, PersistentDataType.STRING));
    }

    @Test
    void bookSetsTitleAuthorPages() {
        List<String> pages = List.of("Act I", "Act II");
        ItemStack stack = new ItemBuilder(Material.WRITTEN_BOOK)
                .book("Budget Bill", "Parliament", pages)
                .build();

        BookMeta meta = (BookMeta) stack.getItemMeta();
        assertNotNull(meta);
        assertEquals("Budget Bill", meta.getTitle());
        assertEquals("Parliament", meta.getAuthor());
        assertEquals(
                pages.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList(),
                meta.pages());
    }

    @Test
    void skullOwnerSetsOwningPlayer() {
        var player = server.addPlayer("Alice");
        UUID ownerId = player.getUniqueId();

        ItemStack stack = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(ownerId)
                .displayAs(c("&fAlice"))
                .build();

        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        assertNotNull(meta);
        assertNotNull(meta.getOwningPlayer());
        assertEquals("Alice", meta.getOwningPlayer().getName());
        assertEquals(LEGACY_SECTION.deserialize(c("&fAlice")), meta.displayName());
    }

    @Test
    void labelledPrefixesLoreWithGray() {
        ItemStack stack = ItemBuilder.labelled(Material.EMERALD, c("&aAction"), "Do the thing");

        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals(LEGACY_SECTION.deserialize(c("&aAction")), meta.displayName());
        assertEquals(List.of(LEGACY_SECTION.deserialize(c("&7Do the thing"))), meta.lore());
    }

    @Test
    void fillerPaneUsesBlankDisplayName() {
        ItemStack stack = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, stack.getType());
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals(LEGACY_SECTION.deserialize(" "), meta.displayName());
    }

    @Test
    void durabilitySetsDamageOnDamageableItem() {
        ItemStack stack = new ItemBuilder(Material.DIAMOND_SWORD)
                .durability(42)
                .build();

        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        Damageable damageable = assertInstanceOf(Damageable.class, meta);
        assertEquals(42, damageable.getDamage());
    }

    @Test
    void typeSwapsMaterialWhilePreservingMeta() {
        ItemStack stack = new ItemBuilder(Material.GOLD_NUGGET)
                .displayAs(c("&aWithdraw"))
                .type(Material.GRAY_DYE)
                .build();

        assertEquals(Material.GRAY_DYE, stack.getType());
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertEquals(LEGACY_SECTION.deserialize(c("&aWithdraw")), meta.displayName());
    }

    @Test
    void enchantAppliesEnchantment() {
        ItemStack stack = new ItemBuilder(Material.DIAMOND_SWORD)
                .enchant(new EnchantmentBuilder(Enchantment.SHARPNESS, 5))
                .build();

        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.hasEnchant(Enchantment.SHARPNESS));
        assertEquals(5, meta.getEnchantLevel(Enchantment.SHARPNESS));
    }
}
