package dev.mrlemoos.kingdom.mint;

import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class TreasuryLordTargetScan {

    private TreasuryLordTargetScan() {}

    public static Optional<Entity> targetedEntity(Player player, double maxDistance) {
        RayTraceResult trace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                entity -> !entity.equals(player));
        if (trace == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trace.getHitEntity());
    }
}
