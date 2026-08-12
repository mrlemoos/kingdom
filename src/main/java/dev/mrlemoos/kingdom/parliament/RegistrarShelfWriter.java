package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.inventory.ItemStack;

public final class RegistrarShelfWriter {

    private RegistrarShelfWriter() {}

    public record ShelfPlacement(RegistrarSite shelf, int slot) {}

    public static ShelfPlacement placeActBook(RegistrarSite anchor, List<String> pages) {
        String title = pages.isEmpty() ? "Act" : pages.get(0);
        return placeBook(anchor, title, pages);
    }

    /** Shelves any bound volume—an Act, or a volume of Hansard—under its own title. */
    public static ShelfPlacement placeBook(RegistrarSite anchor, String bookTitle, List<String> pages) {
        ShelfPlacement placement = findSlot(anchor);
        World world = org.bukkit.Bukkit.getWorld(placement.shelf().worldName());
        if (world == null) {
            throw new IllegalStateException("Registrar world is not loaded: " + placement.shelf().worldName());
        }

        Block block = world.getBlockAt(
                placement.shelf().blockX(),
                placement.shelf().blockY(),
                placement.shelf().blockZ());
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            block.setType(Material.CHISELED_BOOKSHELF);
        }

        if (!(block.getState() instanceof ChiseledBookshelf bookshelf)) {
            throw new IllegalStateException("Registrar shelf is not a chiseled bookshelf.");
        }

        String title = bookTitle == null || bookTitle.isBlank() ? "Act" : bookTitle;
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        ItemStack book = new ItemBuilder(Material.WRITTEN_BOOK)
                .book(title, RegistrarCatalogue.AUTHOR, pages)
                .build();

        bookshelf.getInventory().setItem(placement.slot(), book);
        bookshelf.update();
        return placement;
    }

    static ShelfPlacement findSlot(RegistrarSite anchor) {
        World world = org.bukkit.Bukkit.getWorld(anchor.worldName());
        if (world == null) {
            throw new IllegalStateException("Registrar world is not loaded: " + anchor.worldName());
        }

        List<RegistrarSite> cluster = RegistrarCluster.floodFill(
                anchor,
                site -> {
                    if (!site.worldName().equals(world.getName())) {
                        return false;
                    }
                    return world.getBlockAt(site.blockX(), site.blockY(), site.blockZ()).getType()
                            == Material.CHISELED_BOOKSHELF;
                },
                RegistrarCluster.MAX_SHELVES);

        if (cluster.isEmpty()) {
            cluster = List.of(anchor);
        }

        Map<RegistrarSite, Set<Integer>> occupied = new HashMap<>();
        int slotsPerShelf = 6;
        for (RegistrarSite site : cluster) {
            Block block = world.getBlockAt(site.blockX(), site.blockY(), site.blockZ());
            if (block.getType() != Material.CHISELED_BOOKSHELF
                    || !(block.getState() instanceof ChiseledBookshelf bookshelf)) {
                occupied.put(site, Set.of());
                continue;
            }
            slotsPerShelf = bookshelf.getInventory().getSize();
            Set<Integer> taken = new HashSet<>();
            for (int slot = 0; slot < slotsPerShelf; slot++) {
                ItemStack item = bookshelf.getInventory().getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    taken.add(slot);
                }
            }
            occupied.put(site, taken);
        }

        return RegistrarShelfSlot.next(anchor, cluster, occupied, slotsPerShelf);
    }
}
