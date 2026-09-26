package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.GolemOfficerKind;
import dev.mrlemoos.kingdom.model.police.GolemOrder;
import dev.mrlemoos.kingdom.police.BuiltGolemOath;
import dev.mrlemoos.kingdom.police.GolemBuilderMatcher;
import dev.mrlemoos.kingdom.police.PoliceGolemOrderGui;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public final class PoliceGolemListener implements Listener {

    private final PoliceService policeService;
    private final PoliceGolemService golemService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final JavaPlugin plugin;
    private final TerritoryResolver territoryResolver;
    private final GolemBuilderMatcher builders = new GolemBuilderMatcher();

    public PoliceGolemListener(
            JavaPlugin plugin,
            PoliceService policeService,
            PoliceGolemService golemService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            TerritoryResolver territoryResolver) {
        this.plugin = plugin;
        this.policeService = policeService;
        this.golemService = golemService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.territoryResolver = territoryResolver;
    }

    // --- a golem built by a subject takes the oath ------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeadPlaced(BlockPlaceEvent event) {
        Material placed = event.getBlockPlaced().getType();
        if (placed != Material.CARVED_PUMPKIN && placed != Material.JACK_O_LANTERN) {
            return;
        }
        Location at = event.getBlockPlaced().getLocation();
        if (at.getWorld() == null) {
            return;
        }
        builders.headPlaced(
                event.getPlayer().getUniqueId(),
                at.getWorld().getName(),
                at.getBlockX(),
                at.getBlockY(),
                at.getBlockZ(),
                System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGolemBuilt(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
                || !(event.getEntity() instanceof IronGolem golem)) {
            return;
        }
        // The head's place event and the golem's rising may land in either order; look a tick later.
        Bukkit.getScheduler().runTask(plugin, () -> swearIn(golem));
    }

    private void swearIn(IronGolem golem) {
        if (!golem.isValid() || golemService.isPoliceGolem(golem)) {
            return;
        }
        Location at = golem.getLocation();
        if (at.getWorld() == null) {
            return;
        }
        Optional<UUID> builderId = builders.builderOf(
                at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ(), System.currentTimeMillis());
        if (builderId.isEmpty()) {
            return;
        }
        Player builder = Bukkit.getPlayer(builderId.get());
        Optional<PlayerMembership> membership = kingdomService.getMembership(builderId.get());
        if (builder == null || membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        boolean inOwnTerritory = territoryResolver
                        .resolve(at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ(), kingdomId)
                        .type()
                == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
        var police = policeService.policeState(kingdomId);
        if (police == null) {
            return;
        }
        int cap = policeService.config().maxPatrolGolems();
        BuiltGolemOath oath = BuiltGolemOath.decide(
                membership.get().getRank(), builder.isOp(), inOwnTerritory, police.patrolGolemCount(), cap);
        if (oath == BuiltGolemOath.SWORN
                && !(policeService.registerPatrolGolem(kingdomId, golem.getUniqueId()) instanceof PoliceResult.Success)) {
            oath = BuiltGolemOath.CAPPED;
        }
        if (oath == BuiltGolemOath.CAPPED) {
            RealmFeedback.refuse(
                    builder, "The watch is full (" + cap + " of " + cap + " patrol); this golem stays common.");
            return;
        }
        if (oath != BuiltGolemOath.SWORN) {
            return;
        }
        golemService.adoptPatrol(kingdomId, golem);
        store.saveFrom(kingdomService);
        RealmFeedback.milestone(List.of(builder), "&9Sworn to the watch", "&7Your golem walks the beat as a constable");
        RealmFeedback.success(at);
        builder.sendMessage(c("&9[Police] &fThe iron golem you built is sworn to the watch as a patrol constable ("
                + police.patrolGolemCount() + " of " + cap + ")."));
    }

    // --- the orders window -------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof IronGolem golem) || !golemService.isPoliceGolem(golem)) {
            return;
        }
        Optional<GolemOfficerKind> kind = golemService.kindForGolem(golem);
        if (kind.isEmpty()) {
            return;
        }
        Optional<String> kingdomId = golemService.kingdomIdForGolem(golem);
        if (kingdomId.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!mayCommand(player, kingdomId.get())) {
            player.sendMessage(c("&cOnly the Crown may give orders to a constable."));
            return;
        }
        player.openInventory(PoliceGolemOrderGui.create(
                        golem.getUniqueId(), kind.get(), golemService.orderForGolem(golem))
                .getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PoliceGolemOrderGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        GolemOrder order = gui.orderForSlot(event.getRawSlot());
        GolemOfficerKind newKind = gui.kindForSlot(event.getRawSlot());
        boolean standDown = PoliceGolemOrderGui.isStandDown(event.getRawSlot());
        if (order == null && newKind == null && !standDown) {
            return;
        }

        player.closeInventory();
        Entity entity = Bukkit.getEntity(gui.golemId());
        if (!(entity instanceof IronGolem golem) || !golemService.isPoliceGolem(golem)) {
            player.sendMessage(c("&cThat constable is no longer on duty."));
            return;
        }
        Optional<String> kingdomId = golemService.kingdomIdForGolem(golem);
        if (kingdomId.isEmpty() || !mayCommand(player, kingdomId.get())) {
            player.sendMessage(c("&cOnly the Crown may give orders to a constable."));
            return;
        }

        if (newKind != null) {
            PoliceResult result = policeService.reassignGolem(kingdomId.get(), golem.getUniqueId(), newKind);
            if (!(result instanceof PoliceResult.Success)) {
                RealmFeedback.refuse(player, result.message());
                return;
            }
            golemService.convertGolem(golem, newKind);
            store.saveFrom(kingdomService);
            RealmFeedback.success(golem.getLocation());
            player.sendMessage(c("&9[Police] &f" + result.message()));
            return;
        }
        if (standDown || order == null) {
            Location post = golem.getLocation();
            policeService.deregisterGolem(kingdomId.get(), golem.getUniqueId());
            golemService.removeGolem(golem);
            store.saveFrom(kingdomService);
            RealmFeedback.success(post);
            player.sendMessage(c("&9[Police] &fThe constable stands down and leaves the watch."));
            return;
        }
        golemService.applyOrder(golem, order, player);
        player.sendMessage(switch (order) {
            case FOLLOW -> c("&aThe constable falls in behind you.");
            case STAY -> c("&aThe constable holds this post.");
            case PATROL -> c("&aThe constable resumes patrol.");
        });
    }

    private boolean mayCommand(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !membership.get().hasNobleTitle()) {
            return false;
        }
        return kingdomId.equals(membership.get().getKingdomId())
                && PoliceGolemOrderGui.canCommand(membership.get().getRank());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        handleRemoval(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRemove(EntityRemoveEvent event) {
        handleRemoval(event.getEntity());
    }

    private void handleRemoval(Entity entity) {
        if (!golemService.isPoliceGolem(entity)) {
            return;
        }
        UUID entityId = entity.getUniqueId();
        Optional<String> kingdomId = golemService.kingdomIdForGolem(entity);
        if (kingdomId.isEmpty()) {
            kingdomId = policeService.findKingdomForRegisteredGolem(entityId);
        }
        if (kingdomId.isEmpty()) {
            return;
        }
        if (policeService.deregisterGolem(kingdomId.get(), entityId) instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
    }
}
