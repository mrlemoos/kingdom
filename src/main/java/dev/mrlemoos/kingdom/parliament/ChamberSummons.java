package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * The world-side plumbing every chamber ceremony shares: finding a landing that will not suffocate
 * whoever is summoned, turning them to face the Crown, and listing who is present to summon.
 */
final class ChamberSummons {

    private ChamberSummons() {}

    static Location landingAt(World world, int x, int startY, int z, Location fallbackFacing) {
        OptionalInt feetY = SafeChamberLanding.findFeetY(
                (bx, by, bz) -> world.getBlockAt(bx, by, bz).isPassable(),
                x,
                startY,
                z,
                world.getMinHeight(),
                world.getMaxHeight());
        int y = feetY.orElseGet(() -> world.getHighestBlockYAt(x, z) + 1);
        return new Location(world, x + 0.5, y, z + 0.5, fallbackFacing.getYaw(), fallbackFacing.getPitch());
    }

    static Location landingFor(World world, Location anchor, int[] offset) {
        return landingAt(
                world,
                (int) Math.floor(anchor.getX()) + offset[0],
                (int) Math.floor(anchor.getY()),
                (int) Math.floor(anchor.getZ()) + offset[1],
                anchor);
    }

    static Location landingFor(World world, ChamberSite site, int[] offset, Location fallbackFacing) {
        return landingAt(
                world,
                (int) Math.floor(site.x()) + offset[0],
                (int) Math.floor(site.y()),
                (int) Math.floor(site.z()) + offset[1],
                fallbackFacing);
    }

    /** Turns a landing to look at the focus, so the House faces the Crown it was summoned by. */
    static Location facing(Location landing, Location focus) {
        double dx = focus.getX() - landing.getX();
        double dz = focus.getZ() - landing.getZ();
        landing.setYaw((float) (Math.toDegrees(Math.atan2(-dx, dz))));
        landing.setPitch(0f);
        return landing;
    }

    static List<Player> onlineMembers(KingdomService kingdomService, String kingdomId) {
        List<Player> members = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            kingdomService.getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (kingdomId.equals(membership.getKingdomId())) {
                    members.add(online);
                }
            });
        }
        return members;
    }
}
