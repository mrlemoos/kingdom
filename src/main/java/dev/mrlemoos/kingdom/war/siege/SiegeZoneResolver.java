package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Objects;

/**
 * Resolves whether a chunk is currently a <b>siege</b> zone for an {@link ActiveWar}: combat
 * inside the defender's linked territory with chunk capture active (see the Siege glossary entry
 * in {@code CONTEXT.md}). The attacker sieges the defender's linked territory only — this
 * resolver never considers the attacker's own territory a siege zone for that war. Gated by
 * {@code war.siege.enabled}; disabled, no chunk is ever a siege zone.
 */
public final class SiegeZoneResolver {

    private final SiegeConfig config;

    public SiegeZoneResolver(SiegeConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public SiegeConfig config() {
        return config;
    }

    /** True when {@code chunk} is inside {@code war}'s defender's linked territory and siege is enabled. */
    public boolean isInSiegeZone(ActiveWar war, ChunkCoord chunk, TerritoryPort territory) {
        Objects.requireNonNull(war, "war");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(territory, "territory");
        if (!config.enabled()) {
            return false;
        }
        return territory.isChunkInLinkedTerritory(war.defenderKingdomId(), chunk);
    }
}
