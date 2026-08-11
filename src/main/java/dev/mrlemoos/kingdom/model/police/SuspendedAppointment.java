package dev.mrlemoos.kingdom.model.police;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import java.util.Optional;

/**
 * Appointed noble title and/or sworn role suspended for a prison sentence and restored exactly on
 * release. Elected offices are vacated instead, not suspended.
 */
public final class SuspendedAppointment {

    private final NobleRank nobleRank;
    private final TitleStyle titleStyle;
    private final SwornRole swornRole;

    public SuspendedAppointment(NobleRank nobleRank, TitleStyle titleStyle, SwornRole swornRole) {
        this.nobleRank = nobleRank;
        this.titleStyle = titleStyle;
        this.swornRole = swornRole;
    }

    public static SuspendedAppointment of(
            Optional<NobleRank> rank, Optional<TitleStyle> style, Optional<SwornRole> sworn) {
        return new SuspendedAppointment(
                rank.orElse(null),
                style.orElse(null),
                sworn.orElse(null));
    }

    public Optional<NobleRank> nobleRank() {
        return Optional.ofNullable(nobleRank);
    }

    public Optional<TitleStyle> titleStyle() {
        return Optional.ofNullable(titleStyle);
    }

    public Optional<SwornRole> swornRole() {
        return Optional.ofNullable(swornRole);
    }

    public boolean isEmpty() {
        return nobleRank == null && swornRole == null;
    }
}
