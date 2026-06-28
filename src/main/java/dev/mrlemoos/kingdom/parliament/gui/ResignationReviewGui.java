package dev.mrlemoos.kingdom.parliament.gui;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class ResignationReviewGui implements InventoryHolder {

    public static final String TITLE = ChatColor.DARK_RED + "Resignation";

    static final int SLOT_SUMMARY = 4;
    static final int SLOT_ACCEPT = 20;
    static final int SLOT_REJECT = 24;

    private final String kingdomId;
    private Inventory inventory;

    public ResignationReviewGui(String kingdomId) {
        this.kingdomId = kingdomId;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static ResignationReviewGui create(String kingdomId, String summary) {
        ResignationReviewGui gui = new ResignationReviewGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(SLOT_SUMMARY, summaryItem(summary));
        inventory.setItem(
                SLOT_ACCEPT,
                new ItemBuilder(Material.LIME_WOOL).displayAs(ChatColor.GREEN + "Accept resignation").build());
        inventory.setItem(
                SLOT_REJECT,
                new ItemBuilder(Material.RED_WOOL).displayAs(ChatColor.RED + "Reject resignation").build());
        return gui;
    }

    public ParliamentHubAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_ACCEPT -> ParliamentHubAction.ACCEPT_RESIGNATION;
            case SLOT_REJECT -> ParliamentHubAction.REJECT_RESIGNATION;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack summaryItem(String summary) {
        return new ItemBuilder(Material.PAPER)
                .displayAs(ChatColor.GOLD + "Pending resignation")
                .lore(ChatColor.GRAY + summary)
                .build();
    }
}
