package dev.leo.kingdom.parliament.gui;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class DivisionVoteGui implements InventoryHolder {

    public static final String TITLE = ChatColor.DARK_GREEN + "Division";

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
        inventory.setItem(SLOT_AYE, voteItem(Material.LIME_CONCRETE, ChatColor.GREEN + "Aye", "Support the bill"));
        inventory.setItem(SLOT_NAY, voteItem(Material.RED_CONCRETE, ChatColor.RED + "Nay", "Oppose the bill"));
        inventory.setItem(
                SLOT_ABSTAIN, voteItem(Material.YELLOW_CONCRETE, ChatColor.YELLOW + "Abstain", "Record no vote"));
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

    private static ItemStack voteItem(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack billItem(String billTitle) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + billTitle);
            meta.setLore(List.of(ChatColor.GRAY + "Bill before the House"));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
