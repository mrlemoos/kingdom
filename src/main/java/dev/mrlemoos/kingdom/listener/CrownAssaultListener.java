package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.police.CrownAssault;
import dev.mrlemoos.kingdom.police.CrownAssaultConfig;
import dev.mrlemoos.kingdom.police.CrownAssaultEvaluator;
import dev.mrlemoos.kingdom.police.CrownAssaultFacts;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.police.TrialJuryRuntime;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Witnessed player-attributed hits on this kingdom's Crown open a flagrant treason warrant
 * and an immediate patrol detain. Damage is never cancelled under open PvP.
 */
public final class CrownAssaultListener implements Listener {

    private final KingdomService kingdomService;
    private final KingdomTerritoryResolver territoryResolver;
    private final MechanicalJusticeService justiceService;
    private final PoliceService policeService;
    private final PoliceTrialService trialService;
    private final PoliceGolemService golemService;
    private final TrialJuryRuntime trialJuryRuntime;
    private final CrownAssaultEvaluator evaluator;
    private final Runnable persistHook;

    public CrownAssaultListener(
            KingdomService kingdomService,
            KingdomTerritoryResolver territoryResolver,
            MechanicalJusticeService justiceService,
            PoliceService policeService,
            PoliceTrialService trialService,
            PoliceGolemService golemService,
            TrialJuryRuntime trialJuryRuntime,
            CrownAssaultConfig config,
            Runnable persistHook) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.justiceService = Objects.requireNonNull(justiceService, "justiceService");
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.golemService = Objects.requireNonNull(golemService, "golemService");
        this.trialJuryRuntime = Objects.requireNonNull(trialJuryRuntime, "trialJuryRuntime");
        this.evaluator = new CrownAssaultEvaluator(Objects.requireNonNull(config, "config"));
        this.persistHook = persistHook;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Optional<Player> attackerOpt = attributedPlayer(event.getDamager());
        if (attackerOpt.isEmpty()) {
            return;
        }
        Player attacker = attackerOpt.get();
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        Location at = victim.getLocation();
        if (at.getWorld() == null) {
            return;
        }
        Optional<String> jurisdiction = territoryResolver.owningKingdomId(
                at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ());
        if (jurisdiction.isEmpty()) {
            return;
        }
        String kingdomId = jurisdiction.get();
        UUID attackerId = attacker.getUniqueId();

        Optional<CrownAssault> assault = evaluator.evaluate(new CrownAssaultFacts(
                kingdomId,
                isCrownOfKingdom(victim.getUniqueId(), kingdomId),
                justiceService.hasWarrantImmunity(kingdomId, attackerId),
                true,
                policeService.isPoliceReady(kingdomId),
                alreadyOpen(kingdomId, attackerId),
                true,
                golemService.nearestPatrolDistanceBlocks(kingdomId, at)));
        if (assault.isEmpty()) {
            return;
        }

        PoliceResult opened = justiceService.openFlagrantTreason(kingdomId, attackerId);
        if (!(opened instanceof PoliceResult.Success)) {
            return;
        }
        PoliceResult detained = trialJuryRuntime.detainByPatrolAndResolve(kingdomId, attackerId);
        if (detained instanceof PoliceResult.Success) {
            attacker.sendMessage(c(
                    "&cYou struck the Crown. A patrol has detained you for treason. A trial will follow."));
        } else {
            attacker.sendMessage(c("&cYou struck the Crown. A flagrant treason warrant is now active."));
        }
        if (persistHook != null) {
            persistHook.run();
        }
    }

    private boolean alreadyOpen(String kingdomId, UUID suspectId) {
        return justiceService.findPendingForSuspect(kingdomId, suspectId).isPresent()
                || justiceService.hasActiveWarrant(kingdomId, suspectId)
                || trialService.findOpenCase(kingdomId, suspectId).isPresent();
    }

    private boolean isCrownOfKingdom(UUID playerId, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty()) {
            return false;
        }
        PlayerMembership held = membership.get();
        if (!kingdomId.equals(held.getKingdomId()) || !held.hasNobleTitle()) {
            return false;
        }
        NobleRank rank = held.getRank();
        return rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE;
    }

    static Optional<Player> attributedPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return Optional.of(player);
            }
            return Optional.empty();
        }
        if (damager instanceof Tameable tameable) {
            if (tameable.getOwner() instanceof Player player) {
                return Optional.of(player);
            }
            return Optional.empty();
        }
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player player) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }
}
