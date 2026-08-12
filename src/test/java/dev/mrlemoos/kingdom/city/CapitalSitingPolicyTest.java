package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.city.CapitalSitingPolicy.Verdict;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CapitalSitingPolicyTest {

    @Test
    void theCrownIsTheKingOrQueenOnly() {
        assertTrue(CapitalSitingPolicy.isCrown(NobleRank.KING));
        assertTrue(CapitalSitingPolicy.isCrown(NobleRank.QUEEN));
        assertFalse(CapitalSitingPolicy.isCrown(NobleRank.PRINCE));
        assertFalse(CapitalSitingPolicy.isCrown(null));
    }

    @Test
    void aMonarchMaySiteTheCapitalInsideTheirOwnTerritory() {
        assertEquals(
                Verdict.ALLOWED,
                CapitalSitingPolicy.evaluate(NobleRank.QUEEN, "avalon", Optional.of("avalon")));
    }

    @Test
    void aPrinceMayNotSiteTheCapital() {
        assertEquals(
                Verdict.NOT_THE_CROWN,
                CapitalSitingPolicy.evaluate(NobleRank.PRINCE, "avalon", Optional.of("avalon")));
    }

    @Test
    void unclaimedLandIsRefused() {
        assertEquals(
                Verdict.UNCLAIMED_LAND,
                CapitalSitingPolicy.evaluate(NobleRank.KING, "avalon", Optional.empty()));
    }

    @Test
    void foreignTerritoryIsRefused() {
        assertEquals(
                Verdict.FOREIGN_TERRITORY,
                CapitalSitingPolicy.evaluate(NobleRank.KING, "avalon", Optional.of("mercia")));
    }

    @Test
    void aMonarchOfNoKingdomIsRefused() {
        assertEquals(
                Verdict.NO_KINGDOM,
                CapitalSitingPolicy.evaluate(NobleRank.KING, null, Optional.of("avalon")));
    }

    @Test
    void everyRefusalCarriesAMessage() {
        for (Verdict verdict : Verdict.values()) {
            if (verdict == Verdict.ALLOWED) {
                assertTrue(CapitalSitingPolicy.refusalMessage(verdict).isEmpty());
            } else {
                assertFalse(CapitalSitingPolicy.refusalMessage(verdict).isEmpty());
            }
        }
    }
}
