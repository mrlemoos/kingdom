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

    private final PoliceService policeService;
    private final PoliceCourtService courtService;

    public CourtSummonService(PoliceService policeService, PoliceCourtService courtService) {
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.courtService = Objects.requireNonNull(courtService, "courtService");
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
                world,
                court.get().x() + 0.5,
                court.get().y(),
                court.get().z() + 0.5,
                CourtBench.normaliseYaw(court.get().yaw()),
                0f);
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
        // The accused leads the party, so they take the centre of the first rank: the dock,
        // directly in front of the bench, with the hearing party ranked behind them.
        List<int[]> offsets =
                SafeChamberLanding.frontOffsets(party.size(), focus.getYaw(), CourtBench.DOCK_STAND_OFF);
        for (int i = 0; i < party.size(); i++) {
            teleportTo(world, focus, offsets.get(i), party.get(i));
        }
        if (accusedId != null && !offsets.isEmpty()) {
            int[] dock = offsets.get(0);
            courtService.faceJudgeTowards(
                    kingdomId, focus.getX() + dock[0], focus.getZ() + dock[1]);
        }
    }

    /** Returns the magistrate to the sited yaw once the hearing is over. */
    public void riseCourt(String kingdomId) {
        courtService.restoreJudgeFacing(kingdomId);
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
