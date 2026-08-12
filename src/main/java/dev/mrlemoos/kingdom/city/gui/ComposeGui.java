package dev.mrlemoos.kingdom.city.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Crown compose menu when the monarch right-clicks the Town Crier holding a signed book.
 * Decree path also offers curfew presets.
 */
public final class ComposeGui implements InventoryHolder {

    public static final Component TITLE_KIND = component("&6Compose Gazette");
    public static final Component TITLE_CURFEW = component("&6Decree Curfew");

    public enum Stage {
        KIND,
        CURFEW
    }

    public enum Choice {
        ANNOUNCEMENT,
        DECREE,
        CANCEL,
        CURFEW_DUSK_DAWN,
        CURFEW_NIGHTFALL_MIDNIGHT,
        CURFEW_LIFT,
        CURFEW_NONE,
        BACK
    }

    public static final int SLOT_DECREE = 11;
    public static final int SLOT_ANNOUNCEMENT = 13;
    public static final int SLOT_CANCEL = 15;

    public static final int SLOT_DUSK_DAWN = 10;
    public static final int SLOT_NIGHTFALL = 12;
    public static final int SLOT_LIFT = 14;
    public static final int SLOT_NONE = 16;
    public static final int SLOT_BACK = 22;

    private final String kingdomId;
    private final Stage stage;
    private final String bookTitle;
    private final String bookBody;
    private Inventory inventory;

    public ComposeGui(String kingdomId, Stage stage, String bookTitle, String bookBody) {
        this.kingdomId = kingdomId;
        this.stage = stage;
        this.bookTitle = bookTitle;
        this.bookBody = bookBody;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public Stage stage() {
        return stage;
    }

    public String bookTitle() {
        return bookTitle;
    }

    public String bookBody() {
        return bookBody;
    }

    public static ComposeGui kind(String kingdomId, String bookTitle, String bookBody) {
        ComposeGui gui = new ComposeGui(kingdomId, Stage.KIND, bookTitle, bookBody);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE_KIND);
        gui.inventory = inventory;
        inventory.setItem(
                SLOT_DECREE,
                ItemBuilder.labelled(
                        Material.GOLDEN_HELMET,
                        c("&6&lDecree"),
                        "Binding; enters Hansard; may set curfew"));
        inventory.setItem(
                SLOT_ANNOUNCEMENT,
                ItemBuilder.labelled(
                        Material.PAPER,
                        c("&7Announcement"),
                        "Posted to the Gazette only"));
        inventory.setItem(
                SLOT_CANCEL,
                ItemBuilder.labelled(Material.BARRIER, c("&cCancel"), "Keep the book"));
        return gui;
    }

    public static ComposeGui curfew(String kingdomId, String bookTitle, String bookBody) {
        ComposeGui gui = new ComposeGui(kingdomId, Stage.CURFEW, bookTitle, bookBody);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE_CURFEW);
        gui.inventory = inventory;
        inventory.setItem(
                SLOT_DUSK_DAWN,
                ItemBuilder.labelled(
                        Material.CLOCK, c("&6Dusk–Dawn"), "13000–23000"));
        inventory.setItem(
                SLOT_NIGHTFALL,
                ItemBuilder.labelled(
                        Material.CLOCK, c("&6Nightfall–Midnight"), "13000–18000"));
        inventory.setItem(
                SLOT_LIFT,
                ItemBuilder.labelled(
                        Material.FEATHER, c("&eLift curfew"), "Clear the decree window"));
        inventory.setItem(
                SLOT_NONE,
                ItemBuilder.labelled(
                        Material.BOOK, c("&7No curfew"), "Leave the window unchanged"));
        inventory.setItem(
                SLOT_BACK,
                ItemBuilder.labelled(Material.ARROW, c("&eBack"), "Choose decree or announcement"));
        return gui;
    }

    public Choice choiceForSlot(int slot) {
        if (stage == Stage.KIND) {
            return switch (slot) {
                case SLOT_DECREE -> Choice.DECREE;
                case SLOT_ANNOUNCEMENT -> Choice.ANNOUNCEMENT;
                case SLOT_CANCEL -> Choice.CANCEL;
                default -> null;
            };
        }
        return switch (slot) {
            case SLOT_DUSK_DAWN -> Choice.CURFEW_DUSK_DAWN;
            case SLOT_NIGHTFALL -> Choice.CURFEW_NIGHTFALL_MIDNIGHT;
            case SLOT_LIFT -> Choice.CURFEW_LIFT;
            case SLOT_NONE -> Choice.CURFEW_NONE;
            case SLOT_BACK -> Choice.BACK;
            default -> null;
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
