package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TownCrierDisplayHeightTest {

    @Test
    @DisplayName("the ticker sits clear of the Town Crier nametag")
    void theTickerSitsClearOfTheNametag() {
        assertEquals(2.70, TownCrierService.DISPLAY_HEIGHT);
    }
}
