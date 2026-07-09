package dev.mrlemoos.kingdom.war.crownsquad;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Crown squad feature flag, per-purchase treasury cost, and per-kingdom cap — independent of the
 * war master flag, mirroring {@code war.oath.enabled} and {@code war.roster.cap}. Defaults off:
 * unlike the oath ceremony, a crown squad purchase spends real treasury Corona, so it must be an
 * explicit opt-in.
 *
 * <p>The cap here is crown-squad-only. Coordinating it into a single shared {@code
 * war.army.cap} alongside {@code ConscriptionService}'s pressed villagers (Slice 5.1) is deferred
 * to Slice 5.3's squad assignment work, which already calls for "a strict per-kingdom cap" across
 * rank-and-file. Until then, a kingdom's pressed-villager count and crown-squad count are tracked
 * — and capped — independently.
 */
public record CrownSquadConfig(boolean enabled, double cost, int cap) {

    public static final double DEFAULT_COST = 50.0;
    public static final int DEFAULT_CAP = 4;

    public CrownSquadConfig {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative");
        }
        if (cap < 0) {
            throw new IllegalArgumentException("cap must not be negative");
        }
    }

    public static CrownSquadConfig defaults() {
        return new CrownSquadConfig(false, DEFAULT_COST, DEFAULT_CAP);
    }

    public static CrownSquadConfig fromPluginConfig(FileConfiguration config) {
        return new CrownSquadConfig(
                config.getBoolean("war.crown-squads.enabled", false),
                config.getDouble("war.crown-squads.cost", DEFAULT_COST),
                config.getInt("war.crown-squads.cap", DEFAULT_CAP));
    }
}
