package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.BuildRefusalThrottle;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.loyalty.LoyaltyResult;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.police.BlockActionFacts;
import dev.mrlemoos.kingdom.police.BuildConductEnforcer;
import dev.mrlemoos.kingdom.police.BuildEnforcementDecision;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.occupation.OccupationBuildGate;
import dev.mrlemoos.kingdom.war.occupation.OccupationBuildOutcome;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Cancels block place/break when an enacted build-ban Act covers the jurisdiction.
 * Debounced breaches open the mechanical warrant pipeline and drop political loyalty.
 */
public final class BuildConductListener implements Listener {

    private final KingdomService kingdomService;
    private final KingdomTerritoryResolver territoryResolver;
    private final BuildConductEnforcer enforcer;
    private final MechanicalJusticeService justiceService;
    private final LoyaltyService loyaltyService;
    private final CityService cityService;
    private final WarService warService;
    private final OccupationBuildGate occupationGate;
    private final BuildRefusalThrottle refusalThrottle = new BuildRefusalThrottle();

    public BuildConductListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            BuildConductEnforcer enforcer,
            MechanicalJusticeService justiceService,
            LoyaltyService loyaltyService) {
        this(kingdomService, territoryResolver, enforcer, justiceService, loyaltyService, null, null, null);
    }

    public BuildConductListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            BuildConductEnforcer enforcer,
            MechanicalJusticeService justiceService,
            LoyaltyService loyaltyService,
            CityService cityService) {
        this(kingdomService, territoryResolver, enforcer, justiceService, loyaltyService, cityService, null, null);
    }

    public BuildConductListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            BuildConductEnforcer enforcer,
            MechanicalJusticeService justiceService,
            LoyaltyService loyaltyService,
            CityService cityService,
            WarService warService,
            OccupationBuildGate occupationGate) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.enforcer = Objects.requireNonNull(enforcer, "enforcer");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
        this.cityService = cityService;
        this.warService = warService;
        this.occupationGate = occupationGate;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handle(event.getPlayer(), event.getBlock(), BlockActionFacts.BlockActionType.BREAK, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handle(event.getPlayer(), event.getBlock(), BlockActionFacts.BlockActionType.PLACE, event);
    }

    private void handle(
            Player player,
            Block block,
            BlockActionFacts.BlockActionType actionType,
            org.bukkit.event.Cancellable event) {
        if (player == null || block == null || block.getWorld() == null) {
            return;
        }

        Optional<String> jurisdiction = territoryResolver.owningKingdomId(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (jurisdiction.isEmpty()) {
            return;
        }

        Optional<Kingdom> kingdom = kingdomService.getKingdom(jurisdiction.get());
        if (kingdom.isEmpty()) {
            return;
        }
        UUID actorId = player.getUniqueId();
        if (applyOccupation(player, block, kingdom.get(), actorId, event)) {
            return;
        }

        if (!enforcer.config().enabled()) {
            refuseWithoutPermit(player, kingdom.get(), actorId, event);
            return;
        }

        List<AssentedAct> acts = kingdom.get().getParliamentState().assentedActsView();
        BlockActionFacts facts = new BlockActionFacts(jurisdiction.get(), actionType);
        BuildEnforcementDecision decision =
                enforcer.evaluate(facts, acts, actorId, player.isOp());

        if (!decision.denied()) {
            refuseWithoutPermit(player, kingdom.get(), actorId, event);
            return;
        }

        event.setCancelled(true);
        player.sendMessage(c("&cBuilding is forbidden here by an Act of Parliament."));

        if (decision.breach().isEmpty()) {
            return;
        }
        ActBreach breach = decision.breach().get();
        LoyaltyResult loyalty = loyaltyService.recordActBreach(actorId);
        if (loyalty instanceof LoyaltyResult.Success success) {
            player.sendMessage(c("&e" + success.message()));
        }
        PoliceResult warrant = justiceService.openFromActBreach(breach, actorId);
        if (warrant instanceof PoliceResult.Success) {
            player.sendMessage(c("&7A warrant application has been filed with the Crown."));
        }
    }

    /**
     * Occupation overlay on captured chunks. Occupiers may build without a permit or Act path.
     * Defender members are refused with no warrant — political rights stay, the block does not.
     */
    private boolean applyOccupation(
            Player player, Block block, Kingdom jurisdiction, UUID actorId, org.bukkit.event.Cancellable event) {
        if (warService == null || occupationGate == null) {
            return false;
        }
        Optional<dev.mrlemoos.kingdom.model.war.ActiveWar> war = warService.activeWarFor(jurisdiction.getId());
        if (war.isEmpty() || !war.get().defenderKingdomId().equals(jurisdiction.getId())) {
            return false;
        }
        String actorKingdomId = "";
        Optional<dev.mrlemoos.kingdom.model.PlayerMembership> membership = kingdomService.getMembership(actorId);
        if (membership.isPresent()) {
            actorKingdomId = membership.get().getKingdomId();
        }
        ChunkCoord chunk = new ChunkCoord(
                block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        OccupationBuildOutcome outcome = occupationGate.decide(war.get(), chunk, actorKingdomId);
        if (outcome == OccupationBuildOutcome.ALLOW_OCCUPIER) {
            return true;
        }
        if (outcome == OccupationBuildOutcome.DENY) {
            event.setCancelled(true);
            if (refusalThrottle.shouldSend(actorId, System.currentTimeMillis())) {
                player.sendMessage(c("&cThis land is under occupation. You may not build here."));
            }
            return true;
        }
        return false;
    }

    /**
     * The build-permit gate. Unlike the Act ban this is not a crime: the block event is cancelled
     * and the player told why, with no warrant and no loyalty drop. Operators are not exempt.
     */
    private void refuseWithoutPermit(
            Player player, Kingdom kingdom, UUID actorId, org.bukkit.event.Cancellable event) {
        if (cityService == null) {
            return;
        }
        if (cityService.mayBuild(kingdom.getId(), actorId)) {
            return;
        }
        event.setCancelled(true);
        if (refusalThrottle.shouldSend(actorId, System.currentTimeMillis())) {
            player.sendMessage(c("&cYou need a build permit from the Lord Mayor of "
                    + kingdom.getDisplayName() + "."));
        }
    }
}
