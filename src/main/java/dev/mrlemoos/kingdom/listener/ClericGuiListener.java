package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.ClericService;
import dev.mrlemoos.kingdom.church.gui.CoronationGui;
import dev.mrlemoos.kingdom.church.gui.OathOfServiceGui;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.oath.OathResult;
import dev.mrlemoos.kingdom.war.oath.OathService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantInventory;

/**
 * The cleric keeps no shop. A right-click opens the Coronation for the King, Queen or a Prince of
 * the realm the cleric serves, and is refused to everybody else — the vanilla trading window never
 * opens either way.
 */
public final class ClericGuiListener implements Listener {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final ClericService clericService;
    private final YamlKingdomStore store;
    private final OathService oathService;

    public ClericGuiListener(
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService,
            YamlKingdomStore store,
            OathService oathService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.clericService = Objects.requireNonNull(clericService, "clericService");
        this.store = Objects.requireNonNull(store, "store");
        this.oathService = Objects.requireNonNull(oathService, "oathService");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractCleric(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !clericService.isCleric(event.getRightClicked())) {
            return;
        }
        // Cancelled whoever clicks: the cleric's trades are not for sale.
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<String> kingdomId = clericService.kingdomIdOf(event.getRightClicked());
        if (kingdomId.isEmpty()) {
            player.sendMessage(c("&cThis cleric serves no kingdom."));
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId.get());
        if (kingdom.isEmpty()) {
            player.sendMessage(c("&cThis cleric serves no kingdom."));
            return;
        }
        if (!oathService.config().enabled()) {
            player.sendMessage(c("&7The cleric is at prayer, and keeps no trades."));
            return;
        }
        if (!ChurchPresence.atChurch(churchService, kingdomId.get(), player)) {
            player.sendMessage(c("&cStand at the church to swear the oath of service."));
            return;
        }

        if (!isRoyalOf(kingdomId.get(), player.getUniqueId())) {
            openOath(player, kingdom.get());
            return;
        }

        Optional<UUID> monarch = kingdomService.findMonarch(kingdomId.get()).map(PlayerMembership::getPlayerId);
        String monarchName = monarch.isPresent() ? Bukkit.getOfflinePlayer(monarch.get()).getName() : null;
        boolean crowned = monarch.isPresent() && churchService.isCrowned(kingdomId.get(), monarch.get());
        player.openInventory(CoronationGui.create(
                        kingdomId.get(), kingdom.get().getDisplayName(), monarchName, crowned)
                .getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoronationClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CoronationGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isLeaveSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        if (gui.isOathSlot(event.getSlot())) {
            swear(player, gui.kingdomId());
            return;
        }
        if (!gui.isCrownSlot(event.getSlot()) || !isRoyalOf(gui.kingdomId(), player.getUniqueId())) {
            return;
        }
        crown(player, gui.kingdomId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOathClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OathOfServiceGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (gui.isLeaveSlot(event.getSlot())) {
            player.closeInventory();
            return;
        }
        if (!gui.isSwearSlot(event.getSlot()) || !ChurchPresence.atChurch(churchService, gui.kingdomId(), player)) {
            return;
        }
        swear(player, gui.kingdomId());
    }

    private void swear(Player player, String kingdomId) {
        if (!ChurchPresence.atChurch(churchService, kingdomId, player)) {
            player.sendMessage(c("&cStand at the church to swear the oath of service."));
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        OathResult result;
        if (membership.isPresent()) {
            if (!kingdomId.equals(membership.get().getKingdomId())) {
                player.sendMessage(c("&cYour oath belongs at your own realm's church."));
                return;
            }
            result = oathService.swearAsMember(player.getUniqueId());
        } else {
            result = oathService.swearAsOutsider(
                    kingdomId, player.getUniqueId(), "service to " + realmName(kingdomId));
        }
        if (result instanceof OathResult.Success) {
            store.saveFrom(kingdomService);
            player.sendMessage(c("&a" + messageOf(result)));
            player.closeInventory();
        } else {
            player.sendMessage(c("&c" + messageOf(result)));
        }
    }

    /** Belt and braces: any other road to the cleric's trading window is closed too. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenTrades(InventoryOpenEvent event) {
        if (event.getInventory() instanceof MerchantInventory merchant
                && merchant.getMerchant() instanceof Entity entity
                && clericService.isCleric(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CoronationGui
                || event.getInventory().getHolder() instanceof OathOfServiceGui) {
            event.setCancelled(true);
        }
    }

    /** The coronation itself: the only road to it, now that the command is gone. */
    private void crown(Player player, String kingdomId) {
        Optional<UUID> monarch = kingdomService.findMonarch(kingdomId).map(PlayerMembership::getPlayerId);
        if (monarch.isEmpty()) {
            player.sendMessage(c("&cThis realm has no monarch to crown."));
            return;
        }
        Player crowned = Bukkit.getPlayer(monarch.get());
        if (crowned == null || !ChurchPresence.atChurch(churchService, kingdomId, crowned)) {
            player.sendMessage(c("&cThe monarch must stand at the church to be crowned."));
            return;
        }
        Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
        ChurchResult result = churchService.crown(kingdomId, celebrant, monarch.get());
        if (result instanceof ChurchResult.Success) {
            player.sendMessage(c("&a" + result.message()));
            store.saveFrom(kingdomService);
            Bukkit.broadcastMessage(c("&6")
                    + kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId)
                    + " has crowned its monarch.");
            player.closeInventory();
        } else {
            player.sendMessage(c("&c" + result.message()));
        }
    }

    private void openOath(Player player, Kingdom kingdom) {
        boolean member = kingdomService.getMembership(player.getUniqueId()).isPresent();
        player.openInventory(OathOfServiceGui.create(kingdom.getId(), kingdom.getDisplayName(), member).getInventory());
    }

    private String realmName(String kingdomId) {
        return kingdomService.getKingdom(kingdomId).map(Kingdom::getDisplayName).orElse(kingdomId);
    }

    private static String messageOf(OathResult result) {
        if (result instanceof OathResult.Success success) {
            return success.message();
        }
        if (result instanceof OathResult.Disabled disabled) {
            return disabled.message();
        }
        return ((OathResult.Failure) result).message();
    }

    /** The King, Queen or a Prince of the realm the cleric serves. */
    private boolean isRoyalOf(String kingdomId, UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        return membership.isPresent()
                && kingdomId.equals(membership.get().getKingdomId())
                && CityService.isRoyalExempt(membership.get().getRank());
    }
}
