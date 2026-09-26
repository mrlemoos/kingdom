package dev.mrlemoos.kingdom.hub.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubSection;
import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The Realm Hub: a front page holding the reader's standing, whatever business is live and a door to
 * each {@link RealmHubSection}; behind each door, that section's places, powers and experiences.
 * Powers not theirs are shown greyed and refused rather than hidden, so the realm's workings are
 * discoverable. Rendering only — {@code hub/RealmHubView} decides what is on it.
 */
public final class RealmHubGui implements InventoryHolder {

    public static final Component TITLE = component("&6The Realm");

    private final RealmHubSection section;
    private final int page;
    private final boolean hasNext;
    private final Map<Integer, RealmHubEntry> entries;
    private final Map<Integer, RealmHubSection> doors;
    private Inventory inventory;

    private RealmHubGui(
            RealmHubSection section,
            int page,
            boolean hasNext,
            Map<Integer, RealmHubEntry> entries,
            Map<Integer, RealmHubSection> doors) {
        this.section = section;
        this.page = page;
        this.hasNext = hasNext;
        this.entries = Map.copyOf(entries);
        this.doors = Map.copyOf(doors);
    }

    /** The section shown; empty on the front page. */
    public Optional<RealmHubSection> section() {
        return Optional.ofNullable(section);
    }

    public int page() {
        return page;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return hasNext;
    }

    /** The front page: standing, live business, and a door per section. */
    public static RealmHubGui frontPage(
            List<RealmHubEntry> front, Map<RealmHubSection, List<RealmHubEntry>> sections) {
        Map<Integer, RealmHubEntry> placed = new HashMap<>();
        int live = 0;
        for (RealmHubEntry entry : front) {
            if (entry.topic() == RealmHubTopic.STANDING) {
                placed.put(RealmHubLayout.SLOT_STANDING, entry);
            } else if (live < RealmHubLayout.LIVE_CAPACITY) {
                placed.put(RealmHubLayout.liveSlot(live++), entry);
            }
        }
        Map<Integer, RealmHubSection> doors = new HashMap<>();
        for (RealmHubSection door : RealmHubSection.values()) {
            doors.put(RealmHubLayout.doorSlot(door), door);
        }
        RealmHubGui gui = new RealmHubGui(null, 0, false, placed, doors);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        placed.forEach((slot, entry) -> inventory.setItem(slot, item(entry)));
        for (RealmHubSection door : RealmHubSection.values()) {
            inventory.setItem(RealmHubLayout.doorSlot(door), door(door, sections.getOrDefault(door, List.of())));
        }
        fill(inventory, 0, inventory.getSize());
        return gui;
    }

    /** One page of a section: places, powers, experiences; Back, and arrows only on overflow. */
    public static RealmHubGui section(RealmHubSection section, List<RealmHubEntry> entries, int requestedPage) {
        List<Map<Integer, RealmHubEntry>> pages = RealmHubLayout.sectionPages(entries);
        int page = RealmHubLayout.clampPage(requestedPage, pages.size());
        Map<Integer, RealmHubEntry> placed = pages.get(page);
        RealmHubGui gui = new RealmHubGui(section, page, page < pages.size() - 1, placed, Map.of());
        Inventory inventory = Bukkit.createInventory(gui, 54, component("&6The Realm &7— &6" + section.title()));
        gui.inventory = inventory;
        placed.forEach((slot, entry) -> inventory.setItem(slot, item(entry)));
        if (gui.hasPrevious()) {
            inventory.setItem(
                    RealmHubLayout.SLOT_PREVIOUS,
                    ItemBuilder.labelled(Material.ARROW, c("&ePrevious page"), "Page " + page));
        }
        if (gui.hasNext()) {
            inventory.setItem(
                    RealmHubLayout.SLOT_NEXT,
                    ItemBuilder.labelled(Material.ARROW, c("&eNext page"), "Page " + (page + 2)));
        }
        inventory.setItem(
                RealmHubLayout.SLOT_BACK,
                ItemBuilder.labelled(Material.OAK_DOOR, c("&eBack"), c("&7To the front page")));
        fill(inventory, RealmHubLayout.PAGE_SIZE, inventory.getSize());
        return gui;
    }

    /** Lore reads: where things stand and how (the lines), then who may. */
    static ItemStack item(RealmHubEntry entry) {
        ItemBuilder builder = new ItemBuilder(entry.usable() ? material(entry.topic()) : Material.GRAY_DYE)
                .displayAs(c((entry.usable() ? "&6" : "&8") + entry.title()));
        for (String line : entry.lines()) {
            builder.lore(c((entry.usable() ? "&7" : "&8") + line));
        }
        if (!entry.whoLine().isBlank()) {
            builder.lore(c((entry.usable() ? "&7" : "&c") + entry.whoLine()));
        }
        if (entry.usable()
                && entry.action() != RealmHubAction.NONE
                && entry.action() != RealmHubAction.TAKE_FOUNDATION_STONE) {
            builder.lore(c("&eClick to open"));
        }
        return builder.build();
    }

    static ItemStack door(RealmHubSection section, List<RealmHubEntry> entries) {
        long yours = entries.stream().filter(RealmHubEntry::usable).count();
        return new ItemBuilder(doorMaterial(section))
                .displayAs(c("&6" + section.title()))
                .lore(c("&7" + section.blurb()))
                .lore(c("&7" + yours + " of " + entries.size() + " entr" + (entries.size() == 1 ? "y" : "ies")
                        + " open to you"))
                .lore(c("&eClick to open"))
                .build();
    }

    static Material doorMaterial(RealmHubSection section) {
        return switch (section) {
            case CITY -> Material.BEACON;
            case CHURCH -> Material.CANDLE;
            case PARLIAMENT -> Material.BELL;
            case POLICE -> Material.IRON_BARS;
            case WAR -> Material.IRON_SWORD;
            case TREASURY -> Material.GOLD_INGOT;
        };
    }

    /** The item that stands for each topic, so the screen reads at a glance. */
    static Material material(RealmHubTopic topic) {
        return switch (topic) {
            case STANDING -> Material.PLAYER_HEAD;
            case LOYALTY_LEDGER -> Material.WRITTEN_BOOK;
            case OATH_OF_SERVICE -> Material.IRON_SWORD;
            case RITES -> Material.CANDLE;
            case GAZETTE -> Material.PAPER;
            case BUILD_PERMIT -> Material.BRICKS;
            case ARREST_REWARD -> Material.GOLD_NUGGET;
            case WALLET -> Material.GOLD_NUGGET;
            case LIVE_ELECTION -> Material.LECTERN;
            case LIVE_POLLING -> Material.PAPER;
            case LIVE_DIVISION -> Material.LIME_BANNER;
            case LIVE_WARRANT -> Material.REDSTONE;
            case LIVE_RESIGNATION -> Material.PAPER;
            case LIVE_MUSTER -> Material.IRON_SWORD;
            case LIVE_SIEGE -> Material.SHIELD;
            case LIVE_TREATY -> Material.WRITABLE_BOOK;
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
            case POWER_ARREST -> Material.IRON_SWORD;
            case POWER_WARRANTS -> Material.WRITABLE_BOOK;
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
            case PLACE_BAR -> Material.OAK_FENCE;
            case PLACE_MP_SEATS -> Material.SPRUCE_STAIRS;
            case PLACE_REGISTRAR -> Material.CHISELED_BOOKSHELF;
        };
    }

    /** The entry occupying this slot on the page shown, or null. */
    public RealmHubEntry entryForSlot(int slot) {
        return entries.get(slot);
    }

    /** The section whose door occupies this slot of the front page, or null. */
    public RealmHubSection doorForSlot(int slot) {
        return doors.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void fill(Inventory inventory, int from, int to) {
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = from; slot < to; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

}
