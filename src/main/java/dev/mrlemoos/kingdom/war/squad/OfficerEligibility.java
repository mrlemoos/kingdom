package dev.mrlemoos.kingdom.war.squad;

import dev.mrlemoos.kingdom.war.muster.MusterAnswer;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Domain stub for "officer is a military participant" (see Slice 5.3's {@code SquadService}): a
 * small predicate port composed purely from existing war-domain services, deliberately avoiding
 * any Bukkit type. A member is a military participant if they are either on-duty on the Crown's
 * standing roster (see {@link StandingRosterService#isOnDuty}), or have answered the muster for
 * the currently active war (see {@link MusterService#answerOf}).
 */
public final class OfficerEligibility {

    private OfficerEligibility() {}

    /**
     * Builds the standing-roster-or-muster predicate. {@code activeWarId} is resolved lazily on
     * every {@link Predicate#test} call, so the same predicate instance stays correct as the
     * kingdom's active war changes (or ends) over time. Either service argument may be {@code
     * null} to disable that half of the check; a {@code null} or blank id returned by {@code
     * activeWarId} disables the muster half for that call only.
     */
    public static Predicate<UUID> standingRosterOrMuster(
            StandingRosterService standingRosterService, MusterService musterService, Supplier<String> activeWarId) {
        Objects.requireNonNull(activeWarId, "activeWarId");
        return officerId -> {
            if (standingRosterService != null && standingRosterService.isOnDuty(officerId)) {
                return true;
            }
            if (musterService == null) {
                return false;
            }
            String warId = activeWarId.get();
            if (warId == null || warId.isBlank()) {
                return false;
            }
            Optional<MusterAnswer> answer = musterService.answerOf(warId, officerId);
            return answer.isPresent() && answer.get() == MusterAnswer.ANSWERED;
        };
    }
}
