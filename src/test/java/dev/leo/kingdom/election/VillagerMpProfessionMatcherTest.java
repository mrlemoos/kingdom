package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMpProfessionMatcherTest {

    @Test
    void citizenProfessionMatchesUnemployedKey() {
        assertTrue(VillagerMpProfessionMatcher.matchesKey(
                ProfessionConstituencyResolver.CITIZEN_PROFESSION, VillagerMpProfessionMatcher.NONE_PROFESSION_KEY));
    }

    @Test
    void farmerProfessionMatchesFarmerKey() {
        assertTrue(VillagerMpProfessionMatcher.matchesKey("farmer", "minecraft:farmer"));
        assertFalse(VillagerMpProfessionMatcher.matchesKey("librarian", "minecraft:farmer"));
    }
}
