package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.granary.GranaryStock;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class KingdomInfoSummaryTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static final Function<UUID, String> NAMES = id -> {
        if (ALICE.equals(id)) {
            return "Alice";
        }
        if (BOB.equals(id)) {
            return "Bob";
        }
        return null;
    };

    @Test
    void warLineAtPeaceWhenNoActiveWar() {
        assertEquals("War: at peace", KingdomInfoSummary.warLine("northmarch", Optional.empty(), id -> "X"));
    }

    @Test
    void warLineShowsEnemyAndAimRelativeToViewedKingdom() {
        ActiveWar war = new ActiveWar(
                "war-1",
                "northmarch",
                "northumbria",
                WarAim.TERRITORY_THRESHOLD,
                WarOutcome.ANNEXATION,
                0L,
                1L);
        Function<String, String> names = id -> {
            if ("northumbria".equals(id)) {
                return "Northumbria";
            }
            if ("northmarch".equals(id)) {
                return "Northmarch";
            }
            return id;
        };

        assertEquals(
                "War: vs Northumbria (territory threshold)",
                KingdomInfoSummary.warLine("northmarch", Optional.of(war), names));
        assertEquals(
                "War: vs Northmarch (territory threshold)",
                KingdomInfoSummary.warLine("northumbria", Optional.of(war), names));
    }

    @Test
    void policeLineNamesSingleConstableAndJudgeNoneWithCellOccupancy() {
        KingdomPoliceState police = new KingdomPoliceState();
        police.appointConstable(ALICE);
        police.setCell(1, new PrisonCellLocation("world", 0, 64, 0));

        assertEquals(
                "Police: Constable Alice, Judge none, cells 1",
                KingdomInfoSummary.policeLine(police, NAMES));
    }

    @Test
    void policeLineUsesCountsWhenMultipleConstables() {
        KingdomPoliceState police = new KingdomPoliceState();
        police.appointConstable(ALICE);
        police.appointConstable(BOB);

        assertEquals(
                "Police: 2 Constables, Judge none, cells 0",
                KingdomInfoSummary.policeLine(police, NAMES));
    }

    @Test
    void warLineShowsCapitalFallAim() {
        ActiveWar war = new ActiveWar(
                "war-2",
                "northmarch",
                "southreach",
                WarAim.CAPITAL_FALL,
                WarOutcome.WAR_TRIBUTE,
                0L,
                1L);

        assertEquals(
                "War: vs Southreach (capital fall)",
                KingdomInfoSummary.warLine(
                        "northmarch", Optional.of(war), id -> "southreach".equals(id) ? "Southreach" : id));
    }

    @Test
    void policeLineNamesSingleJudge() {
        KingdomPoliceState police = new KingdomPoliceState();
        police.appointJudge(BOB);

        assertEquals(
                "Police: Constable none, Judge Bob, cells 0",
                KingdomInfoSummary.policeLine(police, NAMES));
    }

    @Test
    void granaryLineShowsStockAgainstCapacityAndWhatItCovers() {
        assertEquals(
                "Granary: 12/40 bales (covers 4 days of winter)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.of(new GranaryStock(12, 40)), 3));
    }

    @Test
    void granaryLineCountsASingleDayInTheSingular() {
        assertEquals(
                "Granary: 3/40 bales (covers 1 day of winter)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.of(new GranaryStock(3, 40)), 3));
    }

    @Test
    void granaryLineOwnsUpToAStockThatCoversNoDayAtAll() {
        assertEquals(
                "Granary: 2/40 bales (covers no day of winter)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.of(new GranaryStock(2, 40)), 3));
    }

    @Test
    void granaryLineSaysTheWinterIsCoveredWhenItIs() {
        assertEquals(
                "Granary: 400/400 bales (covers the winter)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.of(new GranaryStock(400, 400)), 3));
        // No mouths to feed: whatever stands in the granary sees the winter through.
        assertEquals(
                "Granary: 0/40 bales (covers the winter)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.of(new GranaryStock(0, 40)), 0));
    }

    @Test
    void granaryLineNotesWhenNoneIsSited() {
        assertEquals("Granary: none sited", KingdomInfoSummary.granaryLine(null, Optional.empty(), 3));
        assertEquals("Granary: none sited", KingdomInfoSummary.granaryLine("  ", Optional.empty(), 3));
    }

    @Test
    void granaryLineOwnsUpWhenTheStockCannotBeRead() {
        assertEquals(
                "Granary: north_granary (stock unreadable)",
                KingdomInfoSummary.granaryLine("north_granary", Optional.empty(), 3));
    }

    @Test
    void loyaltyLineShowsTierDisplayName() {
        assertEquals("Loyalty: Faithful", KingdomInfoSummary.loyaltyLine(LoyaltyTier.FAITHFUL));
        assertEquals("Loyalty: Doubtful", KingdomInfoSummary.loyaltyLine(LoyaltyTier.DOUBTFUL));
        assertEquals("Loyalty: Disloyal", KingdomInfoSummary.loyaltyLine(LoyaltyTier.DISLOYAL));
        assertEquals("Loyalty: Traitor", KingdomInfoSummary.loyaltyLine(LoyaltyTier.TRAITOR));
    }

    @Test
    void warDebtLineIsNoneWhenTheRealmNeitherOwesNorIsOwed() {
        assertEquals("War debt: none", KingdomInfoSummary.warDebtLine(0, 0));
    }

    @Test
    void warDebtLineNamesWhatTheRealmOwesAndIsOwed() {
        assertEquals("War debt: owes 60 Corona", KingdomInfoSummary.warDebtLine(60, 0));
        assertEquals("War debt: is owed 40 Corona", KingdomInfoSummary.warDebtLine(0, 40));
        assertEquals("War debt: owes 60 Corona; is owed 40 Corona", KingdomInfoSummary.warDebtLine(60, 40));
    }
}
