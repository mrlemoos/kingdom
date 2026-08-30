package dev.mrlemoos.kingdom.church;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.church.KingdomChurchState;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The cleric: the villager standing at a kingdom's church whenever no player priest is sworn, or
 * while the priest is in a cell. Spawned fresh, as the villager Speaker is — it never claims a
 * territory villager, holds no wallet and takes no part in the villager economy.
 */
public final class ClericService {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final NamespacedKey clericTagKey;
    private final NamespacedKey kingdomTagKey;

    public ClericService(JavaPlugin plugin, KingdomService kingdomService, ChurchService churchService) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.clericTagKey = new NamespacedKey(pluginRef, "church_cleric");
        this.kingdomTagKey = new NamespacedKey(pluginRef, "church_cleric_kingdom");
    }

    public boolean isCleric(Entity entity) {
        if (entity == null) {
            return false;
        }
        Byte tag = entity.getPersistentDataContainer().get(clericTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    /** The kingdom this cleric serves, if the entity is one. */
    public Optional<String> kingdomIdOf(Entity entity) {
        if (!isCleric(entity)) {
            return Optional.empty();
        }
        String kingdomId = entity.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    public Optional<Villager> findCleric(Kingdom kingdom) {
        if (kingdom == null) {
            return Optional.empty();
        }
        Optional<UUID> entityId = kingdom.getChurchState().clericEntityId();
        if (entityId.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = Bukkit.getEntity(entityId.get());
        if (entity instanceof Villager villager && villager.isValid()) {
            return Optional.of(villager);
        }
        return Optional.empty();
    }

    /** Stands a fresh cleric at the church, removing any left standing there. */
    public Optional<Villager> spawn(Kingdom kingdom, ChurchSite site) {
        if (kingdom == null || site == null) {
            return Optional.empty();
        }
        despawn(kingdom);

        World world = Bukkit.getWorld(site.worldName());
        if (world == null) {
            return Optional.empty();
        }
        Location location = new Location(world, site.x(), site.y(), site.z(), site.yaw(), site.pitch());
        location.getChunk();

        String kingdomId = kingdom.getId();
        // A cleric in an unloaded chunk is invisible to Bukkit.getEntity, so sweeps could
        // otherwise stack villagers on the altar. Same guard as the Lord Mayor.
        for (Entity nearby : world.getNearbyEntities(location, 8, 8, 8)) {
            if (isCleric(nearby)) {
                nearby.remove();
            }
        }
        Villager cleric = world.spawn(location, Villager.class, spawned -> configure(spawned, kingdomId));
        kingdom.getChurchState().setClericEntityId(cleric.getUniqueId());
        return Optional.of(cleric);
    }

    public void despawn(Kingdom kingdom) {
        if (kingdom == null) {
            return;
        }
        KingdomChurchState church = kingdom.getChurchState();
        church.clericEntityId().ifPresent(entityId -> {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        });
        church.clearClericEntityId();
    }

    /**
     * Stands a cleric wherever one is wanted and none is alive, and dismisses one that is not.
     *
     * @return true when the roll changed and should be saved
     */
    public boolean reconcile(Kingdom kingdom) {
        if (kingdom == null) {
            return false;
        }
        KingdomChurchState church = kingdom.getChurchState();
        if (!churchService.clericWanted(kingdom.getId())) {
            if (church.clericEntityId().isEmpty()) {
                return false;
            }
            despawn(kingdom);
            return true;
        }
        Optional<ChurchSite> site = church.church();
        if (site.isEmpty() || !isSiteLoaded(site.get())) {
            return false;
        }
        Optional<Villager> standing = findCleric(kingdom);
        if (standing.isPresent()) {
            configure(standing.get(), kingdom.getId());
            return false;
        }
        return spawn(kingdom, site.get()).isPresent();
    }

    /** Startup and the 60-second territory sweep: reconciles every kingdom's cleric. */
    public boolean reconcileAll() {
        boolean changed = false;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (reconcile(kingdom)) {
                changed = true;
            }
        }
        return changed;
    }

    private void configure(Villager cleric, String kingdomId) {
        cleric.setProfession(Villager.Profession.CLERIC);
        cleric.setVillagerType(Villager.Type.PLAINS);
        cleric.setRecipes(new ArrayList<>());
        cleric.setAI(false);
        cleric.setSilent(true);
        cleric.setInvulnerable(true);
        cleric.setCollidable(false);
        cleric.setPersistent(true);
        cleric.setRemoveWhenFarAway(false);
        cleric.setCustomName(ChurchAppearance.clericNametag());
        cleric.setCustomNameVisible(true);
        cleric.getPersistentDataContainer().set(clericTagKey, PersistentDataType.BYTE, (byte) 1);
        cleric.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private static boolean isSiteLoaded(ChurchSite site) {
        World world = Bukkit.getWorld(site.worldName());
        if (world == null) {
            return false;
        }
        return world.isChunkLoaded(((int) Math.floor(site.x())) >> 4, ((int) Math.floor(site.z())) >> 4);
    }
}
