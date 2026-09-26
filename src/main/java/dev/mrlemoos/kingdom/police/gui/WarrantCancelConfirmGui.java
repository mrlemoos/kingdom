package dev.mrlemoos.kingdom.police.gui;

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

/** Cancel or step back: the Crown's second thought before withdrawing a warrant from the register. */
public final class WarrantCancelConfirmGui implements InventoryHolder {

    public static final Component TITLE = component("&4Cancel warrant");
    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_WARRANT = 13;
    public static final int SLOT_BACK = 15;

    private final String kingdomId;
    private final String warrantId;
    private final int registerPage;
    private Inventory inventory;

    private WarrantCancelConfirmGui(String kingdomId, String warrantId, int registerPage) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.warrantId = Objects.requireNonNull(warrantId, "warrantId");
        this.registerPage = registerPage;
    }

    public static WarrantCancelConfirmGui create(String kingdomId, String warrantId, String suspectName, int registerPage) {
        WarrantCancelConfirmGui gui = new WarrantCancelConfirmGui(kingdomId, warrantId, registerPage);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        inventory.setItem(SLOT_CONFIRM, ItemBuilder.labelled(
                Material.RED_WOOL, c("&cCancel the warrant"), "Any arrest reward goes back to its poster"));
        inventory.setItem(SLOT_WARRANT, ItemBuilder.labelled(
                Material.PAPER, c("&6Warrant against " + suspectName), "The realm stops pursuing them"));
        inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the register"));
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public String warrantId() {
        return warrantId;
    }

    public int registerPage() {
        return registerPage;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
