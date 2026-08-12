package dev.mrlemoos.kingdom.city.gui;

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

/** The city hall counter: an applicant's standing and the single button that licenses them. */
public final class PermitApplyGui implements InventoryHolder {

    public static final Component TITLE = component("&6City Hall");

    static final int SLOT_STATUS = 11;
    static final int SLOT_APPLY = 15;

    private final String kingdomId;
    private Inventory inventory;

    public PermitApplyGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static PermitApplyGui create(
            String kingdomId, String kingdomName, PermitApplicantStatus status) {
        PermitApplyGui gui = new PermitApplyGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, kingdomName, status);
        return gui;
    }

    static void populate(Inventory inventory, String kingdomName, PermitApplicantStatus status) {
        inventory.clear();
        inventory.setItem(
                SLOT_STATUS,
                new ItemBuilder(Material.PAPER)
                        .displayAs(c("&6Lord Mayor of " + kingdomName))
                        .lore(c("&7" + statusLine(status)))
                        .lore(c("&7A build permit is free and kingdom-wide."))
                        .build());
        inventory.setItem(SLOT_APPLY, applyButton(status));
        fillBackground(inventory);
    }

    static String statusLine(PermitApplicantStatus status) {
        return switch (status) {
            case FOREIGNER -> "You are a foreigner here and may hold no permit.";
            case PRISONER -> "You are serving a prison sentence.";
            case EXEMPT -> "The Crown builds by right; you need no permit.";
            case LICENSED -> "You already hold a build permit.";
            case ELIGIBLE -> "You hold no build permit.";
        };
    }

    private static ItemStack applyButton(PermitApplicantStatus status) {
        Material material = status == PermitApplicantStatus.ELIGIBLE
                ? Material.WRITABLE_BOOK
                : Material.BARRIER;
        String name = status == PermitApplicantStatus.ELIGIBLE
                ? c("&aApply for build permit")
                : c("&cApply for build permit");
        return new ItemBuilder(material)
                .displayAs(name)
                .lore(c("&7" + statusLine(status)))
                .build();
    }

    public boolean isApplySlot(int slot) {
        return slot == SLOT_APPLY;
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
