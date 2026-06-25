package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfessionConstituencyResolverTest {

    @Test
    void returnsTopProfessionsByCount() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("farmer", 10);
        counts.put("librarian", 5);
        counts.put("armorer", 8);
        counts.put("cleric", 3);

        assertEquals(
                List.of("farmer", "armorer", "librarian", "cleric"),
                ProfessionConstituencyResolver.topProfessions(counts, 4));
    }

    @Test
    void breaksTiesAlphabetically() {
        Map<String, Integer> counts = Map.of("farmer", 5, "armorer", 5, "cleric", 5);

        assertEquals(
                List.of("armorer", "cleric", "farmer"),
                ProfessionConstituencyResolver.topProfessions(counts, 3));
    }

    @Test
    void excludesAlreadySeatedProfessions() {
        Map<String, Integer> counts = Map.of("farmer", 10, "librarian", 8, "armorer", 6);

        assertEquals(
                List.of("armorer"),
                ProfessionConstituencyResolver.topProfessionsExcluding(counts, 1, List.of("farmer", "librarian")));
    }
}
