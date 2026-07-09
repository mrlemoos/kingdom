package dev.mrlemoos.kingdom.police;

/**
 * Domain facts for a player damage event. No Bukkit types.
 * Used by the deferred PvP conduct evaluator stub.
 */
public record DamageFacts(DamageKind kind, String actorKingdomId, String victimKingdomId) {

    public enum DamageKind {
        PLAYER_VS_PLAYER,
        FRIENDLY_FIRE,
        SIEGE_NEUTRAL
    }

    public DamageFacts {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
    }

    public static DamageFacts playerVsPlayer(String actorKingdomId, String victimKingdomId) {
        return new DamageFacts(DamageKind.PLAYER_VS_PLAYER, actorKingdomId, victimKingdomId);
    }

    public static DamageFacts friendlyFire(String kingdomId) {
        return new DamageFacts(DamageKind.FRIENDLY_FIRE, kingdomId, kingdomId);
    }

    public static DamageFacts siegeNeutral(String jurisdictionKingdomId) {
        return new DamageFacts(DamageKind.SIEGE_NEUTRAL, null, jurisdictionKingdomId);
    }
}
