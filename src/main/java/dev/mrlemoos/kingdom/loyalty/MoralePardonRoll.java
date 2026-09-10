package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The roll called at the court before a morale pardon: which of the realm's own subjects there is
 * anything to pardon.
 *
 * <p>A subject appears only when their military morale track is open and has fallen below
 * Steadfast. A closed track is left closed — a pardon would open it at Steadfast, which is not a
 * pardon but a conscription — and a Steadfast subject has nothing to forgive. The worst stand at
 * the head of the roll, so the routed are seen first.
 *
 * <p>Plain domain logic with no platform dependency, so the roll is unit-testable.
 */
public final class MoralePardonRoll {

    private MoralePardonRoll() {}

    /** One subject on the roll: who they are and how far their morale has fallen. */
    public record Subject(UUID playerId, MoraleTier tier) {}

    /**
     * The roll for one realm, worst first and then in a settled order so the screen does not shuffle
     * between openings.
     *
     * @param members  every subject sworn to the realm
     * @param tiers    the military morale track as it stands, keyed by subject
     */
    public static List<Subject> of(Collection<UUID> members, Map<UUID, MoraleTier> tiers) {
        if (members == null || tiers == null) {
            return List.of();
        }
        List<Subject> roll = new ArrayList<>();
        for (UUID playerId : members) {
            MoraleTier tier = tiers.get(playerId);
            if (tier == null || tier == MoraleTier.STEADFAST) {
                continue;
            }
            roll.add(new Subject(playerId, tier));
        }
        roll.sort(Comparator.comparingInt((Subject subject) -> -subject.tier().ordinal())
                .thenComparing(subject -> subject.playerId().toString()));
        return List.copyOf(roll);
    }

    /** The tier as the realm names it. */
    public static String display(MoraleTier tier) {
        return switch (tier) {
            case STEADFAST -> "Steadfast";
            case SHAKEN -> "Shaken";
            case BREAKING -> "Breaking";
            case ROUT -> "Rout";
        };
    }
}
