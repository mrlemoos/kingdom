package dev.mrlemoos.kingdom.loyalty.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.loyalty.LoyaltyLedgerView;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The loyalty ledger menu: one item per track, read-only. Clicks are cancelled by the listener —
 * nothing here changes a tier or a clock.
 */
public final class LoyaltyLedgerGui implements InventoryHolder {

    public static final Component TITLE = component("&6Loyalty Ledger");

    public static final int SLOT_POLITICAL = 11;
    public static final int SLOT_MILITARY = 15;

    private Inventory inventory;

    public static LoyaltyLedgerGui create(LoyaltyLedgerView view, String subjectName) {
        LoyaltyLedgerGui gui = new LoyaltyLedgerGui();
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(SLOT_POLITICAL, trackItem(Material.WRITTEN_BOOK, view.political(), subjectName));
        inventory.setItem(SLOT_MILITARY, trackItem(Material.IRON_SWORD, view.military(), subjectName));
        return gui;
    }

    static ItemStack trackItem(Material material, LoyaltyLedgerView.Track track, String subjectName) {
        List<String> lore = new ArrayList<>();
        lore.add(c("&7Subject: &f" + subjectName));
        lore.add(c("&7Standing: &f" + track.tier()));
        track.daysToNextTick()
                .ifPresent(days -> lore.add(c("&7Next recovery: &f"
                        + (days == 0 ? "due at the next day's turn" : days + " in-game day" + (days == 1 ? "" : "s")))));
        lore.add("");
        lore.add(c("&e" + track.tip()));
        return new ItemBuilder(material).displayAs(c("&6" + track.name())).lore(lore).build();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
