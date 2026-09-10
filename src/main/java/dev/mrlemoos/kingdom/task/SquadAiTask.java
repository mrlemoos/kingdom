package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadEntityService;
import dev.mrlemoos.kingdom.war.squad.Squad;
import dev.mrlemoos.kingdom.war.squad.SquadMember;
import dev.mrlemoos.kingdom.war.squad.SquadMoralePolicy;
import dev.mrlemoos.kingdom.war.squad.SquadService;
import dev.mrlemoos.kingdom.war.squad.SquadState;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Capped, main-thread native-mob orders. Pathfinding is deliberately left to Bukkit. */
public final class SquadAiTask implements Runnable {

    private final SquadService squads;
    private final KingdomService kingdoms;
    private final WarService wars;
    private final CrownSquadEntityService crownSquads;
    private final Map<UUID, String> routedVillagers = new HashMap<>();

    public SquadAiTask(
            SquadService squads, KingdomService kingdoms, WarService wars, CrownSquadEntityService crownSquads) {
        this.squads = squads;
        this.kingdoms = kingdoms;
        this.wars = wars;
        this.crownSquads = crownSquads;
    }

    @Override
    public void run() {
        if (!wars.config().enabled() || !squads.config().enabled()) return;
        applyMoralePolicies();
        guideRoutedVillagers();
        for (Squad squad : squads.allView()) {
            Player officer = Bukkit.getPlayer(squad.officerId());
            if (officer == null || !officer.isOnline()) continue;
            SquadMoralePolicy.Behaviour behaviour = SquadMoralePolicy.behaviour(squads.officerMorale(squad));
            for (SquadMember member : squad.members()) {
                Entity entity = entity(member);
                if (!(entity instanceof Mob mob) || entity.isDead()) continue;
                if (behaviour == SquadMoralePolicy.Behaviour.HESITATE) {
                    mob.setTarget(null);
                    continue;
                }
                if (behaviour == SquadMoralePolicy.Behaviour.SCATTER) {
                    scatter(mob, officer);
                    continue;
                }
                switch (squad.state()) {
                    case IDLE -> mob.setTarget(null);
                    case FOLLOW -> follow(mob, officer);
                    case ATTACK -> mob.setTarget(enemy(officer, squad.kingdomId()));
                    case ROUTED -> { }
                }
            }
        }
    }

    private void applyMoralePolicies() {
        for (Squad squad : squads.allView()) {
            if (SquadMoralePolicy.behaviour(squads.officerMorale(squad)) == SquadMoralePolicy.Behaviour.ROUT) {
                for (SquadMember member : squad.members()) {
                    if (member instanceof SquadMember.PressedVillager) routedVillagers.put(member.id(), squad.kingdomId());
                    else {
                        Entity entity = entity(member);
                        if (entity != null) entity.remove();
                    }
                }
            }
            squads.applyMoralePolicy(squad.id());
        }
    }

    private Entity entity(SquadMember member) {
        if (member instanceof SquadMember.CrownUnit && crownSquads != null) {
            return crownSquads.find(member.id()).orElse(null);
        }
        return Bukkit.getEntity(member.id());
    }

    private void guideRoutedVillagers() {
        routedVillagers.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || entity.isDead()) return true;
            Optional<Kingdom> kingdom = kingdoms.getKingdom(entry.getValue());
            if (kingdom.isEmpty()) return true;
            Kingdom home = kingdom.get();
            if (!mob.getWorld().getName().equals(kingdoms.resolveWorldName(home)) || !home.hasWorldGuardRegions()) return true;
            Optional<WorldGuardBridge.RegionBounds> bounds = WorldGuardBridge.regionBounds(mob.getWorld().getName(), home.getWorldGuardRegions().get(0));
            if (bounds.isEmpty()) return true;
            Location target = new Location(mob.getWorld(), (bounds.get().minX() + bounds.get().maxX()) / 2.0d, mob.getLocation().getY(), (bounds.get().minZ() + bounds.get().maxZ()) / 2.0d);
            if (mob.getLocation().distanceSquared(target) <= 9d) return true;
            moveTowards(mob, target);
            return false;
        });
    }

    private void follow(Mob unit, Player officer) {
        unit.setTarget(null);
        if (!unit.getWorld().equals(officer.getWorld())) return;
        double distance = unit.getLocation().distanceSquared(officer.getLocation());
        if (distance <= 9d) return;
        if (distance > 144d) {
            unit.teleport(officer.getLocation());
            return;
        }
        moveTowards(unit, officer.getLocation());
    }

    private void scatter(Mob unit, Player officer) {
        unit.setTarget(null);
        if (!unit.getWorld().equals(officer.getWorld())) return;
        Vector direction = unit.getLocation().toVector().subtract(officer.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() == 0) direction = new Vector(1, 0, 0);
        unit.setVelocity(direction.normalize().multiply(.35d));
    }

    private void moveTowards(Mob unit, Location target) {
        Vector direction = target.toVector().subtract(unit.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() > 0) unit.setVelocity(direction.normalize().multiply(.35d));
    }

    private LivingEntity enemy(Player officer, String kingdomId) {
        Optional<ActiveWar> war = wars.activeWarFor(kingdomId);
        if (war.isEmpty()) return null;
        String enemyKingdom = war.get().attackerKingdomId().equals(kingdomId)
                ? war.get().defenderKingdomId()
                : war.get().attackerKingdomId();
        for (Entity nearby : officer.getNearbyEntities(16, 8, 16)) {
            if (nearby instanceof Player player) {
                Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
                if (membership.isPresent() && enemyKingdom.equals(membership.get().getKingdomId())) return player;
            }
        }
        return null;
    }
}
