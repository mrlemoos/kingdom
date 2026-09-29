package dev.mrlemoos.kingdom.mint;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GiltOpenConfirmGui implements InventoryHolder {
    public static final int SLOT_CONFIRM = 13;
    private final String kingdomId;
    private Inventory inventory;
    private GiltOpenConfirmGui(String kingdomId) { this.kingdomId = kingdomId; }
    public String kingdomId() { return kingdomId; }
    public static GiltOpenConfirmGui create(String kingdomId, long closeDay) {
        GiltOpenConfirmGui gui = new GiltOpenConfirmGui(kingdomId);
        gui.inventory = Bukkit.createInventory(gui, 27, component("&6Confirm gilt sale"));
        gui.inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.EMERALD_BLOCK)
                .displayAs(c("&aOpen sale"))
                .lore(c("&7Closes by realm day " + closeDay))
                .lore(c("&cUnsold principal becomes your personal bond, free of charge."))
                .lore(c("&7The Crown owes its coupons and principal."))
                .build());
        return gui;
    }
    @Override public Inventory getInventory() { return inventory; }
}
