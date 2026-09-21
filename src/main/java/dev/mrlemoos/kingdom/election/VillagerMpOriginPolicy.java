package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import java.util.List;
import java.util.Optional;

/**
 * Where a villager MP belongs when the House rises. A villager must never take the chamber itself
 * for home: an origin recorded on the bench would have it "return" to Parliament for ever after.
 */
public final class VillagerMpOriginPolicy {

    /** A villager standing this near its bench is in the chamber, not at its profession. */
    private static final double CHAMBER_RADIUS = 4.0;

    private VillagerMpOriginPolicy() {}

    public static boolean isAtChamber(MpSeatLocation candidate, MpSeatLocation chamber) {
        if (!candidate.worldName().equals(chamber.worldName())) {
            return false;
        }
        double dx = candidate.x() - chamber.x();
        double dy = candidate.y() - chamber.y();
        double dz = candidate.z() - chamber.z();
        return dx * dx + dy * dy + dz * dz <= CHAMBER_RADIUS * CHAMBER_RADIUS;
    }

    /** True unless the villager was claimed where it already stood in the chamber. */
    public static boolean shouldRecordOrigin(MpSeatLocation candidate, Optional<MpSeatLocation> chamber) {
        return chamber.isEmpty() || !isAtChamber(candidate, chamber.get());
    }

    /**
     * The first home known for a released villager — the seat's record, then the one stored on the
     * villager, then its bed — skipping any that merely names the chamber it is being sent from.
     */
    public static Optional<MpSeatLocation> releaseDestination(
            Optional<MpSeatLocation> seatOrigin,
            Optional<MpSeatLocation> entityOrigin,
            Optional<MpSeatLocation> homeBed,
            Optional<MpSeatLocation> chamber) {
        for (Optional<MpSeatLocation> candidate : List.of(seatOrigin, entityOrigin, homeBed)) {
            if (candidate.isPresent() && shouldRecordOrigin(candidate.get(), chamber)) {
                return candidate;
            }
        }
        return Optional.empty();
    }

    /** A villager this plugin spawned to fill a bench is dismissed when it has no home to go to. */
    public static boolean shouldDismiss(boolean pluginSpawned, boolean hasDestination) {
        return pluginSpawned && !hasDestination;
    }
}
