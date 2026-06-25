package dev.leo.kingdom.election;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.election.MpSeat;
import dev.leo.kingdom.model.election.MpSeatKind;
import dev.leo.kingdom.model.election.MpSeatLocation;
import dev.leo.kingdom.service.KingdomService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerMpEntityService {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;

    public VillagerMpEntityService(JavaPlugin plugin, KingdomService kingdomService) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
    }

    public void syncKingdom(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(this::syncSeats);
    }

    public void syncAllKingdoms() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            syncSeats(kingdom);
        }
    }

    public void despawnKingdomVillagerMps(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
                if (seat.kind() == MpSeatKind.VILLAGER) {
                    despawnSeat(kingdomId, seat.index());
                }
            }
        });
    }

    public void despawnSeat(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).flatMap(k -> k.getElectionState().seat(seatIndex)).ifPresent(seat -> {
            if (seat.kind() == MpSeatKind.VILLAGER) {
                seat.entityId().ifPresent(this::removeEntity);
                seat.setEntityId(null);
            }
        });
    }

    private void syncSeats(Kingdom kingdom) {
        for (MpSeat seat : kingdom.getElectionState().seatsView().values()) {
            if (seat.kind() != MpSeatKind.VILLAGER || seat.profession().isEmpty()) {
                continue;
            }
            Optional<MpSeatLocation> location = kingdom.getElectionState().seatLocation(seat.index());
            if (location.isEmpty()) {
                continue;
            }
            if (seat.entityId().isPresent() && entityAlive(seat.entityId().get())) {
                refreshNametag(seat);
                continue;
            }
            spawnSeatEntity(kingdom, seat, location.get());
        }
    }

    private void spawnSeatEntity(Kingdom kingdom, MpSeat seat, MpSeatLocation seatLocation) {
        World world = plugin.getServer().getWorld(seatLocation.worldName());
        if (world == null) {
            return;
        }
        Location location = new Location(
                world,
                seatLocation.x(),
                seatLocation.y(),
                seatLocation.z(),
                seatLocation.yaw(),
                seatLocation.pitch());
        Villager villager = world.spawn(location, Villager.class, spawned -> configureVillager(spawned, seat));
        seat.setEntityId(villager.getUniqueId());
    }

    private void configureVillager(Villager villager, MpSeat seat) {
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setSilent(true);
        applyProfession(villager, seat.profession().orElse("none"));
        refreshNametag(villager, seat.profession().orElse("none"));
    }

    private void refreshNametag(MpSeat seat) {
        seat.entityId().flatMap(this::findEntity).ifPresent(entity -> {
            if (entity instanceof Villager villager) {
                refreshNametag(villager, seat.profession().orElse("none"));
            }
        });
    }

    private static void refreshNametag(Villager villager, String profession) {
        String label = profession.substring(0, 1).toUpperCase(Locale.ROOT)
                + profession.substring(1).toLowerCase(Locale.ROOT);
        villager.setCustomNameVisible(true);
        villager.setCustomName(
                NobleRank.MP.chatColor() + "" + ChatColor.BOLD + "[MP] " + ChatColor.RESET + label);
    }

    private static void applyProfession(Villager villager, String profession) {
        try {
            org.bukkit.entity.Villager.Profession bukkitProfession =
                    org.bukkit.entity.Villager.Profession.valueOf(profession.toUpperCase(Locale.ROOT));
            villager.setProfession(bukkitProfession);
        } catch (IllegalArgumentException ignored) {
            villager.setProfession(org.bukkit.entity.Villager.Profession.NONE);
        }
    }

    private boolean entityAlive(UUID entityId) {
        return findEntity(entityId).isPresent();
    }

    private Optional<Entity> findEntity(UUID entityId) {
        Entity entity = plugin.getServer().getEntity(entityId);
        return Optional.ofNullable(entity);
    }

    private void removeEntity(UUID entityId) {
        findEntity(entityId).ifPresent(Entity::remove);
    }
}
