package dev.mrlemoos.kingdom.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FoundationStoneLayingTest {

    private static FoundationStoneLaying lay(NobleRank rank, Optional<String> owner) {
        return FoundationStoneLaying.judge(FoundationStone.CHURCH, rank, "northmarch", "northmarch", owner);
    }

    @Test
    void theCrownLayingInsideItsOwnTerritoryHolds() {
        FoundationStoneLaying laying = lay(NobleRank.KING, Optional.of("northmarch"));

        assertTrue(laying.holds());
        assertTrue(laying.stoneSpent());
        assertEquals("", laying.refusal());
    }

    @Test
    void laidOutsideTerritoryIsRefusedAndTheStoneKept() {
        FoundationStoneLaying laying = lay(NobleRank.QUEEN, Optional.empty());

        assertFalse(laying.holds());
        assertFalse(laying.stoneSpent());
        assertEquals("The church must stand inside your kingdom's territory.", laying.refusal());
    }

    @Test
    void laidInAnotherRealmIsRefused() {
        FoundationStoneLaying laying = lay(NobleRank.KING, Optional.of("southmarch"));

        assertFalse(laying.stoneSpent());
        assertEquals("You may not raise the church in another realm's territory.", laying.refusal());
    }

    @Test
    void aSubjectWhoIsNotTheCrownIsRefused() {
        FoundationStoneLaying laying = lay(NobleRank.PRINCE, Optional.of("northmarch"));

        assertFalse(laying.stoneSpent());
        assertEquals("Only the King or Queen may lay the church's foundation stone.", laying.refusal());
    }

    @Test
    void aStoneCutForAnotherRealmIsRefused() {
        FoundationStoneLaying laying = FoundationStoneLaying.judge(
                FoundationStone.CHURCH, NobleRank.KING, "northmarch", "southmarch", Optional.of("northmarch"));

        assertFalse(laying.stoneSpent());
        assertEquals("This stone was cut for another realm.", laying.refusal());
    }

    @Test
    void aSiteThatRefusesAfterTheRulesKeepsTheStone() {
        FoundationStoneLaying laying = lay(NobleRank.KING, Optional.of("northmarch"))
                .settle(Optional.of("Unknown kingdom."));

        assertFalse(laying.stoneSpent());
        assertEquals("Unknown kingdom.", laying.refusal());
        assertTrue(lay(NobleRank.KING, Optional.of("northmarch")).settle(Optional.empty()).stoneSpent());
    }

    @Test
    void theCityChurchAndMintStonesAreCutFromTheHub() {
        assertEquals(Optional.of(FoundationStone.CHURCH), FoundationStone.forTopic(RealmHubTopic.PLACE_CHURCH));
        assertEquals(Optional.of(FoundationStone.CAPITAL), FoundationStone.forTopic(RealmHubTopic.PLACE_CAPITAL));
        assertEquals(
                Optional.of(FoundationStone.TOWN_CRIER), FoundationStone.forTopic(RealmHubTopic.PLACE_TOWN_CRIER));
        assertEquals(Optional.of(FoundationStone.MINT), FoundationStone.forTopic(RealmHubTopic.PLACE_MINTS));
        assertEquals(Optional.empty(), FoundationStone.forTopic(RealmHubTopic.POWER_SITES));
        assertEquals(Optional.of(RealmHubTopic.PLACE_MINTS), FoundationStone.MINT.topic());
        assertEquals(13, FoundationStone.values().length);
    }

    @Test
    void aLordMayLayAMintButNotTheCapitalOrTheCrier() {
        assertTrue(FoundationStoneLaying.judge(
                        FoundationStone.MINT, NobleRank.LORD, "northmarch", "northmarch", Optional.of("northmarch"))
                .stoneSpent());
        assertEquals(
                "Only the King or Queen may lay the capital's foundation stone.",
                FoundationStoneLaying.judge(
                                FoundationStone.CAPITAL,
                                NobleRank.LORD,
                                "northmarch",
                                "northmarch",
                                Optional.of("northmarch"))
                        .refusal());
        assertEquals(
                "Only the King or Queen may lay the Town Crier's foundation stone.",
                FoundationStoneLaying.judge(
                                FoundationStone.TOWN_CRIER,
                                NobleRank.PRINCE,
                                "northmarch",
                                "northmarch",
                                Optional.of("northmarch"))
                        .refusal());
    }

    @Test
    void aKnightMayNotLayAMint() {
        FoundationStoneLaying laying = FoundationStoneLaying.judge(
                FoundationStone.MINT, NobleRank.KNIGHT, "northmarch", "northmarch", Optional.of("northmarch"));

        assertFalse(laying.stoneSpent());
        assertEquals("Only the King, Queen or a Lord may lay a mint's foundation stone.", laying.refusal());
    }

    @Test
    void everyStoneLaidOutsideTerritoryIsKept() {
        for (FoundationStone kind : List.of(
                FoundationStone.CAPITAL, FoundationStone.TOWN_CRIER, FoundationStone.MINT, FoundationStone.CHURCH)) {
            FoundationStoneLaying laying =
                    FoundationStoneLaying.judge(kind, NobleRank.KING, "northmarch", "northmarch", Optional.empty());
            assertFalse(laying.stoneSpent(), kind.name());
            assertTrue(laying.refusal().endsWith("must stand inside your kingdom's territory."), laying.refusal());
        }
    }

    @Test
    void aSiteRefusalKeepsEveryKindOfStone() {
        FoundationStoneLaying crier = FoundationStoneLaying.judge(
                        FoundationStone.TOWN_CRIER, NobleRank.QUEEN, "northmarch", "northmarch", Optional.of("northmarch"))
                .settle(Optional.of("Site a capital before placing the Town Crier."));
        FoundationStoneLaying mint = FoundationStoneLaying.judge(
                        FoundationStone.MINT, NobleRank.LORD, "northmarch", "northmarch", Optional.of("northmarch"))
                .settle(Optional.of("Your kingdom already has the maximum of 3 mints."));

        assertFalse(crier.stoneSpent());
        assertFalse(mint.stoneSpent());
        assertEquals("Site a capital before placing the Town Crier.", crier.refusal());
    }

    @Test
    void onlyTheCrownClearsASiteAndTheMintStoneGoesToLordsToo() {
        assertTrue(FoundationStone.MINT.mayLay(NobleRank.LORD));
        assertFalse(FoundationStone.MINT.mayClear(NobleRank.LORD));
        assertTrue(FoundationStone.MINT.mayClear(NobleRank.QUEEN));
        assertFalse(FoundationStone.CAPITAL.mayLay(NobleRank.LORD));
        assertTrue(FoundationStone.CAPITAL.mayLay(NobleRank.KING));
        assertFalse(FoundationStone.TOWN_CRIER.mayLay(null));
        assertEquals("the King, Queen or a Lord", FoundationStone.MINT.layers());
        assertEquals("the King or Queen", FoundationStone.CAPITAL.layers());
    }

    @Test
    void theParliamentStonesAreCutFromTheHub() {
        assertEquals(Optional.of(FoundationStone.COMMONS), FoundationStone.forTopic(RealmHubTopic.PLACE_COMMONS));
        assertEquals(Optional.of(FoundationStone.LORDS), FoundationStone.forTopic(RealmHubTopic.PLACE_LORDS));
        assertEquals(
                Optional.of(FoundationStone.SPEAKER_CHAIR), FoundationStone.forTopic(RealmHubTopic.PLACE_SPEAKER_CHAIR));
        assertEquals(Optional.of(FoundationStone.BAR), FoundationStone.forTopic(RealmHubTopic.PLACE_BAR));
        assertEquals(Optional.of(FoundationStone.MP_SEAT), FoundationStone.forTopic(RealmHubTopic.PLACE_MP_SEATS));
        assertEquals(Optional.of(FoundationStone.REGISTRAR), FoundationStone.forTopic(RealmHubTopic.PLACE_REGISTRAR));
        assertEquals(Optional.of(RealmHubTopic.PLACE_MP_SEATS), FoundationStone.MP_SEAT.topic());
    }

    @Test
    void theParliamentStonesAreTheCrownsAlone() {
        for (FoundationStone kind : List.of(
                FoundationStone.COMMONS,
                FoundationStone.LORDS,
                FoundationStone.SPEAKER_CHAIR,
                FoundationStone.BAR,
                FoundationStone.MP_SEAT,
                FoundationStone.REGISTRAR)) {
            assertTrue(kind.mayLay(NobleRank.QUEEN), kind.name());
            assertFalse(kind.mayLay(NobleRank.PREMIER), kind.name());
            assertFalse(kind.mayLay(NobleRank.PRINCE), kind.name());
        }
        assertEquals(
                "Only the King or Queen may lay an MP seat's foundation stone.",
                FoundationStoneLaying.judge(
                                FoundationStone.MP_SEAT,
                                NobleRank.SPEAKER,
                                "northmarch",
                                "northmarch",
                                Optional.of("northmarch"))
                        .refusal());
    }

    @Test
    void theCourtCellAndGranaryStonesAreCutFromTheHub() {
        assertEquals(Optional.of(FoundationStone.COURT), FoundationStone.forTopic(RealmHubTopic.PLACE_COURT));
        assertEquals(Optional.of(FoundationStone.CELL), FoundationStone.forTopic(RealmHubTopic.PLACE_PRISON));
        assertEquals(Optional.of(FoundationStone.GRANARY), FoundationStone.forTopic(RealmHubTopic.PLACE_GRANARY));
        assertEquals(Optional.of(RealmHubTopic.PLACE_PRISON), FoundationStone.CELL.topic());
        for (FoundationStone kind : FoundationStone.values()) {
            assertTrue(kind.topic().isPresent(), kind.name() + " has no Hub place");
        }
    }

    @Test
    void theCourtsLecternStaysWhereItIsLaidAndTheCellAndGranaryStonesAreSpent() {
        assertTrue(FoundationStone.COURT.staysWhereLaid());
        assertTrue(FoundationStone.REGISTRAR.staysWhereLaid());
        assertFalse(FoundationStone.CELL.staysWhereLaid());
        assertFalse(FoundationStone.GRANARY.staysWhereLaid());
    }

    @Test
    void theCourtCellAndGranaryStonesAreTheCrownsAlone() {
        for (FoundationStone kind : List.of(FoundationStone.COURT, FoundationStone.CELL, FoundationStone.GRANARY)) {
            assertTrue(kind.mayLay(NobleRank.KING), kind.name());
            assertFalse(kind.mayLay(NobleRank.KNIGHT), kind.name());
            assertFalse(kind.mayLay(NobleRank.PRINCE), kind.name());
            assertTrue(kind.mayClear(NobleRank.QUEEN), kind.name());
            assertFalse(kind.mayClear(NobleRank.DUKE), kind.name());
        }
        assertEquals(
                "Only the King or Queen may lay a cell's foundation stone.",
                FoundationStoneLaying.judge(
                                FoundationStone.CELL, NobleRank.KNIGHT, "northmarch", "northmarch", Optional.of("northmarch"))
                        .refusal());
    }

    @Test
    void aCourtCellOrGranaryStoneRefusedIsKept() {
        for (FoundationStone kind : List.of(FoundationStone.COURT, FoundationStone.CELL, FoundationStone.GRANARY)) {
            FoundationStoneLaying outside =
                    FoundationStoneLaying.judge(kind, NobleRank.QUEEN, "northmarch", "northmarch", Optional.empty());
            assertFalse(outside.stoneSpent(), kind.name());
            assertTrue(outside.refusal().endsWith("must stand inside your kingdom's territory."), outside.refusal());
        }
        FoundationStoneLaying granary = FoundationStoneLaying.judge(
                        FoundationStone.GRANARY, NobleRank.KING, "northmarch", "northmarch", Optional.of("northmarch"))
                .settle(Optional.of("No region lies around the hay bale inside your territory."));
        assertFalse(granary.stoneSpent());
        assertEquals("No region lies around the hay bale inside your territory.", granary.refusal());
    }
}
