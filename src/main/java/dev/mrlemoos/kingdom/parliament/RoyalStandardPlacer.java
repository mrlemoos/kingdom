package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

/** Raises the kingdom flag (Royal Standard) beside each kingdom's House of Lords. */
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

    /**
     * Clears a prior standard when Lords moves, then raises the stored (or Crown-default) flag at
     * the new spot.
     */
    public boolean moveAndRaise(String kingdomId, Optional<ChamberSite> previousLords) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        if (previousLords.isPresent()) {
            clearAt(previousLords.get());
        }
        return raiseFor(kingdom.get());
    }

    public void clearAt(ChamberSite lords) {
        Optional<RoyalStandard.StandardPosition> position = RoyalStandard.positionFor(lords);
        if (position.isEmpty()) {
            return;
        }
        RoyalStandard.StandardPosition standard = position.get();
        World world = Bukkit.getWorld(standard.worldName());
        if (world == null) {
            return;
        }
        KingdomFlagItems.clearIfBanner(world.getBlockAt(standard.x(), standard.y(), standard.z()));
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
        KingdomFlag flag = kingdom.getFlag().orElseGet(KingdomFlag::crownDefault);
        Block block = world.getBlockAt(standard.x(), standard.y(), standard.z());
        return KingdomFlagItems.placeOn(block, flag);
    }
}
