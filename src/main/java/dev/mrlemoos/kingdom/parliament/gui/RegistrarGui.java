package dev.mrlemoos.kingdom.parliament.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.city.gui.GazetteLayout;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.parliament.RegistrarCatalogue;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Paginated registrar catalogue: Acts, then Hansard. */
public final class RegistrarGui implements InventoryHolder {

    public static final Component TITLE = component("&3Registrar");

    private final String kingdomId;
    private final int page;
    private final List<RegistrarCatalogue.Volume> volumes;
    private Inventory inventory;

    public RegistrarGui(String kingdomId, int page, List<RegistrarCatalogue.Volume> volumes) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.page = page;
        this.volumes = List.copyOf(Objects.requireNonNull(volumes, "volumes"));
    }

    public String kingdomId() {
        return kingdomId;
    }

    public int page() {
        return page;
    }

    public List<RegistrarCatalogue.Volume> volumes() {
        return volumes;
    }

    public static RegistrarGui create(String kingdomId, List<RegistrarCatalogue.Volume> volumes, int requestedPage) {
        int page = GazetteLayout.clampPage(requestedPage, volumes.size());
        List<RegistrarCatalogue.Volume> slice = GazetteLayout.pageSlice(volumes, page);
        RegistrarGui gui = new RegistrarGui(kingdomId, page, volumes);
        Inventory inventory = Bukkit.createInventory(gui, 54, TITLE);
        gui.inventory = inventory;
        populate(inventory, slice, page, volumes.size());
        return gui;
    }

    private static void populate(
            Inventory inventory, List<RegistrarCatalogue.Volume> slice, int page, int total) {
        inventory.clear();
        int slot = 0;
        for (RegistrarCatalogue.Volume volume : slice) {
            inventory.setItem(slot++, itemFor(volume));
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
                        c("&3Page " + (page + 1) + " of " + GazetteLayout.pageCount(total)),
                        total == 0 ? "Empty archive" : total + " volume" + (total == 1 ? "" : "s")));
        fillBackground(inventory);
    }

    private static ItemStack itemFor(RegistrarCatalogue.Volume volume) {
        boolean hansard = volume.section() == RegistrarCatalogue.Section.HANSARD;
        return new ItemBuilder(Material.WRITTEN_BOOK)
                .displayAs(hansard ? c("&3" + volume.title()) : c("&f" + volume.title()))
                .lore(hansard ? c("&7Hansard") : c("&7Act"))
                .lore(c("&8Click to read"))
                .build();
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack pane = ItemBuilder.labelled(Material.GRAY_STAINED_GLASS_PANE, " ", " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, pane);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
