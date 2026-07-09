package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

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

    public BuildConductListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            BuildConductEnforcer enforcer,
            MechanicalJusticeService justiceService,
            LoyaltyService loyaltyService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.enforcer = Objects.requireNonNull(enforcer, "enforcer");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
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
        if (!enforcer.config().enabled()) {
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

        List<AssentedAct> acts = kingdom.get().getParliamentState().assentedActsView();
        BlockActionFacts facts = new BlockActionFacts(jurisdiction.get(), actionType);
        UUID actorId = player.getUniqueId();
        BuildEnforcementDecision decision =
                enforcer.evaluate(facts, acts, actorId, player.isOp());

        if (!decision.denied()) {
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
}
