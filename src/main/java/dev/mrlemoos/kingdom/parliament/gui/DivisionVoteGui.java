package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class DivisionVoteGui implements InventoryHolder {

    public static final String TITLE = c("&2Division");

    static final int SLOT_AYE = 11;
    static final int SLOT_BILL_INFO = 13;
    static final int SLOT_NAY = 15;
    static final int SLOT_ABSTAIN = 22;

    private final String kingdomId;
    private final String billTitle;
    private Inventory inventory;

    public DivisionVoteGui(String kingdomId, String billTitle) {
        this.kingdomId = kingdomId;
        this.billTitle = billTitle;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public String billTitle() {
        return billTitle;
    }

    public static DivisionVoteGui create(String kingdomId, String billTitle) {
        DivisionVoteGui gui = new DivisionVoteGui(kingdomId, billTitle);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, billTitle);
        return gui;
    }

    static void populate(Inventory inventory, String billTitle) {
        inventory.clear();
        inventory.setItem(SLOT_AYE, ItemBuilder.labelled(Material.LIME_CONCRETE, c("&aAye"), "Support the bill"));
        inventory.setItem(SLOT_NAY, ItemBuilder.labelled(Material.RED_CONCRETE, c("&cNay"), "Oppose the bill"));
        inventory.setItem(
                SLOT_ABSTAIN, ItemBuilder.labelled(Material.YELLOW_CONCRETE, c("&eAbstain"), "Record no vote"));
        inventory.setItem(SLOT_BILL_INFO, billItem(billTitle));
        fillBackground(inventory);
    }

    public ParliamentHubAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_AYE -> ParliamentHubAction.VOTE_AYE;
            case SLOT_NAY -> ParliamentHubAction.VOTE_NAY;
            case SLOT_ABSTAIN -> ParliamentHubAction.VOTE_ABSTAIN;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack billItem(String billTitle) {
        return new ItemBuilder(Material.WRITTEN_BOOK)
                .displayAs(c("&6" + billTitle))
                .lore(c("&7Bill before the House"))
                .build();
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
