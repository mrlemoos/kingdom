package dev.mrlemoos.kingdom.city.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.city.AllegianceOath;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** City hall for an unaffiliated player: the written oath, then swear or decline. */
public final class OathGui implements InventoryHolder {

    public static final Component TITLE = component("&6Oath of Allegiance");

    public static final int SLOT_SWEAR = 11;
    public static final int SLOT_TEXT = 13;
    public static final int SLOT_DECLINE = 15;

    private final String kingdomId;
    private Inventory inventory;

    public OathGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static OathGui create(String kingdomId, String playerName, String addressee) {
        OathGui gui = new OathGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, playerName, addressee);
        return gui;
    }

    static void populate(Inventory inventory, String playerName, String addressee) {
        inventory.clear();
        String words = AllegianceOath.words(playerName, addressee);
        ItemBuilder book = new ItemBuilder(Material.WRITTEN_BOOK).displayAs(c("&6Oath of Allegiance"));
        for (String line : AllegianceOath.loreLines(words)) {
            book.lore(c("&7" + line));
        }
        book.lore(c("&7Swear before the Lord Mayor."));
        inventory.setItem(SLOT_TEXT, book.build());
        ItemBuilder swear = new ItemBuilder(Material.GOLD_INGOT).displayAs(c("&aI swear"));
        for (String line : AllegianceOath.loreLines(words)) {
            swear.lore(c("&7" + line));
        }
        inventory.setItem(SLOT_SWEAR, swear.build());
        inventory.setItem(
                SLOT_DECLINE,
                new ItemBuilder(Material.BARRIER)
                        .displayAs(c("&cI will not"))
                        .lore(c("&7Leave without joining."))
                        .build());
        fillBackground(inventory);
    }

    public boolean isSwearSlot(int slot) {
        return slot == SLOT_SWEAR;
    }

    public boolean isDeclineSlot(int slot) {
        return slot == SLOT_DECLINE;
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
