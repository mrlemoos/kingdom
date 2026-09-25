package dev.mrlemoos.kingdom.treaty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.treaty.TreatyRegister.Row;
import dev.mrlemoos.kingdom.treaty.TreatyRegister.Status;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreatyRegisterTest {

    private TreatyService treaties;

    @BeforeEach
    void setUp() {
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        kingdoms.createKingdom("eastfold", "Eastfold");
        treaties = new TreatyService(kingdoms);
    }

    @Test
    void listsOnlyTheRealmsOwnTreatiesWithTheirStanding() {
        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);
        treaties.assent("eastfold", "northmarch", TreatyKind.NON_AGGRESSION, 10);
        treaties.assent("southreach", "eastfold", TreatyKind.TRADE_PACT, 10);

        List<Row> rows = TreatyRegister.rows("northmarch", treaties.treatiesView(), 12);

        assertEquals(2, rows.size());
        Row awaitingThem = row(rows, "southreach", TreatyKind.TRADE_PACT);
        assertEquals(Status.AWAITING_THEM, awaitingThem.status());
        assertEquals(5, awaitingThem.daysLeft());
        assertFalse(awaitingThem.crownMayAnswer());
        Row awaitingUs = row(rows, "eastfold", TreatyKind.NON_AGGRESSION);
        assertEquals(Status.AWAITING_US, awaitingUs.status());
        assertTrue(awaitingUs.crownMayAnswer());
        assertFalse(awaitingUs.answerIsRepeal());
    }

    @Test
    void activeTreatyOffersRepealAndTracksARepealInFlight() {
        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);
        treaties.assent("southreach", "northmarch", TreatyKind.TRADE_PACT, 10);

        Row active = TreatyRegister.rows("northmarch", treaties.treatiesView(), 10).get(0);
        assertEquals(Status.ACTIVE, active.status());
        assertTrue(active.crownMayAnswer());
        assertTrue(active.answerIsRepeal());

        treaties.repeal("southreach", "northmarch", TreatyKind.TRADE_PACT, 11);

        Row ours = TreatyRegister.rows("northmarch", treaties.treatiesView(), 11).get(0);
        assertEquals(Status.REPEAL_AWAITING_US, ours.status());
        assertTrue(ours.answerIsRepeal());
        Row theirs = TreatyRegister.rows("southreach", treaties.treatiesView(), 11).get(0);
        assertEquals(Status.REPEAL_AWAITING_THEM, theirs.status());
        assertFalse(theirs.crownMayAnswer());
    }

    @Test
    void lapsedProposalIsLeftOff() {
        treaties.assent("northmarch", "southreach", TreatyKind.TRADE_PACT, 10);

        assertTrue(TreatyRegister.rows("northmarch", treaties.treatiesView(), 17).isEmpty());
    }

    private static Row row(List<Row> rows, String counterpart, TreatyKind kind) {
        return rows.stream()
                .filter(row -> row.counterpartId().equals(counterpart) && row.kind() == kind)
                .findFirst()
                .orElseThrow();
    }
}
