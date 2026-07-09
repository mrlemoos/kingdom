package dev.mrlemoos.kingdom.war.squad;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Squad assignment feature flag and caps, independent of the war master flag — mirrors {@code
 * war.crown-squads.enabled}. Defaults off: unlike muster or conscription, squad assignment drives
 * rank-and-file AI behaviour and should be an explicit opt-in via {@code war.squads.enabled: true}.
 *
 * <p>Two caps are enforced here rather than one unified shared army cap across {@code
 * ConscriptionService}'s pressed villagers and {@code CrownSquadService}'s purchased units — a
 * reconciliation deferred from Slices 5.1/5.2. {@link #maxMembersPerSquad()} bounds a single
 * squad's rank-and-file, {@link #maxSquadsPerOfficer()} bounds how many squads one officer may
 * command at once, and {@link #maxSquadsPerKingdom()} bounds the kingdom's total squad count
 * across every officer. A future shared {@code war.army.cap} — counting pressed-plus-crown
 * rank-and-file assigned or available kingdom-wide — remains an open follow-up; until then a
 * kingdom's conscription cap, crown-squad cap, and these squad caps are tracked independently.
 */
public record SquadConfig(boolean enabled, int maxMembersPerSquad, int maxSquadsPerOfficer, int maxSquadsPerKingdom) {

    public static final int DEFAULT_MAX_MEMBERS_PER_SQUAD = 6;
    public static final int DEFAULT_MAX_SQUADS_PER_OFFICER = 1;
    public static final int DEFAULT_MAX_SQUADS_PER_KINGDOM = 8;

    public SquadConfig {
        if (maxMembersPerSquad < 0) {
            throw new IllegalArgumentException("maxMembersPerSquad must not be negative");
        }
        if (maxSquadsPerOfficer < 0) {
            throw new IllegalArgumentException("maxSquadsPerOfficer must not be negative");
        }
        if (maxSquadsPerKingdom < 0) {
            throw new IllegalArgumentException("maxSquadsPerKingdom must not be negative");
        }
    }

    public static SquadConfig on() {
        return new SquadConfig(
                true, DEFAULT_MAX_MEMBERS_PER_SQUAD, DEFAULT_MAX_SQUADS_PER_OFFICER, DEFAULT_MAX_SQUADS_PER_KINGDOM);
    }

    public static SquadConfig off() {
        return new SquadConfig(
                false, DEFAULT_MAX_MEMBERS_PER_SQUAD, DEFAULT_MAX_SQUADS_PER_OFFICER, DEFAULT_MAX_SQUADS_PER_KINGDOM);
    }

    public static SquadConfig fromPluginConfig(FileConfiguration config) {
        return new SquadConfig(
                config.getBoolean("war.squads.enabled", false),
                config.getInt("war.squads.max-members-per-squad", DEFAULT_MAX_MEMBERS_PER_SQUAD),
                config.getInt("war.squads.max-squads-per-officer", DEFAULT_MAX_SQUADS_PER_OFFICER),
                config.getInt("war.squads.max-squads-per-kingdom", DEFAULT_MAX_SQUADS_PER_KINGDOM));
    }
}
