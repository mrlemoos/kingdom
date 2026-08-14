package dev.mrlemoos.kingdom.feedback;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class HouseBarAudienceTest {

    @Test
    void theHouseSeesTheBar() {
        assertTrue(HouseBarAudience.sees(NobleRank.KING));
        assertTrue(HouseBarAudience.sees(NobleRank.QUEEN));
        assertTrue(HouseBarAudience.sees(NobleRank.PREMIER));
        assertTrue(HouseBarAudience.sees(NobleRank.SPEAKER));
        assertTrue(HouseBarAudience.sees(NobleRank.MP));
    }

    @Test
    void theRestOfTheRealmDoesNot() {
        assertFalse(HouseBarAudience.sees(null));
        assertFalse(HouseBarAudience.sees(NobleRank.PRINCE));
        assertFalse(HouseBarAudience.sees(NobleRank.DUKE));
        assertFalse(HouseBarAudience.sees(NobleRank.LORD));
        assertFalse(HouseBarAudience.sees(NobleRank.COUNT));
        assertFalse(HouseBarAudience.sees(NobleRank.KNIGHT));
    }
}
