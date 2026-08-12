package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/** Raises the Royal Standard beside each kingdom's House of Lords. */
public final class RoyalStandardPlacer {

    private final KingdomService kingdomService;

    public RoyalStandardPlacer(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    /** Raises the standard for every kingdom whose Lords point is set. */
    public void raiseAll() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            raiseFor(kingdom);
        }
    }

    /** Raises the standard for one kingdom, doing nothing when it has no Lords point. */
    public boolean raiseFor(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        return raiseFor(kingdom.get());
    }

    private boolean raiseFor(Kingdom kingdom) {
        Optional<ChamberSite> lords = kingdom.getParliamentSites().lords();
        if (lords.isEmpty()) {
            return false;
        }
        Optional<RoyalStandard.StandardPosition> position = RoyalStandard.positionFor(lords.get());
        if (position.isEmpty()) {
            return false;
        }
        RoyalStandard.StandardPosition standard = position.get();
        World world = Bukkit.getWorld(standard.worldName());
        if (world == null) {
            return false;
        }
        Material banner = Material.matchMaterial(RoyalStandard.crownBannerMaterial());
        if (banner == null) {
            return false;
        }
        Block block = world.getBlockAt(standard.x(), standard.y(), standard.z());
        if (block.getType() == banner) {
            return true;
        }
        // Never overwrite a peer's own building work: only air or a standard already flying.
        if (!block.isEmpty() && !isBanner(block.getType())) {
            return false;
        }
        block.setType(banner, false);
        return true;
    }

    private static boolean isBanner(Material material) {
        return material.name().endsWith("_BANNER");
    }
}
