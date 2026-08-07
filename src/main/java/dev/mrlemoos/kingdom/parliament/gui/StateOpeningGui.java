package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class StateOpeningGui implements InventoryHolder {

    public static final Component TITLE = component("&6State Opening");

    static final int SLOT_INFO = 4;
    static final int SLOT_SUMMON = 11;
    static final int SLOT_DECLARE = 15;

    private final String kingdomId;
    private Inventory inventory;

    public StateOpeningGui(String kingdomId) {
        this.kingdomId = kingdomId;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public static StateOpeningGui create(String kingdomId, boolean summoned, boolean inLords) {
        StateOpeningGui gui = new StateOpeningGui(kingdomId);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;

        inventory.setItem(
                SLOT_INFO,
                ItemBuilder.labelled(
                        Material.WRITTEN_BOOK,
                        c("&6Speech from the Throne"),
                        "Parliament is prorogued until the Crown opens it"));
        inventory.setItem(
                SLOT_SUMMON,
                ItemBuilder.labelled(
                        Material.BELL,
                        summoned ? c("&8Realm summoned") : c("&aSummon the realm"),
                        summoned
                                ? "The realm is already gathered"
                                : "Gather every subject in the House of Lords"));
        inventory.setItem(
                SLOT_DECLARE,
                ItemBuilder.labelled(
                        Material.GOLDEN_HELMET,
                        (summoned && inLords ? c("&a") : c("&8")) + "Declare Parliament open",
                        declareLore(summoned, inLords)));
        return gui;
    }

    public ParliamentHubAction actionForSlot(int slot) {
        return switch (slot) {
            case SLOT_SUMMON -> ParliamentHubAction.SUMMON_REALM;
            case SLOT_DECLARE -> ParliamentHubAction.DECLARE_OPEN;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static String declareLore(boolean summoned, boolean inLords) {
        if (!summoned) {
            return "Summon the realm first";
        }
        if (!inLords) {
            return "You must stand in the House of Lords";
        }
        return "Open the session and begin the Parliament";
    }
}
