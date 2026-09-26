package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class BuiltGolemOathTest {

    @Test
    void aKnightsGolemInTerritoryIsSworn() {
        assertEquals(BuiltGolemOath.SWORN, BuiltGolemOath.decide(NobleRank.KNIGHT, false, true, 0, 2));
    }

    @Test
    void theCrownsGolemIsSworn() {
        assertEquals(BuiltGolemOath.SWORN, BuiltGolemOath.decide(NobleRank.KING, false, true, 1, 2));
        assertEquals(BuiltGolemOath.SWORN, BuiltGolemOath.decide(NobleRank.QUEEN, false, true, 1, 2));
    }

    @Test
    void anOperatorWhoIsAMemberMayBuildAnOfficer() {
        assertEquals(BuiltGolemOath.SWORN, BuiltGolemOath.decide(null, true, true, 0, 2));
    }

    @Test
    void aCommonersGolemStaysVanilla() {
        assertEquals(BuiltGolemOath.NOT_AUTHORISED, BuiltGolemOath.decide(null, false, true, 0, 2));
        assertEquals(BuiltGolemOath.NOT_AUTHORISED, BuiltGolemOath.decide(NobleRank.DUKE, false, true, 0, 2));
    }

    @Test
    void aGolemBuiltOutsideTerritoryStaysVanilla() {
        assertEquals(BuiltGolemOath.OUTSIDE_TERRITORY, BuiltGolemOath.decide(NobleRank.KNIGHT, false, false, 0, 2));
    }

    @Test
    void atTheCapTheNextGolemStaysVanilla() {
        assertEquals(BuiltGolemOath.CAPPED, BuiltGolemOath.decide(NobleRank.KNIGHT, false, true, 2, 2));
    }

    @Test
    void aCommonerAtTheCapIsNotToldOfTheCap() {
        assertEquals(BuiltGolemOath.NOT_AUTHORISED, BuiltGolemOath.decide(NobleRank.MP, false, true, 2, 2));
    }
}
