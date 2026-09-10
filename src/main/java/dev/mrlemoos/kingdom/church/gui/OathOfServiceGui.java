package dev.mrlemoos.kingdom.church.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The cleric's oath book. */
public final class OathOfServiceGui implements InventoryHolder {

    public static final Component TITLE = component("&6Oath of Service");
    public static final int SLOT_SWEAR = 13;
    public static final int SLOT_LEAVE = 15;

    private final String kingdomId;
    private Inventory inventory;

    private OathOfServiceGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static OathOfServiceGui create(String kingdomId, String realmName, boolean member) {
        OathOfServiceGui gui = new OathOfServiceGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(
                SLOT_SWEAR,
                new ItemBuilder(Material.IRON_SWORD)
                        .displayAs(c("&aSwear the oath"))
                        .lore(c("&7" + realmName))
                        .lore(c(member
                                ? "&7Open your military morale before the cleric."
                                : "&7Pledge service to this realm as an outsider."))
                        .build());
        inventory.setItem(SLOT_LEAVE, new ItemBuilder(Material.BARRIER).displayAs(c("&cLeave the church")).build());
        fillBackground(inventory);
        return gui;
    }

    public boolean isSwearSlot(int slot) {
        return slot == SLOT_SWEAR;
    }

    public boolean isLeaveSlot(int slot) {
        return slot == SLOT_LEAVE;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
