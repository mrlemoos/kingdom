package dev.mrlemoos.kingdom.hub.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The Realm Hub: one screen holding everything a subject may do and everywhere the realm's offices
 * stand. Powers not theirs are shown greyed and refused rather than hidden, so the realm's workings
 * are discoverable. Rendering only — {@code hub/RealmHubView} decides what is on it.
 */
public final class RealmHubGui implements InventoryHolder {

    public static final Component TITLE = component("&6The Realm");

    private final int page;
    private final List<RealmHubEntry> pageEntries;
    private Inventory inventory;

    private RealmHubGui(int page, List<RealmHubEntry> pageEntries) {
        this.page = page;
        this.pageEntries = List.copyOf(pageEntries);
    }

    public int page() {
        return page;
    }

    public static RealmHubGui create(List<RealmHubEntry> entries, int requestedPage) {
        int total = entries.size();
        int page = RealmHubLayout.clampPage(requestedPage, total);
        List<RealmHubEntry> slice = RealmHubLayout.pageSlice(entries, page);
        RealmHubGui gui = new RealmHubGui(page, slice);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, slice, page, total);
        return gui;
    }

    private static void populate(Inventory inventory, List<RealmHubEntry> slice, int page, int total) {
        inventory.clear();
        int slot = 0;
        for (RealmHubEntry entry : slice) {
            inventory.setItem(slot++, item(entry));
        }
        if (RealmHubLayout.hasPrevious(page)) {
            inventory.setItem(
                    RealmHubLayout.SLOT_PREVIOUS,
                    ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (RealmHubLayout.hasNext(page, total)) {
            inventory.setItem(
                    RealmHubLayout.SLOT_NEXT,
                    ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(
                RealmHubLayout.SLOT_PAGE,
                ItemBuilder.labelled(
                        Material.BOOK,
                        c("&6Page " + (page + 1) + " of " + RealmHubLayout.pageCount(total)),
                        total + " entr" + (total == 1 ? "y" : "ies")));
        fillBackground(inventory);
    }

    static ItemStack item(RealmHubEntry entry) {
        ItemBuilder builder = new ItemBuilder(entry.usable() ? material(entry.topic()) : Material.GRAY_DYE)
                .displayAs(c((entry.usable() ? "&6" : "&8") + entry.title()));
        if (!entry.refusal().isBlank()) {
            builder.lore(c("&c" + entry.refusal()));
        }
        for (String line : entry.lines()) {
            builder.lore(c((entry.usable() ? "&7" : "&8") + line));
        }
        if (entry.usable() && entry.action() != RealmHubAction.NONE) {
            builder.lore(c("&eClick to open"));
        }
        return builder.build();
    }

    /** The item that stands for each topic, so the screen reads at a glance. */
    static Material material(RealmHubTopic topic) {
        return switch (topic) {
            case STANDING -> Material.PLAYER_HEAD;
            case LOYALTY_LEDGER -> Material.WRITTEN_BOOK;
            case OATH_OF_SERVICE -> Material.IRON_SWORD;
            case GAZETTE -> Material.PAPER;
            case BUILD_PERMIT -> Material.BRICKS;
            case LIVE_ELECTION -> Material.LECTERN;
            case LIVE_POLLING -> Material.PAPER;
            case LIVE_DIVISION -> Material.LIME_BANNER;
            case LIVE_WARRANT -> Material.REDSTONE;
            case LIVE_RESIGNATION -> Material.PAPER;
            case LIVE_MUSTER -> Material.IRON_SWORD;
            case LIVE_SIEGE -> Material.SHIELD;
            case POWER_PERMITS -> Material.WRITABLE_BOOK;
            case POWER_GAZETTE -> Material.FEATHER;
            case POWER_MINTS -> Material.GOLD_NUGGET;
            case POWER_POLICE_GOLEMS -> Material.IRON_BLOCK;
            case POWER_STANDING_ROSTER -> Material.IRON_SWORD;
            case POWER_CONSCRIPTION -> Material.IRON_SWORD;
            case POWER_CROWN_SQUADS -> Material.IRON_GOLEM_SPAWN_EGG;
            case POWER_SQUADS -> Material.LEAD;
            case POWER_MORALE_PARDON -> Material.SHIELD;
            case POWER_PARLIAMENT -> Material.BELL;
            case POWER_WAR_DEBT -> Material.GOLD_INGOT;
            case POWER_CAPITAL -> Material.GOLDEN_HELMET;
            case POWER_SWORN_ROLES -> Material.IRON_SWORD;
            case POWER_SITES -> Material.COMPASS;
            case PLACE_CAPITAL -> Material.BEACON;
            case PLACE_TOWN_CRIER -> Material.NOTE_BLOCK;
            case PLACE_CHURCH -> Material.CANDLE;
            case PLACE_COURT -> Material.LECTERN;
            case PLACE_PRISON -> Material.IRON_BARS;
            case PLACE_GRANARY -> Material.WHEAT;
            case PLACE_MINTS -> Material.GOLD_INGOT;
            case PLACE_COMMONS -> Material.OAK_STAIRS;
            case PLACE_LORDS -> Material.RED_CARPET;
            case PLACE_SPEAKER_CHAIR -> Material.OAK_TRAPDOOR;
        };
    }

    /** The entry occupying this slot on the page shown, or null. */
    public RealmHubEntry entryForSlot(int slot) {
        if (!RealmHubLayout.isEntrySlot(slot) || slot >= pageEntries.size()) {
            return null;
        }
        return pageEntries.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = RealmHubLayout.PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
