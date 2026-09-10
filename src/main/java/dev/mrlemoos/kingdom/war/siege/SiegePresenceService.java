package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Samples military participants currently inside defender territory. */
public final class SiegePresenceService {

    private final SiegeZoneResolver zones;
    private final TerritoryPort territory;
    private final MilitaryParticipantRegistry participants;
    private final MoraleService morale;
    private final Map<String, SiegePresence> presenceByWar = new LinkedHashMap<>();

    public SiegePresenceService(
            SiegeZoneResolver zones, TerritoryPort territory, MilitaryParticipantRegistry participants, MoraleService morale) {
        this.zones = Objects.requireNonNull(zones, "zones");
        this.territory = Objects.requireNonNull(territory, "territory");
        this.participants = Objects.requireNonNull(participants, "participants");
        this.morale = Objects.requireNonNull(morale, "morale");
    }

    /** Replaces one war's live count. Only registered military participants count. */
    public SiegePresence sample(ActiveWar war, Map<UUID, ChunkCoord> positions) {
        Objects.requireNonNull(war, "war");
        Objects.requireNonNull(positions, "positions");
        int attackers = 0;
        int defenders = 0;
        for (Map.Entry<UUID, ChunkCoord> entry : positions.entrySet()) {
            Optional<MilitaryParticipant> participant = participants.findParticipant(war.id(), entry.getKey());
            if (participant.isEmpty() || !zones.isInSiegeZone(war, entry.getValue(), territory)) {
                continue;
            }
            if (war.attackerKingdomId().equals(participant.get().kingdomId())) {
                attackers++;
            } else if (war.defenderKingdomId().equals(participant.get().kingdomId())) {
                defenders++;
            }
        }
        SiegePresence presence = new SiegePresence(attackers, defenders);
        presenceByWar.put(war.id(), presence);
        return presence;
    }

    /** First civilian hostile act inside siege binds their military track; damage remains vanilla. */
    public boolean bindCivilianHostileAction(ActiveWar war, String kingdomId, UUID playerId, ChunkCoord chunk) {
        Objects.requireNonNull(war, "war");
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(chunk, "chunk");
        if (!war.involves(kingdomId) || participants.isParticipant(war.id(), playerId)
                || !zones.isInSiegeZone(war, chunk, territory)) {
            return false;
        }
        morale.recordSiegeHostileAction(playerId, true);
        return participants.bindCivilian(war.id(), kingdomId, playerId);
    }

    public SiegePresence presenceFor(String warId) {
        return presenceByWar.getOrDefault(warId, SiegePresence.EMPTY);
    }

    public SiegeConfig config() {
        return zones.config();
    }

    public void clearForWar(String warId) {
        if (warId != null) {
            presenceByWar.remove(warId);
        }
    }
}
