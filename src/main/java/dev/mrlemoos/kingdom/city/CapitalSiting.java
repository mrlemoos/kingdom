package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.capital.CapitalService;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser;
import dev.mrlemoos.kingdom.worldguard.SubregionChooser.Candidate;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import java.util.Optional;

/**
 * Raising and clearing the capital with its Lord Mayor and Town Crier, and moving the Crier's stand,
 * whichever road led there — the foundation stone or the operators' command. The rules stay in
 * {@link CityService}; this stands the NPCs up, links the war region and saves.
 */
public final class CapitalSiting {

    /** What came of raising a site: the city's verdict, and whether its NPCs could be stood up. */
    public record Raised(CityResult result, boolean mayorStanding, boolean crierStanding) {}

    private final KingdomService kingdomService;
    private final CityService cityService;
    private final LordMayorService lordMayorService;
    private final TownCrierService townCrierService;
    private final CapitalService capitalService;
    private final YamlKingdomStore store;

    /** @param capitalService the capital-fall regions; null when war is not wired */
    public CapitalSiting(
            KingdomService kingdomService,
            CityService cityService,
            LordMayorService lordMayorService,
            TownCrierService townCrierService,
            CapitalService capitalService,
            YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.cityService = cityService;
        this.lordMayorService = lordMayorService;
        this.townCrierService = townCrierService;
        this.capitalService = capitalService;
        this.store = store;
    }

    /** Sites the capital, stands the Lord Mayor there and the Town Crier beside him. */
    public Raised site(String kingdomId, NobleRank actorRank, CapitalLocation capital) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return new Raised(CityResult.fail("Unknown kingdom."), false, false);
        }
        CityResult result = cityService.setCapital(kingdomId, actorRank, capital);
        if (result instanceof CityResult.Failure) {
            return new Raised(result, false, false);
        }
        boolean mayor = lordMayorService.spawn(kingdom.get(), capital).isPresent();
        // The Crier has no implicit home: siting a capital stands one there until it is dismissed.
        cityService.setTownCrierStand(kingdomId, actorRank, capital);
        boolean crier = townCrierService.spawnAtStand(kingdom.get()).isPresent();
        save();
        return new Raised(result, mayor, crier);
    }

    /**
     * Links the smallest WorldGuard subregion around the block as the capital war region; with none
     * there, the war region is left unset. The war region follows the capital.
     *
     * @return the region linked, if any
     */
    public Optional<String> linkWarRegion(String kingdomId, String worldName, int x, int y, int z) {
        if (capitalService == null) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        Optional<String> chosen = Optional.empty();
        if (kingdom.isPresent() && WorldGuardBridge.isAvailable()) {
            List<Candidate> around = SubregionChooser.candidates(worldName, WorldGuardBridge.regionsAt(worldName, x, y, z));
            List<Candidate> territory = SubregionChooser.candidates(worldName, kingdom.get().getWorldGuardRegions());
            chosen = SubregionChooser.smallestAround(x, y, z, around, territory);
        }
        if (chosen.isPresent()) {
            capitalService.setCapital(kingdomId, chosen.get(), worldName);
        } else {
            capitalService.clearCapital(kingdomId);
        }
        save();
        return chosen;
    }

    /** Dissolves the capital; the Lord Mayor and the Town Crier go with it. */
    public CityResult clear(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        lordMayorService.despawn(kingdom.get());
        townCrierService.despawn(kingdom.get());
        CityResult result = cityService.clearCapital(kingdomId, actorRank);
        if (result instanceof CityResult.Success) {
            save();
        }
        return result;
    }

    /** Releases the capital war region along with the capital. */
    public void releaseWarRegion(String kingdomId) {
        if (capitalService != null && capitalService.hasCapital(kingdomId)) {
            capitalService.clearCapital(kingdomId);
            save();
        }
    }

    /** Moves the Town Crier's stand and stands the Crier there. */
    public Raised standCrier(String kingdomId, NobleRank actorRank, CapitalLocation stand) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return new Raised(CityResult.fail("Unknown kingdom."), false, false);
        }
        CityResult result = cityService.setTownCrierStand(kingdomId, actorRank, stand);
        if (result instanceof CityResult.Failure) {
            return new Raised(result, false, false);
        }
        boolean crier = townCrierService.spawnAtStand(kingdom.get()).isPresent();
        save();
        return new Raised(result, true, crier);
    }

    /** Sends the Town Crier back to cry at the city hall. */
    public Raised returnCrier(String kingdomId, NobleRank actorRank) {
        Optional<CapitalLocation> capital = cityService.capital(kingdomId);
        if (capital.isEmpty()) {
            return new Raised(CityResult.fail("This kingdom has no capital."), false, false);
        }
        Raised raised = standCrier(kingdomId, actorRank, capital.get());
        if (raised.result() instanceof CityResult.Failure) {
            return raised;
        }
        return new Raised(CityResult.ok("The Town Crier has returned to the city hall."), true, raised.crierStanding());
    }

    /** Dismisses the Town Crier altogether. */
    public CityResult dismissCrier(String kingdomId, NobleRank actorRank) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        // Despawn first: dismissal needs the stand to find a crier whose chunk was unloaded.
        townCrierService.despawn(kingdom.get());
        CityResult result = cityService.clearTownCrierStand(kingdomId, actorRank);
        if (result instanceof CityResult.Success) {
            save();
        }
        return result;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }
}
