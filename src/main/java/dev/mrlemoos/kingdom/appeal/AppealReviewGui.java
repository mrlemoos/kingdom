package dev.mrlemoos.kingdom.appeal;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AppealReviewGui implements InventoryHolder {
    public static final Component TITLE = component("&4Appeal to Crown");

    private final String kingdomId;
    private Inventory inventory;

    private AppealReviewGui(String kingdomId) {
        this.kingdomId = kingdomId;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static AppealReviewGui create(String kingdomId, String prisoner) {
        AppealReviewGui gui = new AppealReviewGui(kingdomId);
        gui.inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory.setItem(4, new ItemBuilder(Material.PAPER).displayAs(c("&6Prisoner: " + prisoner)).build());
        gui.inventory.setItem(19, new ItemBuilder(Material.RED_WOOL).displayAs(c("&cUphold")).build());
        gui.inventory.setItem(22, new ItemBuilder(Material.YELLOW_WOOL).displayAs(c("&eCommute")).build());
        gui.inventory.setItem(25, new ItemBuilder(Material.LIME_WOOL).displayAs(c("&aPardon")).build());
        return gui;
    }

    public Action action(int slot) {
        return switch (slot) {
            case 19 -> Action.UPHOLD;
            case 22 -> Action.COMMUTE;
            case 25 -> Action.PARDON;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public enum Action {
        UPHOLD,
        COMMUTE,
        PARDON
    }
}
