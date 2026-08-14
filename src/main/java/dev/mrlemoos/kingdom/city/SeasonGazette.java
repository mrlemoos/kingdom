package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import java.util.Optional;
import java.util.UUID;

/**
 * The season turn as a Gazette announcement, so the town crier cries it on his ticker alongside the
 * Crown's own news. Authored by the realm rather than by any hand, and so posted directly rather than
 * through {@link GazetteService}, which admits the Crown alone.
 */
public final class SeasonGazette {

    /** The realm itself: no player wrote the weather. */
    public static final UUID REALM_AUTHOR = new UUID(0L, 0L);

    private SeasonGazette() {}

    public static GazettePost post(Season season, long mcDay) {
        return new GazettePost(
                season.displayName() + " is come upon the realm",
                season.proclamation(),
                REALM_AUTHOR,
                mcDay,
                GazettePostKind.ANNOUNCEMENT,
                Optional.empty());
    }
}
