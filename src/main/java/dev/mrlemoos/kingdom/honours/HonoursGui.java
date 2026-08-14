package dev.mrlemoos.kingdom.honours;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** The Crown's honours list: the titles a monarch may bestow with a golden sword in hand. */
public final class HonoursGui implements InventoryHolder {

    public static final Component TITLE = component("&6Honours of the Crown");

    /** The Crown's own titles are never in its gift, and MP seats are filled by election. */
    public static final List<NobleRank> GRANTABLE = List.of(
            NobleRank.PRINCE,
            NobleRank.SPEAKER,
            NobleRank.DUKE,
            NobleRank.LORD,
            NobleRank.COUNT,
            NobleRank.KNIGHT);

    static final int FIRST_SLOT = 10;
    public static final int SLOT_STRIP = 22;

    private final UUID targetId;
    private Inventory inventory;

    private HonoursGui(UUID targetId) {
        this.targetId = Objects.requireNonNull(targetId, "targetId");
    }

    public UUID targetId() {
        return targetId;
    }

    public static HonoursGui create(UUID targetId, String targetName, NobleRank currentRank) {
        HonoursGui gui = new HonoursGui(targetId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, targetName, currentRank);
        return gui;
    }

    static void populate(Inventory inventory, String targetName, NobleRank currentRank) {
        inventory.clear();
        for (int index = 0; index < GRANTABLE.size(); index++) {
            NobleRank rank = GRANTABLE.get(index);
            inventory.setItem(
                    FIRST_SLOT + index,
                    new ItemBuilder(materialOf(rank))
                            .displayAs(rank.chatColor() + rank.displayTitle(TitleStyle.MASCULINE))
                            .lore(c("&7Bestow upon " + targetName + "."))
                            .lore(c(rank == currentRank ? "&aHeld already." : "&7Not held."))
                            .lore(c("&7Left-click: " + rank.displayTitle(TitleStyle.MASCULINE)))
                            .lore(c("&7Right-click: " + rank.displayTitle(TitleStyle.FEMININE)))
                            .build());
        }
        inventory.setItem(
                SLOT_STRIP,
                new ItemBuilder(Material.BARRIER)
                        .displayAs(c("&cStrip of title"))
                        .lore(c("&7Return " + targetName + " to the commons."))
                        .build());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    /** The rank offered at this slot, or null when the slot bestows nothing. */
    public static NobleRank rankForSlot(int slot) {
        int index = slot - FIRST_SLOT;
        if (index < 0 || index >= GRANTABLE.size()) {
            return null;
        }
        return GRANTABLE.get(index);
    }

    public static boolean isStripSlot(int slot) {
        return slot == SLOT_STRIP;
    }

    private static Material materialOf(NobleRank rank) {
        return switch (rank) {
            case PRINCE -> Material.GOLDEN_HELMET;
            case SPEAKER -> Material.BELL;
            case DUKE -> Material.DIAMOND;
            case LORD -> Material.AMETHYST_SHARD;
            case COUNT -> Material.EMERALD;
            case KNIGHT -> Material.IRON_SWORD;
            default -> Material.PAPER;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
