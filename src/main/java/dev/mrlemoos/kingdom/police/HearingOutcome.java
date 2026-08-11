package dev.mrlemoos.kingdom.police;

import java.util.Objects;
import java.util.Optional;

/** Result of {@link TrialJuryService#resolveHearing}. */
public record HearingOutcome(
        HearingResolution resolution, PoliceResult result, Optional<TrialJurySession> session) {

    public HearingOutcome {
        Objects.requireNonNull(resolution, "resolution");
        Objects.requireNonNull(result, "result");
        session = session == null ? Optional.empty() : session;
    }

    public static HearingOutcome of(
            HearingResolution resolution, PoliceResult result, Optional<TrialJurySession> session) {
        return new HearingOutcome(resolution, result, session);
    }
}
