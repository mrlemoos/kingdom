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

/** The Lord Mayor's roll of members the Crown may appoint to its standing roster. */
public final class StandingRosterGui implements InventoryHolder {

    public static final Component TITLE = component("&6Standing Roster");
    public static final int PAGE_SIZE = 45;
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_PERMIT_REGISTER = 47;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 53;

    public record Entry(UUID playerId, boolean rostered) {}

    private final String kingdomId;
    private final int page;
    private final List<Entry> entries;
    private Inventory inventory;

    private StandingRosterGui(String kingdomId, int page, List<Entry> entries) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
        this.entries = List.copyOf(entries);
    }

    public static StandingRosterGui create(String kingdomId, List<Entry> allEntries, int requestedPage, int cap) {
        int pages = pageCount(allEntries.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int from = page * PAGE_SIZE;
        List<Entry> entries = from >= allEntries.size()
                ? List.of()
                : List.copyOf(allEntries.subList(from, Math.min(from + PAGE_SIZE, allEntries.size())));
        StandingRosterGui gui = new StandingRosterGui(kingdomId, page, entries);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, entries, page, allEntries.size(), (int) allEntries.stream().filter(Entry::rostered).count(), cap);
        return gui;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public UUID playerForSlot(int slot) {
        return slot >= 0 && slot < entries.size() ? entries.get(slot).playerId() : null;
    }

    public boolean isRostered(int slot) {
        return slot >= 0 && slot < entries.size() && entries.get(slot).rostered();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void populate(Inventory inventory, List<Entry> entries, int page, int total, int rosterSize, int cap) {
        int slot = 0;
        for (Entry entry : entries) {
            inventory.setItem(slot++, head(entry));
        }
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (page + 1 < pageCount(total)) {
            inventory.setItem(SLOT_NEXT, ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(SLOT_PERMIT_REGISTER, ItemBuilder.labelled(Material.WRITABLE_BOOK, c("&6Permit Register"), "Open the city permit register"));
        inventory.setItem(SLOT_PAGE, ItemBuilder.labelled(
                Material.IRON_SWORD,
                c("&6Roster " + rosterSize + " / " + cap),
                "Click a member to appoint or remove them."));
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int index = PAGE_SIZE; index < inventory.getSize(); index++) {
            if (inventory.getItem(index) == null) {
                inventory.setItem(index, filler);
            }
        }
    }

    private static ItemStack head(Entry entry) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.playerId());
        String name = player.getName();
        boolean rostered = entry.rostered();
        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(entry.playerId())
                .displayAs(c((rostered ? "&a" : "&f") + (name == null ? entry.playerId() : name)))
                .lore(c(rostered ? "&aStanding roster" : "&7Not rostered"))
                .lore(c(rostered ? "&eClick to remove" : "&eClick to appoint"))
                .build();
    }

    private static int pageCount(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
