package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/** Reads written books from a live registrar shelf cluster. */
public final class RegistrarArchive {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private RegistrarArchive() {}

    public static List<RegistrarCatalogue.ShelvedBook> scan(World world, List<RegistrarSite> cluster) {
        Objects.requireNonNull(world, "world");
        if (cluster == null || cluster.isEmpty()) {
            return List.of();
        }
        List<RegistrarCatalogue.ShelvedBook> books = new ArrayList<>();
        for (RegistrarSite site : cluster) {
            if (!world.getName().equals(site.worldName())) {
                continue;
            }
            Block block = world.getBlockAt(site.blockX(), site.blockY(), site.blockZ());
            if (block.getType() != Material.CHISELED_BOOKSHELF) {
                continue;
            }
            if (!(block.getState() instanceof ChiseledBookshelf bookshelf)) {
                continue;
            }
            for (ItemStack stack : bookshelf.getInventory().getContents()) {
                if (stack == null || stack.getType() != Material.WRITTEN_BOOK) {
                    continue;
                }
                if (!(stack.getItemMeta() instanceof BookMeta meta)) {
                    continue;
                }
                String title = meta.getTitle() == null ? "" : meta.getTitle();
                String author = meta.getAuthor() == null ? "" : meta.getAuthor();
                List<String> pages = new ArrayList<>();
                for (var page : meta.pages()) {
                    pages.add(PLAIN.serialize(page));
                }
                books.add(new RegistrarCatalogue.ShelvedBook(title, author, pages));
            }
        }
        return List.copyOf(books);
    }
}
