package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import java.util.List;
import java.util.Optional;

/** The Town Crier's current warning when the realm cannot warm its hearths tomorrow. */
public final class HearthGazette {

    private HearthGazette() {}

    public static GazettePost lowFuelPost(int hearths, List<String> lowLocations, long mcDay) {
        String title = hearths == 0 ? "The realm has no hearths" : "Hearth fuel runs low";
        String body = hearths == 0
                ? "The realm has no burning hearth with a fuel store. Light a campfire with a container touching it in the realm."
                : "Some hearths do not hold one day's fuel. Right-click each lit campfire with fuel. Low hearths at "
                        + String.join("; ", lowLocations) + ".";
        return new GazettePost(
                title, body, SeasonGazette.REALM_AUTHOR, mcDay, GazettePostKind.ANNOUNCEMENT, Optional.empty());
    }

    public static GazettePost readyPost(long mcDay) {
        return new GazettePost(
                "The hearths are ready",
                "Every burning hearth holds at least one day's fuel.",
                SeasonGazette.REALM_AUTHOR,
                mcDay,
                GazettePostKind.ANNOUNCEMENT,
                Optional.empty());
    }

    public static String stateOf(GazettePost post) {
        return switch (post.title()) {
            case "The realm has no hearths" -> "no-hearth";
            case "Hearth fuel runs low" -> "low-fuel";
            case "The hearths are ready" -> "ready";
            default -> "";
        };
    }
}
