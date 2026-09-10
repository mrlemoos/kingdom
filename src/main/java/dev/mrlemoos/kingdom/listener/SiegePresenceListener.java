package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.capture.ChunkCapturePresenceTick;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.muster.MusterAnswer;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantReason;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantRegistry;
import dev.mrlemoos.kingdom.war.siege.SiegePresenceService;
import dev.mrlemoos.kingdom.war.victory.VictoryResult;
import dev.mrlemoos.kingdom.war.victory.VictoryTick;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Main-thread siege sampler. It observes combat; it never changes vanilla PvP damage. */
public final class SiegePresenceListener implements Listener, Runnable {

    private final KingdomService kingdoms;
    private final WarService wars;
    private final StandingRosterService roster;
    private final MusterService musters;
    private final MilitaryParticipantRegistry participants;
    private final SiegePresenceService presence;
    private final ChunkCapturePresenceTick captureTick;
    private VictoryTick victoryTick;
    private Consumer<VictoryResult.Victory> onDecisiveVictory;

    public SiegePresenceListener(
            KingdomService kingdoms,
            WarService wars,
            StandingRosterService roster,
            MusterService musters,
            MilitaryParticipantRegistry participants,
            SiegePresenceService presence,
            ChunkCapturePresenceTick captureTick) {
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
        this.wars = Objects.requireNonNull(wars, "wars");
        this.roster = Objects.requireNonNull(roster, "roster");
        this.musters = Objects.requireNonNull(musters, "musters");
        this.participants = Objects.requireNonNull(participants, "participants");
        this.presence = Objects.requireNonNull(presence, "presence");
        this.captureTick = Objects.requireNonNull(captureTick, "captureTick");
    }

    public void setVictoryTick(VictoryTick victoryTick) {
        this.victoryTick = victoryTick;
    }

    public void setOnDecisiveVictory(Consumer<VictoryResult.Victory> onDecisiveVictory) {
        this.onDecisiveVictory = onDecisiveVictory;
    }

    @Override
    public void run() {
        if (!wars.config().enabled() || !presenceEnabled()) {
            return;
        }
        Map<UUID, ChunkCoord> positions = new LinkedHashMap<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == null) {
                continue;
            }
            positions.put(player.getUniqueId(), new ChunkCoord(
                    player.getWorld().getName(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()));
        }
        for (ActiveWar war : wars.activeWarsView()) {
            for (UUID playerId : positions.keySet()) {
                reconcileParticipant(war, playerId);
            }
            presence.sample(war, positions);
            captureTick.sample(war, positions);
            evaluateVictory(war);
        }
    }

    private void evaluateVictory(ActiveWar war) {
        if (victoryTick == null) {
            return;
        }
        VictoryResult result = victoryTick.evaluate(war);
        if (!(result instanceof VictoryResult.Victory victory)) {
            return;
        }
        String attacker = kingdomDisplay(victory.victorKingdomId());
        String defender = kingdomDisplay(victory.defeatedKingdomId());
        org.bukkit.Bukkit.broadcastMessage(dev.mrlemoos.kingdom.helpers.ColourEncoder.c(
                "&4[War] " + attacker + " has won a decisive victory over " + defender + "."));
        if (onDecisiveVictory != null) {
            onDecisiveVictory.accept(victory);
        }
    }

    private String kingdomDisplay(String kingdomId) {
        Optional<dev.mrlemoos.kingdom.model.Kingdom> kingdom = kingdoms.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return kingdomId;
        }
        return kingdom.get().getDisplayName();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!wars.config().enabled() || !presenceEnabled() || event.getEntity().getWorld() == null) {
            return;
        }
        Optional<Player> attacker = CrownAssaultListener.attributedPlayer(event.getDamager());
        if (attacker.isEmpty()) {
            return;
        }
        Optional<PlayerMembership> membership = kingdoms.getMembership(attacker.get().getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        wars.activeWarFor(membership.get().getKingdomId()).ifPresent(war -> presence.bindCivilianHostileAction(
                war,
                membership.get().getKingdomId(),
                attacker.get().getUniqueId(),
                new ChunkCoord(
                        event.getEntity().getWorld().getName(),
                        event.getEntity().getLocation().getChunk().getX(),
                        event.getEntity().getLocation().getChunk().getZ())));
    }

    private void reconcileParticipant(ActiveWar war, UUID playerId) {
        Optional<PlayerMembership> membership = kingdoms.getMembership(playerId);
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!war.involves(kingdomId) || participants.isParticipant(war.id(), playerId)) {
            return;
        }
        if (roster.isOnDuty(playerId)) {
            participants.markParticipant(war.id(), kingdomId, playerId, MilitaryParticipantReason.STANDING_ROSTER);
        } else if (musters.answerOf(war.id(), playerId).orElse(null) == MusterAnswer.ANSWERED) {
            participants.markParticipant(war.id(), kingdomId, playerId, MilitaryParticipantReason.MUSTER_ANSWERED);
        }
    }

    private boolean presenceEnabled() {
        return presence.config().enabled();
    }
}
