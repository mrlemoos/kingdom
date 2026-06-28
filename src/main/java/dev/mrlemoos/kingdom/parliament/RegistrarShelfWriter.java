package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;

public final class RegistrarShelfWriter {

    private RegistrarShelfWriter() {}

    public record ShelfPlacement(RegistrarSite shelf, int slot) {}

    public static ShelfPlacement placeActBook(RegistrarSite anchor, List<String> pages, List<RegistrarSite> existingShelves) {
        ShelfPlacement placement = findSlot(anchor, existingShelves);
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

        if (!(block.getState() instanceof TileState tileState) || !(tileState instanceof ChiseledBookshelf bookshelf)) {
            throw new IllegalStateException("Registrar shelf is not a chiseled bookshelf.");
        }

        String title = pages.isEmpty() ? "Act" : pages.get(0);
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        ItemStack book = new ItemBuilder(Material.WRITTEN_BOOK)
                .book(title, "Parliament", pages)
                .build();

        bookshelf.getInventory().setItem(placement.slot(), book);
        bookshelf.update();
        return placement;
    }

    private static ShelfPlacement findSlot(RegistrarSite anchor, List<RegistrarSite> existingShelves) {
        List<RegistrarSite> candidates = new java.util.ArrayList<>();
        candidates.add(anchor);
        candidates.addAll(existingShelves);

        for (RegistrarSite site : candidates) {
            World world = org.bukkit.Bukkit.getWorld(site.worldName());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(site.blockX(), site.blockY(), site.blockZ());
            if (block.getType() != Material.CHISELED_BOOKSHELF) {
                return new ShelfPlacement(site, 0);
            }
            if (block.getState() instanceof ChiseledBookshelf bookshelf) {
                for (int slot = 0; slot < bookshelf.getInventory().getSize(); slot++) {
                    ItemStack item = bookshelf.getInventory().getItem(slot);
                    if (item == null || item.getType().isAir()) {
                        return new ShelfPlacement(site, slot);
                    }
                }
            }
        }

        RegistrarSite next = adjacentShelf(anchor, candidates.size());
        return new ShelfPlacement(next, 0);
    }

    private static RegistrarSite adjacentShelf(RegistrarSite anchor, int offset) {
        int[][] deltas = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        int[] delta = deltas[offset % deltas.length];
        int steps = (offset / deltas.length) + 1;
        return RegistrarSite.of(
                anchor.worldName(),
                anchor.blockX() + delta[0] * steps,
                anchor.blockY() + delta[1] * steps,
                anchor.blockZ() + delta[2] * steps);
    }
}
