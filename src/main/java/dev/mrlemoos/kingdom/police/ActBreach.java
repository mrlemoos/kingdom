package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.parliament.ConductKind;

/**
 * A detected violation of a conduct provision in an enacted Act within jurisdiction.
 */
public record ActBreach(
        String jurisdictionKingdomId, String actBillId, ConductKind provisionKind) {

    public ActBreach {
        if (jurisdictionKingdomId == null || jurisdictionKingdomId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionKingdomId must not be blank");
        }
        if (actBillId == null || actBillId.isBlank()) {
            throw new IllegalArgumentException("actBillId must not be blank");
        }
        if (provisionKind == null) {
            throw new IllegalArgumentException("provisionKind must not be null");
        }
    }
}
