package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.church.ChurchSite;
import org.junit.jupiter.api.Test;

class ChurchProximityTest {

    private static final ChurchSite CHURCH = new ChurchSite("world", 0, 64, 0, 0f, 0f);

    @Test
    void standingAtTheAltarCounts() {
        assertTrue(ChurchProximity.isAtChurch(CHURCH, "world", 2, 64, 2));
    }

    @Test
    void shoutingFromAcrossTheRealmDoesNot() {
        assertFalse(ChurchProximity.isAtChurch(CHURCH, "world", 200, 64, 0));
    }

    @Test
    void anotherWorldIsNeverTheChurch() {
        assertFalse(ChurchProximity.isAtChurch(CHURCH, "world_nether", 0, 64, 0));
    }
}
