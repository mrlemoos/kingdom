package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import java.util.Optional;

/**
 * The granary's own news for the Gazette: word that the silo has no room left and the day's surplus
 * grain went to waste, and the twice-yearly reckoning of how far short of the winter the realm
 * stands. The overflow is hung once, when the granary first overflows, rather than on every day's
 * tally — the realm needs telling, not nagging.
 *
 * <p>Authored by the realm rather than by any hand, and so posted directly rather than through
 * {@link GazetteService}, which admits the Crown alone.
 */
public final class GranaryGazette {

    private GranaryGazette() {}

    /**
     * @param wastedWheat the wheat thrown away for want of room, in wheat rather than bales
     */
    public static GazettePost overflowPost(int wastedWheat, long mcDay) {
        int wasted = Math.max(0, wastedWheat);
        return new GazettePost(
                "The granary is full",
                "The granary has no room left: " + wasted
                        + " wheat of today's harvest was left to spoil in the fields. "
                        + "Build the granary out, or the realm stores no more against the winter.",
                SeasonGazette.REALM_AUTHOR,
                mcDay,
                GazettePostKind.ANNOUNCEMENT,
                Optional.empty());
    }

    /**
     * The shortfall as the realm is warned of it, on the season turn into Harvest and again on the
     * last day of autumn: the bales it stands short of seeing the winter through at its present
     * head-count, or word that it is provisioned — or that it has no granary at all.
     *
     * @param granaryRegionId the kingdom's linked granary region, or null when it has sited none
     * @param shortfallBales the bales it stands short, nought when it is provisioned
     */
    public static GazettePost shortfallPost(String granaryRegionId, int shortfallBales, long mcDay) {
        return new GazettePost(
                shortfallTitle(granaryRegionId, shortfallBales),
                shortfallBody(granaryRegionId, shortfallBales),
                SeasonGazette.REALM_AUTHOR,
                mcDay,
                GazettePostKind.ANNOUNCEMENT,
                Optional.empty());
    }

    private static String shortfallTitle(String granaryRegionId, int shortfallBales) {
        if (granaryRegionId == null || granaryRegionId.isBlank()) {
            return "The realm has no granary";
        }
        int shortfall = Math.max(0, shortfallBales);
        if (shortfall <= 0) {
            return "The granary is provisioned for the winter";
        }
        return "The granary stands " + shortfall + (shortfall == 1 ? " bale" : " bales") + " short";
    }

    /** The body of the warning; the town crier cries the same words the Gazette hangs. */
    public static String shortfallBody(String granaryRegionId, int shortfallBales) {
        if (granaryRegionId == null || granaryRegionId.isBlank()) {
            return "The realm has sited no granary, and so stores nothing against the winter. "
                    + "Its villagers will go hungry from the first day of Hallowtide. "
                    + "Link one with /kingdom granary setregion.";
        }
        int shortfall = Math.max(0, shortfallBales);
        if (shortfall <= 0) {
            return "The granary is provisioned: it holds hay enough to feed the realm "
                    + "through the whole of the winter at its present head-count.";
        }
        return "The granary stands " + shortfall + (shortfall == 1 ? " bale " : " bales ")
                + "short of feeding the realm through the winter at its present head-count. "
                + "Bring the grain in, or the villagers go hungry before the thaw.";
    }
}
