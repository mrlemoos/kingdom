package dev.mrlemoos.kingdom.honours;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * The Crown's honours list: the titles a monarch may bestow with a golden sword in hand, and beneath
 * them the sworn roles — constable, judge and priest — sworn and unsworn in the same window.
 */
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
    /** The sworn-roles row: constable, judge, priest, in {@link SwornRole} order. */
    public static final int FIRST_SWORN_SLOT = 30;

    private final UUID targetId;
    private Inventory inventory;

    private HonoursGui(UUID targetId) {
        this.targetId = Objects.requireNonNull(targetId, "targetId");
    }

    public UUID targetId() {
        return targetId;
    }

    public static HonoursGui create(
            UUID targetId, String targetName, NobleRank currentRank, Set<SwornRole> swornRoles) {
        HonoursGui gui = new HonoursGui(targetId);
        Inventory inventory = Bukkit.createInventory(gui, 36, TITLE);
        gui.inventory = inventory;
        populate(inventory, targetName, currentRank, swornRoles);
        return gui;
    }

    public static void populate(
            Inventory inventory, String targetName, NobleRank currentRank, Set<SwornRole> swornRoles) {
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
        SwornRole[] roles = SwornRole.values();
        for (int index = 0; index < roles.length; index++) {
            SwornRole role = roles[index];
            String label = SwornRoleAppointments.label(role);
            boolean held = swornRoles != null && swornRoles.contains(role);
            inventory.setItem(
                    FIRST_SWORN_SLOT + index,
                    new ItemBuilder(materialOf(role))
                            .displayAs(c("&b" + label))
                            .lore(c(held ? "&aSworn." : "&7Not sworn."))
                            .lore(c(held
                                    ? "&7Click: release " + targetName + " from the office."
                                    : "&7Click: swear " + targetName + " as " + label + "."))
                            .lore(c("&8" + exclusionOf(role)))
                            .build());
        }
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

    /** The sworn role toggled at this slot, or null when the slot swears nothing. */
    public static SwornRole swornRoleForSlot(int slot) {
        int index = slot - FIRST_SWORN_SLOT;
        SwornRole[] roles = SwornRole.values();
        if (index < 0 || index >= roles.length) {
            return null;
        }
        return roles[index];
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

    private static Material materialOf(SwornRole role) {
        return switch (role) {
            case CONSTABLE -> Material.SHIELD;
            case JUDGE -> Material.LECTERN;
            case PRIEST -> Material.CANDLE;
        };
    }

    private static String exclusionOf(SwornRole role) {
        return switch (role) {
            case CONSTABLE -> "Not with judge or priest.";
            case JUDGE -> "Not with constable or priest.";
            case PRIEST -> "One to a realm; not with constable or judge.";
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
