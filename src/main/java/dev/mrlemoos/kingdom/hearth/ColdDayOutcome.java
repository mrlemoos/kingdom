package dev.mrlemoos.kingdom.hearth;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the winter day came to: how many hearths burnt, what each villager yields for it, and who has
 * been cold long enough to down tools.
 */
public record ColdDayOutcome(int hearthsBurned, Map<UUID, Double> yieldFactors, Set<UUID> striking) {

    public ColdDayOutcome {
        yieldFactors = Map.copyOf(yieldFactors);
        striking = Set.copyOf(striking);
    }

    public double yieldFactorFor(UUID villagerId) {
        Double factor = yieldFactors.get(villagerId);
        return factor == null ? 1.0 : factor.doubleValue();
    }
}
