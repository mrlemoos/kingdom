package dev.mrlemoos.kingdom.city.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Paginated Gazette board: authored posts plus live realm state read at open. */
public final class GazetteGui implements InventoryHolder {

    public static final Component TITLE = component("&6Gazette");

    public sealed interface Entry permits AuthoredEntry, LiveEntry {}

    public record AuthoredEntry(GazettePost post) implements Entry {}

    public record LiveEntry(String heading, List<String> lines) implements Entry {}

    private final String kingdomId;
    private final int page;
    private Inventory inventory;

    public GazetteGui(String kingdomId, int page) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public static GazetteGui create(
            String kingdomId, List<GazettePost> posts, GazetteLiveState liveState, int requestedPage) {
        List<Entry> entries = new ArrayList<>();
        if (liveState != null) {
            entries.add(new LiveEntry("State of the Realm", liveState.lines()));
        }
        for (GazettePost post : posts) {
            entries.add(new AuthoredEntry(post));
        }
        int page = GazetteLayout.clampPage(requestedPage, entries.size());
        List<Entry> slice = GazetteLayout.pageSlice(entries, page);
        GazetteGui gui = new GazetteGui(kingdomId, page);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, slice, page, entries.size());
        return gui;
    }

    private static void populate(Inventory inventory, List<Entry> slice, int page, int total) {
        inventory.clear();
        int slot = 0;
        for (Entry entry : slice) {
            inventory.setItem(slot++, itemFor(entry));
        }
        if (GazetteLayout.hasPrevious(page)) {
            inventory.setItem(
                    GazetteLayout.SLOT_PREVIOUS,
                    ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (GazetteLayout.hasNext(page, total)) {
            inventory.setItem(
                    GazetteLayout.SLOT_NEXT,
                    ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(
                GazetteLayout.SLOT_PAGE,
                ItemBuilder.labelled(
                        Material.BOOK,
                        c("&6Page " + (page + 1) + " of " + GazetteLayout.pageCount(total)),
                        total + " item" + (total == 1 ? "" : "s")));
        fillBackground(inventory);
    }

    private static ItemStack itemFor(Entry entry) {
        return switch (entry) {
            case LiveEntry live -> {
                ItemBuilder builder = new ItemBuilder(Material.WRITABLE_BOOK)
                        .displayAs(c("&6" + live.heading()));
                for (String line : live.lines()) {
                    builder.lore(c("&7" + line));
                }
                yield builder.build();
            }
            case AuthoredEntry authored -> {
                GazettePost post = authored.post();
                boolean decree = post.kind() == GazettePostKind.DECREE;
                ItemBuilder builder = new ItemBuilder(decree ? Material.GOLDEN_HELMET : Material.PAPER)
                        .displayAs(decree ? c("&6&lDECREE &f" + post.title()) : c("&7" + post.title()));
                builder.lore(c("&8Day " + post.mcDay()));
                for (String line : wrapBody(post.body())) {
                    builder.lore(c(decree ? "&f" + line : "&7" + line));
                }
                yield builder.build();
            }
        };
    }

    private static List<String> wrapBody(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        String[] words = body.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > 40 && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
            if (lines.size() >= 6) {
                break;
            }
        }
        if (current.length() > 0 && lines.size() < 6) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = GazetteLayout.PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
