package dev.mrlemoos.kingdom.war.levy;

import java.util.List;
import java.util.UUID;

/**
 * What the levy cost the realm on one day: what was owed, what the treasury could find, what stands
 * as arrears, whether the realm was warned, whose morale fell and who deserted.
 */
public record LevyDayOutcome(
        double billed,
        double paid,
        double arrears,
        boolean warningIssued,
        List<UUID> demoralised,
        List<UUID> deserters,
        List<String> announcements) {

    public static LevyDayOutcome nothing() {
        return new LevyDayOutcome(0.0, 0.0, 0.0, false, List.of(), List.of(), List.of());
    }

    public boolean unpaid() {
        return arrears > 0.0;
    }
}
