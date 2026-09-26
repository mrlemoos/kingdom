package dev.mrlemoos.kingdom.war.tribute.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.war.tribute.TributeDesk;
import dev.mrlemoos.kingdom.war.tribute.WarDebt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The war debts the realm owes, one per creditor; a click chooses how much of one to pay. */
public final class WarDebtGui implements InventoryHolder {

    public static final int CAPACITY = 18;
    public static final int SLOT_BACK = 22;

    private final String kingdomId;
    private final Map<Integer, String> creditors;
    private Inventory inventory;

    private WarDebtGui(String kingdomId, Map<Integer, String> creditors) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.creditors = Map.copyOf(creditors);
    }

    public static WarDebtGui create(String kingdomId, List<WarDebt> owed, Function<String, String> displayName) {
        Map<Integer, String> creditors = new HashMap<>();
        for (int i = 0; i < Math.min(owed.size(), CAPACITY); i++) {
            creditors.put(i, owed.get(i).creditorKingdomId());
        }
        WarDebtGui gui = new WarDebtGui(kingdomId, creditors);
        Inventory inventory = Bukkit.createInventory(gui, 27, component("&6War Debt"));
        gui.inventory = inventory;
        for (int i = 0; i < Math.min(owed.size(), CAPACITY); i++) {
            WarDebt debt = owed.get(i);
            inventory.setItem(i, new ItemBuilder(Material.GOLD_INGOT)
                    .displayAs(c("&6Owed to " + displayName.apply(debt.creditorKingdomId())))
                    .lore(c("&7Outstanding: &f" + TributeDesk.formatCorona(debt.amount()) + " Corona"))
                    .lore(c("&7Paid from the treasury."))
                    .lore(c("&eClick to pay"))
                    .build());
        }
        if (owed.isEmpty()) {
            inventory.setItem(4, ItemBuilder.labelled(Material.PAPER, c("&7No war debt"), "The realm owes no one."));
        }
        inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.OAK_DOOR, c("&eBack"), "To the Treasury"));
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

    /** The creditor on this slot, or null. */
    public String creditorForSlot(int slot) {
        return creditors.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
