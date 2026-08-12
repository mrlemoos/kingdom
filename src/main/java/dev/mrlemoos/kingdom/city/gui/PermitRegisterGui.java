package dev.mrlemoos.kingdom.city.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** The paginated roll of a kingdom's build-permit holders, one player head each. */
public final class PermitRegisterGui implements InventoryHolder {

    public static final Component TITLE = component("&6Permit Register");

    /** One holder of the roll: who they are and when the permit was issued. */
    public record Entry(UUID holderId, long grantedAtMs) {}

    private final String kingdomId;
    private final int page;
    private final List<Entry> pageEntries;
    private Inventory inventory;

    public PermitRegisterGui(String kingdomId, int page, List<Entry> pageEntries) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
        this.pageEntries = List.copyOf(pageEntries);
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public static PermitRegisterGui create(
            String kingdomId,
            List<Entry> allEntries,
            int requestedPage,
            long nowMs,
            CityStatistics statistics) {
        int total = allEntries.size();
        int page = PermitRegisterLayout.clampPage(requestedPage, total);
        List<Entry> slice = PermitRegisterLayout.pageSlice(allEntries, page);
        PermitRegisterGui gui = new PermitRegisterGui(kingdomId, page, slice);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, slice, page, total, nowMs, statistics);
        return gui;
    }

    private static void populate(
            Inventory inventory,
            List<Entry> slice,
            int page,
            int total,
            long nowMs,
            CityStatistics statistics) {
        inventory.clear();
        int slot = 0;
        for (Entry entry : slice) {
            inventory.setItem(slot++, head(entry, nowMs));
        }
        if (PermitRegisterLayout.hasPrevious(page)) {
            inventory.setItem(
                    PermitRegisterLayout.SLOT_PREVIOUS,
                    ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (PermitRegisterLayout.hasNext(page, total)) {
            inventory.setItem(
                    PermitRegisterLayout.SLOT_NEXT,
                    ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(
                PermitRegisterLayout.SLOT_PAGE,
                ItemBuilder.labelled(
                        Material.BOOK,
                        c("&6Page " + (page + 1) + " of " + PermitRegisterLayout.pageCount(total)),
                        total + " permit holder" + (total == 1 ? "" : "s")));
        if (statistics != null) {
            inventory.setItem(PermitRegisterLayout.SLOT_STATISTICS, statisticsItem(statistics));
        }
        fillBackground(inventory);
    }

    private static ItemStack statisticsItem(CityStatistics statistics) {
        ItemBuilder builder = new ItemBuilder(Material.WRITABLE_BOOK)
                .displayAs(c("&6State of the Realm"));
        for (String line : statistics.lines()) {
            builder.lore(c("&7" + line));
        }
        return builder.build();
    }

    private static ItemStack head(Entry entry, long nowMs) {
        OfflinePlayer holder = Bukkit.getOfflinePlayer(entry.holderId());
        String name = holder.getName();
        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(entry.holderId())
                .displayAs(c("&f" + (name == null ? entry.holderId().toString() : name)))
                .lore(c("&7Granted " + PermitRegisterLayout.grantedAgo(entry.grantedAtMs(), nowMs)))
                .lore(c("&7Click to revoke this permit"))
                .build();
    }

    /** The holder occupying this slot on the page shown, or null. */
    public UUID holderForSlot(int slot) {
        if (!PermitRegisterLayout.isHeadSlot(slot) || slot >= pageEntries.size()) {
            return null;
        }
        return pageEntries.get(slot).holderId();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = PermitRegisterLayout.PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }
}
