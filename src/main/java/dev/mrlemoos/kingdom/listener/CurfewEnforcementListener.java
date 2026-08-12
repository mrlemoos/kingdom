package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.city.CurfewBreachThrottle;
import dev.mrlemoos.kingdom.city.DecreeCurfewResolver;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import dev.mrlemoos.kingdom.police.CurfewEvaluator;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.MovementFacts;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Files a warrant application when a player is abroad in linked territory outside the kingdom's
 * curfew window. King/Queen/Prince immunity comes from {@link MechanicalJusticeService}; operators
 * are not exempt.
 */
public final class CurfewEnforcementListener implements Listener {

    public static final String DECREE_CURFEW_BILL_ID = "decree-curfew";

    private final KingdomService kingdomService;
    private final KingdomTerritoryResolver territoryResolver;
    private final MechanicalJusticeService justiceService;
    private final FileConfiguration pluginConfig;
    private final CurfewBreachThrottle throttle = new CurfewBreachThrottle();

    public CurfewEnforcementListener(
            JavaPlugin plugin,
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            MechanicalJusticeService justiceService) {
        Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.pluginConfig = plugin.getConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || to.getWorld() == null) {
            return;
        }
        // Only re-check when the player enters a new block — cuts noise without missing breaches.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Optional<String> jurisdiction = territoryResolver.owningKingdomId(
                to.getWorld().getName(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        if (jurisdiction.isEmpty()) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(jurisdiction.get());
        if (kingdom.isEmpty()) {
            return;
        }

        CurfewEnforcementConfig window = DecreeCurfewResolver.resolve(
                kingdom.get().getCityState().decreeCurfew(), pluginConfig);
        if (!window.enabled()) {
            return;
        }

        CurfewEvaluator evaluator = new CurfewEvaluator(window);
        long worldTime = to.getWorld().getTime();
        Optional<ActBreach> breach = evaluator.evaluate(
                MovementFacts.inJurisdiction(jurisdiction.get(), worldTime),
                List.of(syntheticCurfewAct(jurisdiction.get())));
        if (breach.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (!throttle.shouldOpen(jurisdiction.get(), playerId, now)) {
            return;
        }

        PoliceResult result = justiceService.openFromActBreach(breach.get(), playerId);
        if (result instanceof PoliceResult.Success) {
            player.sendMessage(c("&cYou are abroad after curfew. A warrant application has been filed."));
        }
    }

    static AssentedAct syntheticCurfewAct(String kingdomId) {
        return new AssentedAct(
                DECREE_CURFEW_BILL_ID,
                "Decree Curfew",
                BillType.BUDGET,
                0L,
                List.of("Decree curfew"),
                Map.of(),
                null,
                "world",
                0,
                64,
                0,
                0,
                List.of(new ConductProvision(ConductKind.CURFEW)));
    }
}
