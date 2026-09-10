package dev.mrlemoos.kingdom.war.capture;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipant;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantReason;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantRegistry;
import dev.mrlemoos.kingdom.war.siege.SiegeZoneResolver;
import dev.mrlemoos.kingdom.war.siege.TerritoryPort;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One sampler pass: military participants currently standing in siege-zone chunks credit those
 * chunks toward capture/recapture. Domain-only; Bukkit supplies the position map.
 */
public final class ChunkCapturePresenceTick {

    private final SiegeZoneResolver zones;
    private final TerritoryPort territory;
    private final MilitaryParticipantRegistry participants;
    private final ChunkCaptureService capture;

    public ChunkCapturePresenceTick(
            SiegeZoneResolver zones,
            TerritoryPort territory,
            MilitaryParticipantRegistry participants,
            ChunkCaptureService capture) {
        this.zones = Objects.requireNonNull(zones, "zones");
        this.territory = Objects.requireNonNull(territory, "territory");
        this.participants = Objects.requireNonNull(participants, "participants");
        this.capture = Objects.requireNonNull(capture, "capture");
    }

    public ChunkCaptureService capture() {
        return capture;
    }

    /** Credits each occupied siege-zone chunk once for this sample. */
    public void sample(ActiveWar war, Map<UUID, ChunkCoord> positions) {
        Objects.requireNonNull(war, "war");
        Objects.requireNonNull(positions, "positions");
        Set<ChunkCoord> occupied = new LinkedHashSet<>();
        for (ChunkCoord chunk : positions.values()) {
            if (zones.isInSiegeZone(war, chunk, territory)) {
                occupied.add(chunk);
            }
        }
        for (ChunkCoord chunk : occupied) {
            int attackers = presenceCredit(war, war.attackerKingdomId(), positions, chunk);
            int defenders = presenceCredit(war, war.defenderKingdomId(), positions, chunk);
            capture.tick(war.id(), chunk, war.attackerKingdomId(), war.defenderKingdomId(), attackers, defenders);
        }
    }

    /**
     * Defender civilians keep political rights in occupied land but do not credit recapture.
     * Roster, muster, and oath participants still count so the chunk can be taken back.
     */
    private int presenceCredit(
            ActiveWar war, String kingdomId, Map<UUID, ChunkCoord> positions, ChunkCoord chunk) {
        Optional<String> controller = capture.controller(war.id(), chunk);
        int credit = 0;
        for (Map.Entry<UUID, ChunkCoord> entry : positions.entrySet()) {
            if (!chunk.equals(entry.getValue())) {
                continue;
            }
            Optional<MilitaryParticipant> found = participants.findParticipant(war.id(), entry.getKey());
            if (found.isEmpty() || !kingdomId.equals(found.get().kingdomId())) {
                continue;
            }
            if (controller.isPresent()
                    && controller.get().equals(war.attackerKingdomId())
                    && kingdomId.equals(war.defenderKingdomId())
                    && found.get().reason() == MilitaryParticipantReason.CIVILIAN_HOSTILE_BIND) {
                continue;
            }
            credit++;
        }
        return credit;
    }
}
