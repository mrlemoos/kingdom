package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The pure decision behind field morale decay: whether one soldier kept in the field loses heart
 * on this realm day. A season that asks nothing of the men (no decay days) never bites; a closed
 * morale track has nothing to lose; and a routed man is already as low as the ladder goes.
 *
 * <p>The clock starts lazily, as the morale recovery clock does: the first day a soldier is seen in
 * the field only marks him, and the step falls once a full season's wait has passed since.
 */
public final class FieldMoraleDecayPolicy {

    private FieldMoraleDecayPolicy() {}

    public static boolean shouldDecay(
            int decayDays, Optional<MoraleTier> tier, OptionalLong lastDecayDay, long realmDay) {
        if (decayDays <= 0 || tier.isEmpty() || !lastDecayDay.isPresent()) {
            return false;
        }
        if (tier.get() == MoraleTier.ROUT) {
            return false;
        }
        return realmDay - lastDecayDay.getAsLong() >= decayDays;
    }
}
