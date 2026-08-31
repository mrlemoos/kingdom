package dev.mrlemoos.kingdom.church.gui;

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

/** The cleric's book, opened by the Crown or a Prince: crown the realm's rightful monarch. */
public final class CoronationGui implements InventoryHolder {

    public static final Component TITLE = component("&6Coronation");

    public static final int SLOT_CROWN = 11;
    public static final int SLOT_RITE = 13;
    public static final int SLOT_LEAVE = 15;

    private final String kingdomId;
    private Inventory inventory;

    public CoronationGui(String kingdomId) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
    }

    public String kingdomId() {
        return kingdomId;
    }

    /**
     * @param monarchName the rightful monarch, or null when the realm has none
     * @param crowned whether that monarch has already taken the crown
     */
    public static CoronationGui create(String kingdomId, String realmName, String monarchName, boolean crowned) {
        CoronationGui gui = new CoronationGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        populate(inventory, realmName, monarchName, crowned);
        return gui;
    }

    static void populate(Inventory inventory, String realmName, String monarchName, boolean crowned) {
        inventory.clear();
        inventory.setItem(
                SLOT_RITE,
                new ItemBuilder(Material.WRITTEN_BOOK)
                        .displayAs(c("&6The Rite of Coronation"))
                        .lore(c("&7" + realmName))
                        .lore(c("&7The monarch kneels at the altar; the church"))
                        .lore(c("&7sets the crown upon them before the realm."))
                        .build());
        ItemBuilder crown = new ItemBuilder(Material.GOLDEN_HELMET)
                .displayAs(c(crowned ? "&7Already crowned" : "&aCrown the monarch"));
        if (monarchName == null) {
            crown.lore(c("&cThis realm has no monarch to crown."));
        } else {
            crown.lore(c("&7Monarch: &f" + monarchName));
            crown.lore(c(crowned
                    ? "&7The crown is already theirs."
                    : "&7They must stand at the church."));
        }
        inventory.setItem(SLOT_CROWN, crown.build());
        inventory.setItem(
                SLOT_LEAVE,
                new ItemBuilder(Material.BARRIER)
                        .displayAs(c("&cLeave the church"))
                        .build());
        fillBackground(inventory);
    }

    public boolean isCrownSlot(int slot) {
        return slot == SLOT_CROWN;
    }

    public boolean isLeaveSlot(int slot) {
        return slot == SLOT_LEAVE;
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
