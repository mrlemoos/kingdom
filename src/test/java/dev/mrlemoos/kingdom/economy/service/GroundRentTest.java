package dev.mrlemoos.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroundRentTest {
    private final EconomyService economy = new EconomyService();
    private final UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void unpaidBeaconRentBecomesArrearsAndBreakingStopsFutureRent() {
        economy.recordEstatePlacement("northmarch", "world", 1, 64, 1, WealthBlockType.BEACON, owner);
        economy.creditWalletDirect(owner, 0.2);

        economy.settleGroundRent("northmarch", true, Map.of(owner, NobleRank.KNIGHT), RealmWealthRates.defaults());

        assertEquals(0.2, economy.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.3, economy.getGroundRentArrears("northmarch", owner), 1e-9);
        economy.removeEstate("northmarch", "world", 1, 64, 1);
        economy.settleGroundRent("northmarch", true, Map.of(owner, NobleRank.KNIGHT), RealmWealthRates.defaults());
        assertEquals(0.3, economy.getGroundRentArrears("northmarch", owner), 1e-9);
    }

    @Test
    void royalRankAndAbsentCapitalPauseNewRentButNotEarlierArrears() {
        economy.recordEstatePlacement("northmarch", "world", 1, 64, 1, WealthBlockType.BEACON, owner);
        economy.settleGroundRent("northmarch", true, Map.of(owner, NobleRank.KNIGHT), RealmWealthRates.defaults());
        economy.creditWalletDirect(owner, 0.2);
        economy.settleGroundRent("northmarch", false, Map.of(owner, NobleRank.KING), RealmWealthRates.defaults());
        assertEquals(0.3, economy.getGroundRentArrears("northmarch", owner), 1e-9);
    }
}
