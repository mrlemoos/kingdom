package dev.mrlemoos.kingdom.election;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProfessionConstituencyResolver {

    public static final String CITIZEN_PROFESSION = "none";

    private static int compareByCountDescThenKey(
            Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
        int byCount = Integer.compare(right.getValue(), left.getValue());
        if (byCount != 0) {
            return byCount;
        }
        return left.getKey().compareTo(right.getKey());
    }

    private ProfessionConstituencyResolver() {
    }

    public static List<String> topProfessions(Map<String, Integer> professionCounts, int limit) {
        if (limit <= 0 || professionCounts == null || professionCounts.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(professionCounts.entrySet());
        sorted.sort(ProfessionConstituencyResolver::compareByCountDescThenKey);
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            result.add(entry.getKey());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public static List<String> topProfessionsWithCitizenBackfill(Map<String, Integer> professionCounts, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<String> ranked = topProfessionsExcluding(professionCounts, limit, List.of(CITIZEN_PROFESSION));
        List<String> result = new ArrayList<>(ranked);
        while (result.size() < limit) {
            result.add(CITIZEN_PROFESSION);
        }
        return result;
    }

    public static String displayLabel(String profession) {
        if (CITIZEN_PROFESSION.equals(profession)) {
            return "Citizen";
        }
        return capitaliseProfession(profession);
    }

    /** Nametag suffix for ordinary villagers (not seated profession MPs). */
    public static String villagerProfessionNametag(String profession) {
        String normalised = profession.contains(":")
                ? profession.substring(profession.indexOf(':') + 1).toLowerCase()
                : profession.toLowerCase();
        if (CITIZEN_PROFESSION.equals(normalised) || "none".equals(normalised)) {
            return "Commoner";
        }
        return capitaliseProfession(normalised);
    }

    private static String capitaliseProfession(String profession) {
        if (profession.isEmpty()) {
            return "Commoner";
        }
        return profession.substring(0, 1).toUpperCase() + profession.substring(1).toLowerCase();
    }

    public static List<String> topProfessionsExcluding(
            Map<String, Integer> professionCounts, int limit, List<String> exclude) {
        if (limit <= 0 || professionCounts == null || professionCounts.isEmpty()) {
            return List.of();
        }
        List<String> excluded = exclude != null ? exclude : List.of();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(professionCounts.entrySet());
        sorted.sort(ProfessionConstituencyResolver::compareByCountDescThenKey);
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
