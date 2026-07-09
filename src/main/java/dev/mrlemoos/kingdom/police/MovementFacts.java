package dev.mrlemoos.kingdom.police;

/**
 * Domain facts for movement inside a kingdom's jurisdiction at a world time.
 */
public record MovementFacts(String jurisdictionKingdomId, long worldTimeTick) {

    public MovementFacts {
        if (jurisdictionKingdomId == null || jurisdictionKingdomId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionKingdomId must not be blank");
        }
    }

    public static MovementFacts inJurisdiction(String jurisdictionKingdomId, long worldTimeTick) {
        return new MovementFacts(jurisdictionKingdomId, worldTimeTick);
    }
}
