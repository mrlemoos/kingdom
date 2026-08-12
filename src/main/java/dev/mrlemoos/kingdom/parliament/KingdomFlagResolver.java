package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import java.util.Optional;

/** Chooses which design flies when Lords is set. */
public final class KingdomFlagResolver {

    private KingdomFlagResolver() {
    }

    /**
     * Held banner wins; else keep the existing flag; else Crown orange for a first raise.
     */
    public static KingdomFlag resolve(Optional<KingdomFlag> existing, Optional<KingdomFlag> held) {
        if (held != null && held.isPresent()) {
            return held.get();
        }
        if (existing != null && existing.isPresent()) {
            return existing.get();
        }
        return KingdomFlag.crownDefault();
    }
}
