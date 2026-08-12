package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.parliament.SafeChamberLanding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Summons the accused and the hearing party (judge or jurors) to the court, reusing the ceremony
 * landing geometry. Pre-trial only — the prison teleport bar does not apply yet.
 */
public final class CourtSummonService {

    private static final int STAND_OFF = 3;

    private final PoliceService policeService;

    public CourtSummonService(PoliceService policeService) {
        this.policeService = Objects.requireNonNull(policeService, "policeService");
    }

    public void summonHearing(String kingdomId, UUID accusedId, List<UUID> hearingPartyIds) {
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (court.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(court.get().worldName());
        if (world == null) {
            return;
        }
        Location focus = new Location(
                world, court.get().x() + 0.5, court.get().y(), court.get().z() + 0.5);
        List<UUID> party = new ArrayList<>();
        if (accusedId != null) {
            party.add(accusedId);
        }
        if (hearingPartyIds != null) {
            for (UUID id : hearingPartyIds) {
                if (id != null && !party.contains(id)) {
                    party.add(id);
                }
            }
        }
        List<int[]> offsets = SafeChamberLanding.frontOffsets(party.size(), focus.getYaw(), STAND_OFF);
        for (int i = 0; i < party.size(); i++) {
            teleportTo(world, focus, offsets.get(i), party.get(i));
        }
    }

    private static void teleportTo(World world, Location focus, int[] offset, UUID entityId) {
        Player player = Bukkit.getPlayer(entityId);
        if (player != null && player.isOnline()) {
            player.teleport(facing(landingFor(world, focus, offset), focus));
            return;
        }
        for (World loaded : Bukkit.getWorlds()) {
            for (Entity entity : loaded.getEntities()) {
                if (entity.getUniqueId().equals(entityId)) {
                    entity.teleport(facing(landingFor(world, focus, offset), focus));
                    return;
                }
            }
        }
    }

    private static Location landingFor(World world, Location anchor, int[] offset) {
        int x = (int) Math.floor(anchor.getX()) + offset[0];
        int z = (int) Math.floor(anchor.getZ()) + offset[1];
        int startY = (int) Math.floor(anchor.getY());
        OptionalInt feetY = SafeChamberLanding.findFeetY(
                (bx, by, bz) -> world.getBlockAt(bx, by, bz).isPassable(),
                x,
                startY,
                z,
                world.getMinHeight(),
                world.getMaxHeight());
        int y = feetY.orElseGet(() -> world.getHighestBlockYAt(x, z) + 1);
        return new Location(world, x + 0.5, y, z + 0.5, anchor.getYaw(), anchor.getPitch());
    }

    private static Location facing(Location landing, Location focus) {
        double dx = focus.getX() - landing.getX();
        double dz = focus.getZ() - landing.getZ();
        landing.setYaw((float) (Math.toDegrees(Math.atan2(-dx, dz))));
        landing.setPitch(0f);
        return landing;
    }
}
