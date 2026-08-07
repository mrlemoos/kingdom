package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.GolemOrder;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class PoliceGolemOrderGui implements InventoryHolder {

    public static final Component TITLE = component("&9Constable Orders");

    public static final int SLOT_FOLLOW = 2;
    public static final int SLOT_STAY = 4;
    public static final int SLOT_PATROL = 6;

    private final UUID golemId;
    private Inventory inventory;

    public PoliceGolemOrderGui(UUID golemId) {
        this.golemId = golemId;
    }

    public UUID golemId() {
        return golemId;
    }

    public static boolean canCommand(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }

    public static PoliceGolemOrderGui create(UUID golemId, GolemOrder current) {
        PoliceGolemOrderGui gui = new PoliceGolemOrderGui(golemId);
        Inventory inventory = Bukkit.createInventory(gui, 9, TITLE);
        gui.inventory = inventory;

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

    public GolemOrder orderForSlot(int slot) {
        return switch (slot) {
            case SLOT_FOLLOW -> GolemOrder.FOLLOW;
            case SLOT_STAY -> GolemOrder.STAY;
            case SLOT_PATROL -> GolemOrder.PATROL;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static String label(String name, boolean active) {
        return c(active ? "&a" : "&f") + name + (active ? c(" &8(current)") : "");
    }
}
