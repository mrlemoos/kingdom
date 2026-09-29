package dev.mrlemoos.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.GiltTerms;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GiltServiceTest {
    private final EconomyService economy = new EconomyService();
    private final String realm = "northmarch";
    private final UUID monarch = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID buyer = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void unsoldFaceBecomesTheIssuingMonarchsPersonalDebt() {
        economy.placeRoyalMint(realm, new MintLocation("world", 1, 64, 1), 2);
        economy.enactBudget(realm, 100, new GiltTerms(100, 0.001, 7), 2);
        assertInstanceOf(EconomyResult.Success.class, economy.openGiltSale(realm, monarch, 3));
        economy.creditWalletDirect(buyer, 60);
        assertInstanceOf(EconomyResult.Success.class, economy.buyGilt(realm, buyer, 60, 3));
        assertEquals(60, economy.getTreasuryBalance(realm), 1e-9);

        economy.settleGilts(realm, 10);

        assertEquals(100, economy.getGiltPrincipal(realm), 1e-9);
        assertEquals(100, economy.getNationalDebt(realm), 1e-9);
        assertEquals(40, economy.getGiltHolding(realm, monarch), 1e-9);
        assertEquals(60, economy.getGiltHolding(realm, buyer), 1e-9);
    }

    @Test
    void shortCouponCapitalisesAndDefaultsThenMaturityRetriesPrincipal() {
        economy.placeRoyalMint(realm, new MintLocation("world", 1, 64, 1), 2);
        economy.enactBudget(realm, 1, new GiltTerms(1, 0.001, 7), 0);
        economy.openGiltSale(realm, monarch, 0);
        economy.settleGilts(realm, 7);
        economy.settleGilts(realm, 8);

        assertEquals(1.001, economy.getGiltPrincipal(realm), 1e-9);
        assertTrue(economy.isGiltDefault(realm));
        economy.creditTreasury(realm, 2);
        economy.settleGilts(realm, 14);
        assertEquals(0, economy.getGiltPrincipal(realm), 1e-9);
        assertEquals(0, economy.getGiltHolding(realm, monarch), 1e-9);
        assertTrue(economy.kingdomEconomies().get(realm).giltIssues().get(0).paid(monarch) > 1.0);
    }
}
