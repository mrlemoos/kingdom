package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

public final class MintLecternGuard {

    private MintLecternGuard() {
    }

    public static boolean shouldReleaseClaim(
            boolean treasuryLord,
            Location jobSite,
            Location potentialJobSite,
            List<MintLocation> mintLocations) {
        if (treasuryLord) {
            return false;
        }
        for (MintLocation mint : mintLocations) {
            if (matchesMintLectern(mint, jobSite) || matchesMintLectern(mint, potentialJobSite)) {
                return true;
            }
        }
        return false;
    }

    public static void releaseClaim(Villager villager) {
        villager.setMemory(org.bukkit.entity.memory.MemoryKey.JOB_SITE, null);
        villager.setMemory(org.bukkit.entity.memory.MemoryKey.POTENTIAL_JOB_SITE, null);
        if (villager.getProfession() == Villager.Profession.LIBRARIAN) {
            villager.setProfession(Villager.Profession.NONE);
        }
    }

    private static boolean matchesMintLectern(MintLocation mint, Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return mint.worldName().equals(location.getWorld().getName())
                && mint.x() == location.getBlockX()
                && mint.y() == location.getBlockY()
                && mint.z() == location.getBlockZ();
    }
}
