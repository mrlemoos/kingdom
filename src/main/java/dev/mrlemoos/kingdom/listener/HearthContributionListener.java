package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.hearth.BukkitHearthScan;
import dev.mrlemoos.kingdom.hearth.HearthConfig;
import dev.mrlemoos.kingdom.hearth.HearthContribution;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Member deliveries to a hearth, credited only for fuel the container accepted. */
public final class HearthContributionListener implements Listener {

    private final KingdomService kingdoms;
    private final KingdomTerritoryResolver territory;
    private final HearthConfig config;

    public HearthContributionListener(
            KingdomService kingdoms, KingdomTerritoryResolver territory, HearthConfig config) {
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
        this.territory = Objects.requireNonNull(territory, "territory");
        this.config = config == null ? HearthConfig.defaults() : config;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCampfire(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block campfire = event.getClickedBlock();
        if (campfire == null || !isCampfire(campfire.getType())) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir() || !config.isFuel(held.getType().name())) return;
        BlockData blockData = campfire.getBlockData();
        if (!(blockData instanceof Lightable lightable) || !lightable.isLit()) {
            event.setCancelled(true);
            RealmFeedback.instruct(player, "Light the campfire before provisioning it.");
            return;
        }

        Optional<String> owner = territory.owningKingdomId(
                campfire.getWorld().getName(), campfire.getX(), campfire.getY(), campfire.getZ());
        if (owner.isEmpty()) return;
        event.setCancelled(true);
        Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !owner.get().equals(membership.get().getKingdomId())) {
            RealmFeedback.refuse(player, "Only this realm's members may provision its hearths.");
            return;
        }
        Container container = BukkitHearthScan.adjacentContainer(campfire);
        if (container == null) {
            RealmFeedback.instruct(player, "Place a container touching the campfire, then right-click the campfire with fuel.");
            return;
        }
        int before = fuelCount(container);
        int needed = Math.max(0, config.fuelPerDay() - before);
        if (needed == 0) {
            RealmFeedback.instruct(player, "This hearth already holds one day's fuel.");
            return;
        }
        int offered = Math.min(held.getAmount(), needed);
        ItemStack delivery = held.clone();
        delivery.setAmount(offered);
        container.getInventory().addItem(delivery);
        int accepted = Math.max(0, fuelCount(container) - before);
        if (accepted <= 0) {
            RealmFeedback.refuse(player, "The hearth's container has no room for that fuel.");
            return;
        }
        Optional<HearthContribution> contribution = HearthContribution.plan(
                before, held.getAmount(), accepted, config.fuelPerDay());
        if (contribution.isEmpty()) return;
        HearthContribution added = contribution.get();
        if (added.added() == held.getAmount()) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            held.setAmount(held.getAmount() - added.added());
        }
        RealmFeedback.success(player.getLocation());
        RealmFeedback.instruct(player, "Added " + added.added() + " fuel. "
                + (added.remaining() == 0 ? "This hearth has a day's fuel." : added.remaining() + " more fuel needed."));
        if (before < config.fuelPerDay() && added.remaining() == 0) {
            Optional<Kingdom> kingdom = kingdoms.getKingdom(owner.get());
            if (kingdom.isPresent()) {
                var supply = BukkitHearthScan.supplyIn(
                        campfire.getWorld(), kingdom.get().getWorldGuardRegions(), config);
                if (supply.complete() && supply.hearths() > 0 && supply.lowHearths() == 0) {
                    RealmFeedback.kingdomMessage(
                            kingdoms, owner.get(), "&6" + player.getName() + " provisioned the realm's hearths for a day.");
                }
            }
        }
    }

    private int fuelCount(Container container) {
        int count = 0;
        for (ItemStack stack : container.getInventory().getContents()) {
            if (stack != null && config.isFuel(stack.getType().name())) count += stack.getAmount();
        }
        return count;
    }

    private static boolean isCampfire(Material material) {
        return material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }
}
