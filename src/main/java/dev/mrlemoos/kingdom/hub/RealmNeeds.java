package dev.mrlemoos.kingdom.hub;

import java.util.ArrayList;
import java.util.List;

/** Current food and warmth supplies, rendered for a realm subject. */
public final class RealmNeeds {

    private RealmNeeds() {}

    public static List<String> lines(
            String granaryRegion,
            int foodShortfall,
            int knownFuel,
            int hearths,
            int lowHearths,
            List<String> lowLocations,
            boolean fuelScanComplete) {
        ArrayList<String> lines = new ArrayList<>();
        if (granaryRegion == null || granaryRegion.isBlank()) {
            lines.add("Food: no granary. The Crown must site one from the Realm Hub.");
        } else if (foodShortfall < 0) {
            lines.add("Food: granary stock unavailable; check the granary in person.");
        } else if (foodShortfall <= 0) {
            lines.add("Food: granary holds enough hay for this winter.");
        } else {
            lines.add("Food: " + foodShortfall + (foodShortfall == 1 ? " bale" : " bales") + " short at "
                    + granaryRegion + ". Take hay there and sneak-right-click its bale.");
        }
        if (fuelScanComplete && hearths == 0) {
            lines.add("Warmth: no burning hearth. Light a campfire with a touching container in the realm. Build permits apply.");
        } else if (!fuelScanComplete) {
            lines.add("Warmth: some hearth stores are unloaded; check the hearths in person."
                    + (lowLocations.isEmpty() ? "" : " Known low hearths: " + locations(lowLocations) + "."));
        } else if (lowHearths <= 0) {
            lines.add("Warmth: all " + hearths + " hearths hold one day's fuel (" + knownFuel + " fuel items).");
        } else {
            lines.add("Warmth: " + lowHearths + (lowHearths == 1 ? " hearth needs" : " hearths need")
                    + " one day's fuel (" + knownFuel + " fuel items in all stores). Right-click each lit campfire with fuel.");
            lines.add("Low hearths at " + locations(lowLocations) + ".");
        }
        return List.copyOf(lines);
    }

    private static String locations(List<String> locations) {
        int shown = Math.min(3, locations.size());
        return String.join("; ", locations.subList(0, shown))
                + (locations.size() > shown ? "; and " + (locations.size() - shown) + " more" : "");
    }
}
