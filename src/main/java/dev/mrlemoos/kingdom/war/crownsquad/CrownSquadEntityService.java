package dev.mrlemoos.kingdom.war.crownsquad;

import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Bukkit identity and lifecycle for ledgered crown squads. No command or squad AI lives here. */
public final class CrownSquadEntityService {

    private final CrownSquadService squads;
    private final EconomyService economy;
    private final NamespacedKey squadKey;
    private final NamespacedKey kingdomKey;
    private final EntityType entityType;

    public CrownSquadEntityService(JavaPlugin plugin, CrownSquadService squads, EconomyService economy) {
        this(plugin, squads, economy, entityType(squads.config().entityType()));
    }

    private static EntityType entityType(String configured) {
        try {
            EntityType type = EntityType.valueOf(configured);
            return switch (type) {
                case IRON_GOLEM, ZOMBIE, SKELETON -> type;
                default -> EntityType.IRON_GOLEM;
            };
        } catch (IllegalArgumentException ex) {
            return EntityType.IRON_GOLEM;
        }
    }

    CrownSquadEntityService(JavaPlugin plugin, CrownSquadService squads, EconomyService economy, EntityType entityType) {
        JavaPlugin pluginRef = Objects.requireNonNull(plugin, "plugin");
        this.squads = Objects.requireNonNull(squads, "squads");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.squadKey = new NamespacedKey(pluginRef, "crown_squad_unit");
        this.kingdomKey = new NamespacedKey(pluginRef, "crown_squad_kingdom");
    }

    public boolean spawn(CrownSquadUnit unit, Location mintLocation) {
        if (unit == null || mintLocation == null || mintLocation.getWorld() == null) return false;
        World world = mintLocation.getWorld();
        Entity entity = world.spawnEntity(mintLocation.clone().add(1.5, 0, 0.5), entityType);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return false;
        }
        living.setRemoveWhenFarAway(false);
        living.setCustomName("Crown Squad");
        living.setCustomNameVisible(true);
        living.getPersistentDataContainer().set(squadKey, PersistentDataType.STRING, unit.unitId().toString());
        living.getPersistentDataContainer().set(kingdomKey, PersistentDataType.STRING, unit.kingdomId());
        return true;
    }

    public void remove(CrownSquadUnit unit) {
        if (unit == null) return;
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            if (unit.unitId().toString().equals(entity.getPersistentDataContainer().get(squadKey, PersistentDataType.STRING))) entity.remove();
        }
    }

    /** Finds the live mob carrying this ledger unit's persistent identity. */
    public java.util.Optional<Entity> find(UUID unitId) {
        if (unitId == null) return java.util.Optional.empty();
        String expected = unitId.toString();
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            if (expected.equals(entity.getPersistentDataContainer().get(squadKey, PersistentDataType.STRING))) {
                return java.util.Optional.of(entity);
            }
        }
        return java.util.Optional.empty();
    }

    /** Recreates any ledgered unit missing from loaded worlds, at its kingdom's first mint. */
    public void reconcileAll() {
        Set<UUID> ledgered = squads.allUnitsView().stream().map(CrownSquadUnit::unitId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> present = new HashSet<>();
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            String raw = entity.getPersistentDataContainer().get(squadKey, PersistentDataType.STRING);
            if (raw == null) continue;
            try {
                UUID unitId = UUID.fromString(raw);
                if (ledgered.contains(unitId)) present.add(unitId); else entity.remove();
            } catch (IllegalArgumentException ignored) { entity.remove(); }
        }
        for (CrownSquadUnit unit : squads.allUnitsView()) {
            if (!present.contains(unit.unitId())) mintLocation(unit.kingdomId()).ifPresent(location -> spawn(unit, location));
        }
    }

    private java.util.Optional<Location> mintLocation(String kingdomId) {
        KingdomEconomy kingdom = economy.kingdomEconomies().get(kingdomId);
        if (kingdom == null || kingdom.mintLocations().isEmpty()) return java.util.Optional.empty();
        MintLocation mint = kingdom.mintLocations().get(0);
        World world = Bukkit.getWorld(mint.worldName());
        return world == null ? java.util.Optional.empty() : java.util.Optional.of(new Location(world, mint.x() + .5, mint.y(), mint.z() + .5));
    }
}
