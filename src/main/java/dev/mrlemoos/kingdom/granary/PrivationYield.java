package dev.mrlemoos.kingdom.granary;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What a villager yields on a day it is both frozen and starving: the one cut laid on top of the
 * other. Cold and hunger keep separate ledgers and are answered separately — a hearth lit does
 * nothing for an empty granary — so their cuts multiply rather than one hiding the other.
 */
public final class PrivationYield {

    private PrivationYield() {}

    /** The cold day's factors and the hungry day's, multiplied where they fall on the same villager. */
    public static Map<UUID, Double> combine(Map<UUID, Double> cold, Map<UUID, Double> hunger) {
        Map<UUID, Double> combined = new LinkedHashMap<>();
        if (cold != null) {
            combined.putAll(cold);
        }
        if (hunger == null) {
            return Map.copyOf(combined);
        }
        for (Map.Entry<UUID, Double> entry : hunger.entrySet()) {
            Double standing = combined.get(entry.getKey());
            combined.put(
                    entry.getKey(),
                    standing == null
                            ? entry.getValue()
                            : Double.valueOf(standing.doubleValue() * entry.getValue().doubleValue()));
        }
        return Map.copyOf(combined);
    }
}
