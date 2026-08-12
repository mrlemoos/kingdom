package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Lord Mayor: the realm NPC standing at a kingdom's city hall to issue build permits. A tamed,
 * seated, invulnerable, AI-less wolf with a {@code [Lord Mayor]} nametag. It holds no wallet, is
 * never a territory villager, and takes no part in the villager economy.
 */
public final class LordMayorService {

    /** The nametag every Lord Mayor wears, always visible. */
    public static final String NAMETAG = ColourEncoder.c("&6Lord Mayor");

    private final KingdomService kingdomService;
    private final NamespacedKey mayorTagKey;
    private final NamespacedKey kingdomTagKey;

    public LordMayorService(JavaPlugin plugin, KingdomService kingdomService) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.mayorTagKey = new NamespacedKey(pluginRef, "lord_mayor");
        this.kingdomTagKey = new NamespacedKey(pluginRef, "lord_mayor_kingdom");
    }

    public NamespacedKey mayorTagKey() {
        return mayorTagKey;
    }

    /** True when this entity is a Lord Mayor of any kingdom. */
    public boolean isLordMayor(Entity entity) {
        if (entity == null) {
            return false;
        }
        Byte tag = entity.getPersistentDataContainer().get(mayorTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    /** The kingdom this Lord Mayor serves, if the entity is one. */
    public Optional<String> kingdomIdOf(Entity entity) {
        if (!isLordMayor(entity)) {
            return Optional.empty();
        }
        String kingdomId = entity.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    /** The live Lord Mayor of this kingdom, when one is standing. */
    public Optional<Wolf> findMayor(Kingdom kingdom) {
        if (kingdom == null) {
            return Optional.empty();
        }
        Optional<UUID> entityId = kingdom.getCityState().lordMayorEntityId();
        if (entityId.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = Bukkit.getEntity(entityId.get());
        if (entity instanceof Wolf wolf && wolf.isValid()) {
            return Optional.of(wolf);
        }
        return Optional.empty();
    }

    /**
     * Removes any previous Lord Mayor and stands a fresh one at the capital.
     *
     * @return the spawned mayor, or empty when the capital's world is not loaded
     */
    public Optional<Wolf> spawn(Kingdom kingdom, CapitalLocation capital) {
        if (kingdom == null || capital == null) {
            return Optional.empty();
        }
        despawn(kingdom);

        Optional<Location> site = toBukkitLocation(capital);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        Location location = site.get();
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        location.getChunk();

        String kingdomId = kingdom.getId();
        Wolf mayor = world.spawn(location, Wolf.class, spawned -> configure(spawned, kingdomId));
        kingdom.getCityState().setLordMayorEntityId(mayor.getUniqueId());
        return Optional.of(mayor);
    }

    /** Removes the kingdom's Lord Mayor, if any, and forgets it. */
    public void despawn(Kingdom kingdom) {
        if (kingdom == null) {
            return;
        }
        KingdomCityState city = kingdom.getCityState();
        Optional<UUID> entityId = city.lordMayorEntityId();
        if (entityId.isPresent()) {
            Entity entity = Bukkit.getEntity(entityId.get());
            if (entity != null) {
                entity.remove();
            }
        }
        city.clearLordMayorEntityId();
    }

    /**
     * Stands a Lord Mayor wherever a capital exists and none is alive, and removes any left behind
     * by a dissolved capital.
     *
     * @return true when any kingdom's mayor state changed and the roll should be saved
     */
    public boolean reconcile(Kingdom kingdom) {
        if (kingdom == null) {
            return false;
        }
        KingdomCityState city = kingdom.getCityState();
        Optional<CapitalLocation> capital = city.capital();
        if (capital.isEmpty()) {
            if (city.lordMayorEntityId().isEmpty()) {
                return false;
            }
            despawn(kingdom);
            return true;
        }
        Optional<Wolf> standing = findMayor(kingdom);
        if (standing.isPresent()) {
            configure(standing.get(), kingdom.getId());
            return false;
        }
        return spawn(kingdom, capital.get()).isPresent();
    }

    /** The periodic sweep and startup entry point: reconciles every kingdom's Lord Mayor. */
    public boolean reconcileAll() {
        boolean changed = false;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (reconcile(kingdom)) {
                changed = true;
            }
        }
        return changed;
    }

    private void configure(Wolf mayor, String kingdomId) {
        mayor.setTamed(true);
        mayor.setSitting(true);
        mayor.setAI(false);
        mayor.setInvulnerable(true);
        mayor.setPersistent(true);
        mayor.setRemoveWhenFarAway(false);
        mayor.setSilent(true);
        mayor.setCollarColor(DyeColor.YELLOW);
        mayor.setCustomName(NAMETAG);
        mayor.setCustomNameVisible(true);
        mayor.getPersistentDataContainer().set(mayorTagKey, PersistentDataType.BYTE, (byte) 1);
        mayor.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private static Optional<Location> toBukkitLocation(CapitalLocation capital) {
        World world = Bukkit.getWorld(capital.worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
                world, capital.x(), capital.y(), capital.z(), capital.yaw(), capital.pitch()));
    }
}
