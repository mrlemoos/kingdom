package dev.leo.kingdom.election;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ProfessionConstituencyResolver {

    private ProfessionConstituencyResolver() {}

    public static List<String> topProfessions(Map<String, Integer> professionCounts, int limit) {
        if (limit <= 0 || professionCounts == null || professionCounts.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(professionCounts.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        return sorted.stream().limit(limit).map(Map.Entry::getKey).toList();
    }

    public static List<String> topProfessionsExcluding(
            Map<String, Integer> professionCounts, int limit, List<String> exclude) {
        if (limit <= 0 || professionCounts == null || professionCounts.isEmpty()) {
            return List.of();
        }
        List<String> excluded = exclude != null ? exclude : List.of();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(professionCounts.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            if (excluded.contains(entry.getKey())) {
                continue;
            }
            result.add(entry.getKey());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }
}
