package dev.mrlemoos.kingdom.police;

/**
 * A witnessed assault on that kingdom's King, Queen, Prince, or Princess.
 */
public record CrownAssault(String jurisdictionKingdomId) {

    public static final String BILL_ID = "treason";

    public CrownAssault {
        if (jurisdictionKingdomId == null || jurisdictionKingdomId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionKingdomId must not be blank");
        }
    }
}
