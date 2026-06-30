package dev.mrlemoos.kingdom.mint;

import java.util.List;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;

public final class MintLecternGuard {

    /**
     * Vanilla lectern employment profession. Distinct from
     * {@link TreasuryLordAppearance#PROFESSION},
     * which is cosmetic for the treasury lord NPC.
     */
    private static final Villager.Profession LECTERN_EMPLOYMENT_PROFESSION = Villager.Profession.LIBRARIAN;

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

    public static void releaseClaim(Villager villager, boolean treasuryLord) {
        Location employedSite = villager.getMemory(MemoryKey.JOB_SITE);
        Villager.Profession profession = villager.getProfession();
        villager.setMemory(MemoryKey.JOB_SITE, null);
        villager.setMemory(MemoryKey.POTENTIAL_JOB_SITE, null);
        if (treasuryLord) {
            return;
        }
        if (profession == LECTERN_EMPLOYMENT_PROFESSION) {
            villager.setProfession(Villager.Profession.NONE);
            return;
        }
        if (employedSite != null && profession == TreasuryLordAppearance.PROFESSION) {
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
