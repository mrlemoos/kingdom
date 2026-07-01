package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.GolemOfficerKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PoliceGolemService {

    private final KingdomService kingdomService;
    private final PoliceService policeService;
    private final NamespacedKey golemTagKey;
    private final NamespacedKey kingdomTagKey;
    private final NamespacedKey kindTagKey;

    public PoliceGolemService(JavaPlugin plugin, KingdomService kingdomService, PoliceService policeService) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.golemTagKey = new NamespacedKey(pluginRef, "police_golem");
        this.kingdomTagKey = new NamespacedKey(pluginRef, "police_kingdom");
        this.kindTagKey = new NamespacedKey(pluginRef, "police_golem_kind");
    }

    public IronGolem spawnPatrol(String kingdomId, Location location) {
        return spawnGolem(kingdomId, location, GolemOfficerKind.PATROL);
    }

    public IronGolem spawnGuard(String kingdomId, Location location) {
        return spawnGolem(kingdomId, location, GolemOfficerKind.GUARD);
    }

    public boolean isPoliceGolem(Entity entity) {
        if (!(entity instanceof IronGolem)) {
            return false;
        }
        Byte tag = entity.getPersistentDataContainer().get(golemTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    public Optional<String> kingdomIdForGolem(Entity entity) {
        if (!isPoliceGolem(entity)) {
            return Optional.empty();
        }
        String kingdomId = entity.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    public Optional<GolemOfficerKind> kindForGolem(Entity entity) {
        if (!isPoliceGolem(entity)) {
            return Optional.empty();
        }
        String kind = entity.getPersistentDataContainer().get(kindTagKey, PersistentDataType.STRING);
        if (kind == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(GolemOfficerKind.valueOf(kind));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<IronGolem> findGolemForDespawn(Player player, String kingdomId) {
        Optional<Entity> targeted = PoliceGolemTargetScan.targetedEntity(player, 5.0);
        if (targeted.isPresent()
                && targeted.get() instanceof IronGolem golem
                && isPoliceGolem(golem)
                && kingdomIdForGolem(golem).filter(kingdomId::equals).isPresent()) {
            return Optional.of(golem);
        }

        return findNearestRegisteredGolem(kingdomId, player.getLocation());
    }

    public void removeGolem(Entity entity) {
        entity.remove();
    }

    public void pruneStaleGolems() {
        for (var kingdom : kingdomService.listKingdoms()) {
            String kingdomId = kingdom.getId();
            Set<UUID> presentPatrol = new HashSet<>();
            Set<UUID> presentGuard = new HashSet<>();
            for (UUID golemId : kingdom.getPoliceState().patrolGolemsView()) {
                if (findGolemById(golemId).filter(g -> isValidGolem(g, kingdomId)).isPresent()) {
                    presentPatrol.add(golemId);
                }
            }
            for (UUID golemId : kingdom.getPoliceState().guardGolemsView()) {
                if (findGolemById(golemId).filter(g -> isValidGolem(g, kingdomId)).isPresent()) {
                    presentGuard.add(golemId);
                }
            }
            policeService.pruneStalePatrolGolems(kingdomId, presentPatrol);
            policeService.pruneStaleGuardGolems(kingdomId, presentGuard);
        }
    }

    private IronGolem spawnGolem(String kingdomId, Location location, GolemOfficerKind kind) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("World not loaded.");
        }
        return world.spawn(location, IronGolem.class, spawned -> configureGolem(spawned, kingdomId, kind));
    }

    private void configureGolem(IronGolem golem, String kingdomId, GolemOfficerKind kind) {
        boolean patrol = kind == GolemOfficerKind.PATROL;
        golem.setAI(patrol);
        golem.setSilent(true);
        golem.setInvulnerable(true);
        golem.setCollidable(true);
        golem.setRemoveWhenFarAway(false);
        golem.setCustomName(patrol ? PoliceAppearance.patrolGolemNametag() : PoliceAppearance.guardGolemNametag());
        golem.setCustomNameVisible(true);
        golem.getPersistentDataContainer().set(golemTagKey, PersistentDataType.BYTE, (byte) 1);
        golem.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
        golem.getPersistentDataContainer().set(kindTagKey, PersistentDataType.STRING, kind.name());
    }

    private Optional<IronGolem> findNearestRegisteredGolem(String kingdomId, Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return Optional.empty();
        }

        var police = kingdomService.getKingdom(kingdomId).orElseThrow().getPoliceState();
        IronGolem nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (UUID golemId : police.patrolGolemsView()) {
            Optional<IronGolem> golem = findGolemById(golemId).filter(g -> isValidGolem(g, kingdomId));
            if (golem.isPresent() && golem.get().getWorld().equals(world)) {
                double distance = golem.get().getLocation().distanceSquared(origin);
                if (distance < nearestDistance) {
                    nearest = golem.get();
                    nearestDistance = distance;
                }
            }
        }
        for (UUID golemId : police.guardGolemsView()) {
            Optional<IronGolem> golem = findGolemById(golemId).filter(g -> isValidGolem(g, kingdomId));
            if (golem.isPresent() && golem.get().getWorld().equals(world)) {
                double distance = golem.get().getLocation().distanceSquared(origin);
                if (distance < nearestDistance) {
                    nearest = golem.get();
                    nearestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private boolean isValidGolem(IronGolem golem, String kingdomId) {
        return golem.isValid()
                && !golem.isDead()
                && isPoliceGolem(golem)
                && kingdomIdForGolem(golem).filter(kingdomId::equals).isPresent();
    }

    private Optional<IronGolem> findGolemById(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(entityId) && entity instanceof IronGolem golem) {
                    return Optional.of(golem);
                }
            }
        }
        return Optional.empty();
    }
}
