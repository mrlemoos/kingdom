package dev.mrlemoos.kingdom.model.city;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One Crown-authored Gazette post. Decrees may optionally carry a curfew window for display;
 * active enforcement lives on {@link KingdomCityState}.
 *
 * @param curfew empty when the post does not set or change a curfew window
 */
public record GazettePost(
        String title,
        String body,
        UUID authorUuid,
        long mcDay,
        GazettePostKind kind,
        Optional<GazetteCurfewWindow> curfew) {

    public GazettePost {
        title = title == null ? "" : title;
        body = body == null ? "" : body;
        authorUuid = Objects.requireNonNull(authorUuid, "authorUuid");
        kind = Objects.requireNonNull(kind, "kind");
        curfew = curfew == null ? Optional.empty() : curfew;
    }

    /** Inclusive Minecraft day-tick window attached to a decree for the Gazette record. */
    public record GazetteCurfewWindow(long startTick, long endTick) {}
}
