package dev.mrlemoos.kingdom.war.muster.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** One-choice levy response. The service remains authority for every gate. */
public final class MusterGui implements InventoryHolder {

    public static final int ANSWER_SLOT = 3;
    public static final int REFUSE_SLOT = 5;
    private final String warId;
    private final Inventory inventory;

    private MusterGui(String warId) {
        this.warId = warId;
        inventory = Bukkit.createInventory(this, 9, component("&6Muster"));
        inventory.setItem(ANSWER_SLOT, ItemBuilder.labelled(Material.LIME_WOOL, c("&aAnswer Muster"), "Stand ready with the levy."));
        inventory.setItem(REFUSE_SLOT, ItemBuilder.labelled(Material.RED_WOOL, c("&cRefuse Muster"), "Levy morale falls to Shaken."));
    }

    public static MusterGui create(String warId) { return new MusterGui(warId); }
    public String warId() { return warId; }
    @Override public Inventory getInventory() { return inventory; }
}
