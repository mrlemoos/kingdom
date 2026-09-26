package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.GolemOfficerKind;
import dev.mrlemoos.kingdom.model.police.GolemOrder;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class PoliceGolemOrderGui implements InventoryHolder {

    public static final Component TITLE = component("&9Constable Orders");

    public static final int SLOT_FOLLOW = 1;
    public static final int SLOT_STAY = 2;
    public static final int SLOT_PATROL = 3;
    /** Posts a patrol golem as a guard, or sends a guard out on patrol. */
    public static final int SLOT_KIND = 5;
    public static final int SLOT_STAND_DOWN = 7;

    private final UUID golemId;
    private final GolemOfficerKind kind;
    private Inventory inventory;

    public PoliceGolemOrderGui(UUID golemId) {
        this(golemId, GolemOfficerKind.PATROL);
    }

    public PoliceGolemOrderGui(UUID golemId, GolemOfficerKind kind) {
        this.golemId = golemId;
        this.kind = kind == null ? GolemOfficerKind.PATROL : kind;
    }

    public GolemOfficerKind kind() {
        return kind;
    }

    public UUID golemId() {
        return golemId;
    }

    public static boolean canCommand(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }

    public static PoliceGolemOrderGui create(UUID golemId, GolemOfficerKind kind, GolemOrder current) {
        PoliceGolemOrderGui gui = new PoliceGolemOrderGui(golemId, kind);
        Inventory inventory = Bukkit.createInventory(gui, 9, TITLE);
        gui.inventory = inventory;

        inventory.setItem(
                SLOT_KIND,
                gui.kind == GolemOfficerKind.GUARD
                        ? ItemBuilder.labelled(Material.COMPASS, c("&fSend on patrol"), "The guard leaves its post to walk the beat")
                        : ItemBuilder.labelled(Material.SHIELD, c("&fPost as a guard"), "The constable stands guard here"));
        inventory.setItem(
                SLOT_STAND_DOWN,
                ItemBuilder.labelled(
                        Material.BARRIER, c("&cStand down"), "The constable leaves the watch for good"));
        if (gui.kind == GolemOfficerKind.GUARD) {
            return gui;
        }
        inventory.setItem(
                SLOT_FOLLOW,
                ItemBuilder.labelled(
                        Material.LEAD,
                        label("Follow me", current == GolemOrder.FOLLOW),
                        "The constable escorts you"));
        inventory.setItem(
                SLOT_STAY,
                ItemBuilder.labelled(
                        Material.IRON_BLOCK, label("Stay here", current == GolemOrder.STAY), "The constable holds post"));
        inventory.setItem(
                SLOT_PATROL,
                ItemBuilder.labelled(
                        Material.COMPASS,
                        label("Patrol", current == GolemOrder.PATROL),
                        "The constable walks the beat"));
        return gui;
    }

    /** The order a slot gives; null for anything else, and always null for a guard, which holds its post. */
    public GolemOrder orderForSlot(int slot) {
        if (kind == GolemOfficerKind.GUARD) {
            return null;
        }
        return switch (slot) {
            case SLOT_FOLLOW -> GolemOrder.FOLLOW;
            case SLOT_STAY -> GolemOrder.STAY;
            case SLOT_PATROL -> GolemOrder.PATROL;
            default -> null;
        };
    }

    /** The kind a golem becomes when its kind slot is clicked, if {@code slot} is that slot. */
    public GolemOfficerKind kindForSlot(int slot) {
        if (slot != SLOT_KIND) {
            return null;
        }
        return kind == GolemOfficerKind.GUARD ? GolemOfficerKind.PATROL : GolemOfficerKind.GUARD;
    }

    public static boolean isStandDown(int slot) {
        return slot == SLOT_STAND_DOWN;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static String label(String name, boolean active) {
        return c(active ? "&a" : "&f") + name + (active ? c(" &8(current)") : "");
    }
}
