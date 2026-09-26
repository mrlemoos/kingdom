package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.Warrant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * The one road for the business done on an active warrant — a constable's arrest, an arrest reward
 * posted at the court, the Crown's cancellation — shared by the iron sword, the court's windows, the
 * Realm Hub's warrant register and the operators' {@code /kingdom police} commands. Each act is the
 * existing police service's; this only runs it, persists on success and seats the trial after an arrest.
 */
public final class WarrantDesk {

    private final PoliceService policeService;
    private final MechanicalJusticeService justiceService;
    private final PoliceTrialService trialService;
    private final TrialJuryRuntime trialJuryRuntime;
    private final TerritoryResolver territoryResolver;
    private final Runnable persist;

    public WarrantDesk(
            PoliceService policeService,
            MechanicalJusticeService justiceService,
            PoliceTrialService trialService,
            TrialJuryRuntime trialJuryRuntime,
            TerritoryResolver territoryResolver,
            Runnable persist) {
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.trialJuryRuntime = Objects.requireNonNull(trialJuryRuntime, "trialJuryRuntime");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.persist = Objects.requireNonNull(persist, "persist");
    }

    public boolean isConstable(String kingdomId, UUID playerId) {
        return policeService.isConstable(kingdomId, playerId);
    }

    public boolean hasActiveWarrant(String kingdomId, UUID suspectId) {
        return justiceService.hasActiveWarrant(kingdomId, suspectId);
    }

    /** This realm's active warrants, oldest first. */
    public List<Warrant> activeWarrants(String kingdomId) {
        return WarrantRegister.active(justiceService.warrantsView(), kingdomId);
    }

    public Optional<Warrant> activeWarrant(String kingdomId, String warrantId) {
        Optional<Warrant> found = justiceService.findById(kingdomId, warrantId);
        if (found.isEmpty() || !activeWarrants(kingdomId).contains(found.get())) {
            return Optional.empty();
        }
        return found;
    }

    /** A constable's arrest: the pending trial opens, the reward is paid, and the trial is seated. */
    public PoliceResult arrest(String kingdomId, UUID constableId, UUID suspectId) {
        PoliceResult arrested = trialService.arrest(kingdomId, constableId, suspectId);
        if (arrested instanceof PoliceResult.Success) {
            persist.run();
            trialJuryRuntime.resolveAfterArrest(kingdomId, suspectId);
        }
        return arrested;
    }

    /** Posts or tops up the arrest reward on a suspect's active warrant, from the poster's wallet. */
    public PoliceResult postReward(String kingdomId, UUID posterId, UUID suspectId, double amount) {
        PoliceResult result = trialService.arrestRewardService().postOrTopUp(kingdomId, posterId, suspectId, amount);
        if (result instanceof PoliceResult.Success) {
            persist.run();
        }
        return result;
    }

    /** The Crown withdraws an active warrant; any reward goes back to its poster. */
    public PoliceResult cancel(String kingdomId, UUID crownId, String warrantId) {
        return persisted(trialService.arrestRewardService().cancelAndRefund(kingdomId, crownId, warrantId));
    }

    public PoliceResult cancelForSuspect(String kingdomId, UUID crownId, UUID suspectId) {
        return persisted(trialService.arrestRewardService().cancelActiveForSuspect(kingdomId, crownId, suspectId));
    }

    private PoliceResult persisted(PoliceResult result) {
        if (result instanceof PoliceResult.Success) {
            persist.run();
        }
        return result;
    }

    /** True when the location stands inside the realm's own linked territory. */
    public boolean inJurisdiction(Location location, String kingdomId) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
        return territory.type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
    }

    /** True within ballot range of the realm's court. */
    public boolean nearCourt(Location location, String kingdomId) {
        Optional<CourtLocation> court = policeService.court(kingdomId);
        if (court.isEmpty() || location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null || !world.getName().equals(court.get().worldName())) {
            return false;
        }
        return CourtProximity.isWithinBallotRange(
                location.getX() - (court.get().x() + 0.5),
                location.getY() - court.get().y(),
                location.getZ() - (court.get().z() + 0.5));
    }
}
