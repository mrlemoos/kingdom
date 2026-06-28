package dev.mrlemoos.kingdom.election;

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

    @Test
    void padsRemainingSeatsWithCitizenBackfill() {
        Map<String, Integer> counts = Map.of(
                "farmer", 10,
                "librarian", 8,
                "armorer", 6,
                "cleric", 5,
                "shepherd", 4,
                "fisherman", 3);

        assertEquals(
                List.of("farmer", "librarian", "armorer", "cleric", "shepherd", "fisherman", "none", "none"),
                ProfessionConstituencyResolver.topProfessionsWithCitizenBackfill(counts, 8));
    }

    @Test
    void citizenBackfillWhenNoProfessionsExist() {
        assertEquals(
                List.of("none", "none", "none"),
                ProfessionConstituencyResolver.topProfessionsWithCitizenBackfill(Map.of(), 3));
    }

    @Test
    void citizenBackfillDoesNotRankNoneAheadOfProfessionMps() {
        Map<String, Integer> counts = Map.of("none", 100, "farmer", 10, "librarian", 8);

        assertEquals(
                List.of("farmer", "librarian", "none", "none", "none", "none", "none", "none"),
                ProfessionConstituencyResolver.topProfessionsWithCitizenBackfill(counts, 8));
    }

    @Test
    void villagerProfessionNametagUsesCommonerForNoProfession() {
        assertEquals("Commoner", ProfessionConstituencyResolver.villagerProfessionNametag("none"));
        assertEquals(
                "Commoner",
                ProfessionConstituencyResolver.villagerProfessionNametag(
                        VillagerMpProfessionMatcher.NONE_PROFESSION_KEY));
    }

    @Test
    void villagerProfessionNametagCapitalisesProfession() {
        assertEquals("Farmer", ProfessionConstituencyResolver.villagerProfessionNametag("farmer"));
        assertEquals("Librarian", ProfessionConstituencyResolver.villagerProfessionNametag("librarian"));
    }

    @Test
    void mpDisplayLabelStillUsesCitizenForSeatBackfill() {
        assertEquals("Citizen", ProfessionConstituencyResolver.displayLabel("none"));
    }
}
