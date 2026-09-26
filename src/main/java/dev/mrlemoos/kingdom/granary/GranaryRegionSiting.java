package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.granary.GranarySiting.Verdict;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser.Candidate;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import java.util.Optional;

/**
 * Linking and releasing the granary region by its hay-bale stone. The rules stay in
 * {@link GranarySiting}; this asks WorldGuard what lies around the bale and saves.
 */
public final class GranaryRegionSiting {

    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public GranaryRegionSiting(KingdomService kingdomService, YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.store = store;
    }

    /** Links the smallest region around the block inside territory; the stone's own verdict otherwise. */
    public GranarySiting.Stone linkAround(String kingdomId, NobleRank actorRank, String worldName, int x, int y, int z) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return new GranarySiting.Stone(Verdict.NO_TERRITORY, Optional.empty());
        }
        boolean worldGuard = WorldGuardBridge.isAvailable();
        List<Candidate> around = worldGuard
                ? SubregionChooser.candidates(worldName, WorldGuardBridge.regionsAt(worldName, x, y, z))
                : List.of();
        List<Candidate> territory = worldGuard
                ? SubregionChooser.candidates(worldName, kingdom.get().getWorldGuardRegions())
                : List.of();
        GranarySiting.Stone stone = GranarySiting.evaluateStone(actorRank, worldGuard, x, y, z, around, territory);
        if (stone.regionId().isPresent()) {
            kingdom.get().setGranaryRegion(stone.regionId().get());
            save();
        }
        return stone;
    }

    /** The granary region linked, if any. */
    public Optional<String> region(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        String region = kingdom.get().getGranaryRegion();
        return region == null || region.isBlank() ? Optional.empty() : Optional.of(region);
    }

    /** Releases the granary region; the hay standing in it stays where it is. */
    public Verdict release(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Verdict.NO_GRANARY;
        }
        Verdict verdict = GranarySiting.evaluateClear(actorRank, false, kingdom.get().getGranaryRegion());
        if (verdict == Verdict.ALLOWED) {
            kingdom.get().clearGranaryRegion();
            save();
        }
        return verdict;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }
}
