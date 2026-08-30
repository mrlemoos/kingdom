package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.BuildRefusalThrottle;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.CityResult;
import dev.mrlemoos.kingdom.city.HorsePermitEnforcer;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A saddled horse belongs to whoever saddled it: the permit is entered on the realm's register and
 * nobody but its holder — or the King, Queen or Prince of the holder's realm — may ride it, open
 * its saddlebags or lead it away.
 */
public final class HorsePermitListener implements Listener {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final CityService cityService;
    private final YamlKingdomStore store;
    private final BuildRefusalThrottle refusalThrottle = new BuildRefusalThrottle();

    public HorsePermitListener(
            JavaPlugin plugin,
            KingdomService kingdomService,
            CityService cityService,
            YamlKingdomStore store) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.cityService = Objects.requireNonNull(cityService, "cityService");
        this.store = Objects.requireNonNull(store, "store");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractHorse horse)) {
            return;
        }
        if (refuse(event.getPlayer(), horse)) {
            event.setCancelled(true);
            return;
        }
        EquipmentSlot hand = saddleHand(event.getPlayer());
        if (hand == null || hand != event.getHand()) {
            return;
        }
        if (saddleUp(event.getPlayer(), horse, hand)) {
            event.setCancelled(true);
        }
        // The saddle goes on a tick after the click, so the register is read once it is there.
        plugin.getServer().getScheduler().runTask(plugin, () -> claim(event.getPlayer(), horse));
    }

    /**
     * Vanilla makes a rider open the saddlebags to saddle a horse; here a saddle in hand is enough,
     * on a horse the player has tamed and nobody else has claimed.
     *
     * @return whether the saddle was buckled on, and so whether the click is spent
     */
    private boolean saddleUp(Player player, AbstractHorse horse, EquipmentSlot hand) {
        if (horse instanceof Llama || !horse.isTamed() || isSaddled(horse)) {
            return false;
        }
        AnimalTamer owner = horse.getOwner();
        if (owner != null && !player.getUniqueId().equals(owner.getUniqueId())) {
            return false;
        }
        ItemStack held = player.getInventory().getItem(hand);
        ItemStack saddle = held.clone();
        saddle.setAmount(1);
        horse.getInventory().setSaddle(saddle);
        if (player.getGameMode() != GameMode.CREATIVE) {
            held.setAmount(held.getAmount() - 1);
        }
        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbstractHorse horse)
                || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (refuse(player, horse)) {
            event.setCancelled(true);
        }
    }

    /** Saddlebags closed: whoever put the saddle on has claimed the horse. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AbstractHorse horse
                && event.getPlayer() instanceof Player player) {
            claim(player, horse);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMount(VehicleEnterEvent event) {
        if (event.getVehicle() instanceof AbstractHorse horse
                && event.getEntered() instanceof Player player
                && refuse(player, horse)) {
            event.setCancelled(true);
        }
    }

    /** A dead horse holds no permit. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse
                && cityService.revokeHorsePermitEverywhere(horse.getUniqueId())) {
            store.saveFrom(kingdomService);
        }
    }

    /** Enters an unclaimed, freshly saddled horse on its saddler's realm register. */
    private void claim(Player player, AbstractHorse horse) {
        if (!horse.isValid() || !isSaddled(horse) || cityService.horseOwner(horse.getUniqueId()).isPresent()) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        CityResult result = cityService.grantHorsePermit(
                membership.get().getKingdomId(), horse.getUniqueId(), player.getUniqueId());
        if (!(result instanceof CityResult.Success)) {
            return;
        }
        if (horse.getOwner() == null) {
            horse.setOwner(player);
        }
        store.saveFrom(kingdomService);
        player.sendMessage(c("&aThis horse now stands on your name in the realm's register."));
    }

    /** The hand carrying a saddle, main hand first, or null when neither does. */
    private static EquipmentSlot saddleHand(Player player) {
        if (player.getInventory().getItemInMainHand().getType() == Material.SADDLE) {
            return EquipmentSlot.HAND;
        }
        if (player.getInventory().getItemInOffHand().getType() == Material.SADDLE) {
            return EquipmentSlot.OFF_HAND;
        }
        return null;
    }

    private static boolean isSaddled(AbstractHorse horse) {
        ItemStack saddle = horse.getInventory().getSaddle();
        return saddle != null && !saddle.getType().isAir();
    }

    /** True when this player must be turned away from the horse; tells them so, at most now and then. */
    private boolean refuse(Player player, AbstractHorse horse) {
        Optional<UUID> owner = cityService.horseOwner(horse.getUniqueId());
        if (owner.isEmpty()) {
            return false;
        }
        Optional<PlayerMembership> ownerMembership = kingdomService.getMembership(owner.get());
        Optional<PlayerMembership> actorMembership = kingdomService.getMembership(player.getUniqueId());
        boolean sameRealm = ownerMembership.isPresent()
                && actorMembership.isPresent()
                && ownerMembership.get().getKingdomId().equals(actorMembership.get().getKingdomId());
        NobleRank actorRank = actorMembership.isPresent() ? actorMembership.get().getRank() : null;
        if (HorsePermitEnforcer.mayHandle(owner.get(), player.getUniqueId(), actorRank, sameRealm)) {
            return false;
        }
        if (refusalThrottle.shouldSend(player.getUniqueId(), System.currentTimeMillis())) {
            player.sendMessage(c("&cThis horse belongs to " + ownerName(owner.get()) + "."));
        }
        return true;
    }

    private static String ownerName(UUID ownerId) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        String name = owner.getName();
        return name == null ? "another subject" : name;
    }
}
