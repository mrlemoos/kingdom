package dev.mrlemoos.kingdom.treaty;

import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import java.util.Set;

public record TreatyState(
        String firstKingdomId, String secondKingdomId, TreatyKind kind, long expiresOnMcDay, Set<String> assentedBy,
        boolean active, long repealExpiresOnMcDay, Set<String> repealAssentedBy) {

    public TreatyState {
        assentedBy = Set.copyOf(assentedBy);
        repealAssentedBy = Set.copyOf(repealAssentedBy);
    }

    public TreatyState(
            String firstKingdomId, String secondKingdomId, TreatyKind kind, long expiresOnMcDay, Set<String> assentedBy,
            boolean active) {
        this(firstKingdomId, secondKingdomId, kind, expiresOnMcDay, assentedBy, active, Long.MAX_VALUE, Set.of());
    }
}
