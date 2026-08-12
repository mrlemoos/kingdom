package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.gui.GazetteLayout;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.parliament.RegistrarArchive;
import dev.mrlemoos.kingdom.parliament.RegistrarCatalogue;
import dev.mrlemoos.kingdom.parliament.RegistrarCluster;
import dev.mrlemoos.kingdom.parliament.gui.RegistrarGui;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Public registrar catalogue on right-click; seals take/put/break on the face-connected cluster (OP
 * bypass for direct shelf interaction and break).
 */
public final class RegistrarListener implements Listener {

    private final KingdomService kingdomService;

    public RegistrarListener(KingdomService kingdomService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractShelf(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }

        Claim claim = claimOf(block);
        if (claim instanceof Claim.None) {
            return;
        }
        if (claim instanceof Claim.Ambiguous) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(c("&cThis shelf is claimed by more than one registrar."));
            return;
        }
        Claim.Owned owned = (Claim.Owned) claim;

        // OP seal bypass: sneak keeps vanilla take/put; otherwise open the catalogue.
        if (event.getPlayer().isOp() && event.getPlayer().isSneaking()) {
            return;
        }

        event.setCancelled(true);
        openCatalogue(event.getPlayer(), owned.kingdomId(), block.getWorld(), 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakShelf(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }
        Claim claim = claimOf(block);
        if (claim instanceof Claim.None) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(c("&cThe registrar is sealed. Only an operator may break it."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        Block sourceBlock = blockOf(event.getSource());
        Block destBlock = blockOf(event.getDestination());
        if (isSealedBlock(sourceBlock) || isSealedBlock(destBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatalogueClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RegistrarGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == GazetteLayout.SLOT_PREVIOUS && GazetteLayout.hasPrevious(gui.page())) {
            reopen(player, gui.kingdomId(), gui.page() - 1);
            return;
        }
        if (slot == GazetteLayout.SLOT_NEXT && GazetteLayout.hasNext(gui.page(), gui.volumes().size())) {
            reopen(player, gui.kingdomId(), gui.page() + 1);
            return;
        }
        if (!GazetteLayout.isContentSlot(slot)) {
            return;
        }
        int index = gui.page() * GazetteLayout.PAGE_SIZE + slot;
        if (index < 0 || index >= gui.volumes().size()) {
            return;
        }
        RegistrarCatalogue.Volume volume = gui.volumes().get(index);
        ItemStack book = new ItemBuilder(Material.WRITTEN_BOOK)
                .book(volume.title(), RegistrarCatalogue.AUTHOR, volume.pages())
                .build();
        player.closeInventory();
        player.openBook(Objects.requireNonNull(book, "book"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatalogueDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RegistrarGui) {
            event.setCancelled(true);
        }
    }

    private static Block blockOf(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null || inventory.getLocation() == null) {
            return null;
        }
        return inventory.getLocation().getBlock();
    }

    private void reopen(Player player, String kingdomId, int page) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            player.closeInventory();
            return;
        }
        Optional<RegistrarSite> anchor = kingdom.get().getParliamentSites().registrar();
        if (anchor.isEmpty()) {
            player.closeInventory();
            return;
        }
        World world = player.getServer().getWorld(anchor.get().worldName());
        if (world == null) {
            player.closeInventory();
            return;
        }
        openCatalogue(player, kingdomId, world, page);
    }

    private void openCatalogue(Player player, String kingdomId, World world, int page) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cUnknown kingdom."));
            return;
        }
        Optional<RegistrarSite> anchor = kingdom.get().getParliamentSites().registrar();
        if (anchor.isEmpty()) {
            player.sendMessage(c("&cThis kingdom has no registrar."));
            return;
        }
        List<RegistrarSite> cluster = RegistrarCluster.floodFill(
                anchor.get(), site -> isShelf(world, site), RegistrarCluster.MAX_SHELVES);
        List<RegistrarCatalogue.Volume> volumes =
                RegistrarCatalogue.organise(RegistrarArchive.scan(world, cluster));
        player.openInventory(Objects.requireNonNull(
                RegistrarGui.create(kingdomId, volumes, page).getInventory(), "registrar inventory"));
    }

    private boolean isSealedBlock(Block block) {
        if (block == null || block.getType() != Material.CHISELED_BOOKSHELF) {
            return false;
        }
        return !(claimOf(block) instanceof Claim.None);
    }

    private Claim claimOf(Block block) {
        RegistrarSite site = RegistrarSite.of(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        Map<String, RegistrarSite> anchors = new HashMap<>();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            kingdom.getParliamentSites().registrar().ifPresent(anchor -> anchors.put(kingdom.getId(), anchor));
        }
        if (anchors.isEmpty()) {
            return Claim.None.INSTANCE;
        }
        World world = block.getWorld();
        String match = null;
        for (Map.Entry<String, RegistrarSite> entry : anchors.entrySet()) {
            RegistrarSite anchor = entry.getValue();
            if (!anchor.worldName().equals(site.worldName())) {
                continue;
            }
            List<RegistrarSite> cluster = RegistrarCluster.floodFill(
                    anchor, candidate -> isShelf(world, candidate), RegistrarCluster.MAX_SHELVES);
            boolean contains = false;
            for (RegistrarSite member : cluster) {
                if (member.blockX() == site.blockX()
                        && member.blockY() == site.blockY()
                        && member.blockZ() == site.blockZ()) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                continue;
            }
            if (match != null) {
                return Claim.Ambiguous.INSTANCE;
            }
            match = entry.getKey();
        }
        if (match == null) {
            return Claim.None.INSTANCE;
        }
        return new Claim.Owned(match);
    }

    private static boolean isShelf(World world, RegistrarSite site) {
        if (!world.getName().equals(site.worldName())) {
            return false;
        }
        return world.getBlockAt(site.blockX(), site.blockY(), site.blockZ()).getType()
                == Material.CHISELED_BOOKSHELF;
    }

    private sealed interface Claim {
        enum None implements Claim {
            INSTANCE
        }

        enum Ambiguous implements Claim {
            INSTANCE
        }

        record Owned(String kingdomId) implements Claim {}
    }
}
