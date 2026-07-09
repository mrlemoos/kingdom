package dev.mrlemoos.kingdom.model.war;

/**
 * The bilateral hostilities between exactly two kingdoms from enactment of a war bill until peace bill
 * enactment or decisive victory. Domain-only record — no Bukkit world or entity identifiers.
 */
public record ActiveWar(
        String id,
        String attackerKingdomId,
        String defenderKingdomId,
        WarAim aim,
        WarOutcome outcome,
        long startedAtMs,
        long musterDeadlineAtMs) {

    public ActiveWar {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (attackerKingdomId == null || attackerKingdomId.isBlank()) {
            throw new IllegalArgumentException("attackerKingdomId must not be blank");
        }
        if (defenderKingdomId == null || defenderKingdomId.isBlank()) {
            throw new IllegalArgumentException("defenderKingdomId must not be blank");
        }
        if (aim == null) {
            throw new IllegalArgumentException("aim must not be null");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("outcome must not be null");
        }
    }

    public boolean involves(String kingdomId) {
        return attackerKingdomId.equals(kingdomId) || defenderKingdomId.equals(kingdomId);
    }
}
