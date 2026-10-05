package dev.mrlemoos.kingdom.granary;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the winter day's reckoning at the table came to: what each villager yields for the hunger
 * behind it, who has gone hungry long enough to down tools, and who remains starving.
 */
public record HungerDayOutcome(
        Map<UUID, Double> yieldFactors, Set<UUID> striking, Set<UUID> starving) {

    public HungerDayOutcome {
        yieldFactors = Map.copyOf(yieldFactors);
        striking = Set.copyOf(striking);
        starving = Set.copyOf(starving);
    }

    public double yieldFactorFor(UUID villagerId) {
        Double factor = yieldFactors.get(villagerId);
        return factor == null ? 1.0 : factor.doubleValue();
    }

    /** Whether the realm is starving: somebody has remained unfed past the famine threshold. */
    public boolean famine() {
        return !starving.isEmpty();
    }
}
