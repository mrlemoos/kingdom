package dev.mrlemoos.kingdom.parliament.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParliamentHubViewTest {

    @Test
    void premierSeesTableButtonsInCommons() {
        ParliamentHubView view = new ParliamentHubView(
                NobleRank.PREMIER,
                null,
                true,
                false,
                false,
                false,
                false,
                Optional.empty());

        Set<ParliamentHubAction> visible = view.visibleActions();

        assertTrue(visible.contains(ParliamentHubAction.TABLE_FISCAL));
        assertTrue(visible.contains(ParliamentHubAction.TABLE_BUDGET));
        assertTrue(visible.contains(ParliamentHubAction.TABLE_SPEND_MINT));
        assertTrue(visible.contains(ParliamentHubAction.TABLE_SPEND_STIPEND));
        assertTrue(visible.contains(ParliamentHubAction.STIPEND_OTHER));
    }

    @Test
    void speakerSeesOpenAndCloseDivisionInCommons() {
        ParliamentHubView openView = new ParliamentHubView(
                NobleRank.SPEAKER,
                BillState.TABLED,
                true,
                false,
                false,
                false,
                false,
                Optional.of("Finance Act 2026"));

        assertTrue(openView.visibleActions().contains(ParliamentHubAction.OPEN_DIVISION));
        assertTrue(openView.isEnabled(ParliamentHubAction.OPEN_DIVISION));

        ParliamentHubView closeView = new ParliamentHubView(
                NobleRank.SPEAKER,
                BillState.DIVISION_OPEN,
                true,
                false,
                false,
                true,
                false,
                Optional.of("Finance Act 2026"));

        assertTrue(closeView.visibleActions().contains(ParliamentHubAction.CLOSE_DIVISION));
    }

    @Test
    void closeDivisionBlockedWhenTiedWithoutCastingVote() {
        ParliamentHubView view = new ParliamentHubView(
                NobleRank.SPEAKER,
                BillState.DIVISION_OPEN,
                true,
                false,
                true,
                false,
                false,
                Optional.of("Supply Bill 2026"));

        assertTrue(view.closeDivisionBlocked());
        assertTrue(view.visibleActions().contains(ParliamentHubAction.CLOSE_DIVISION));
        assertFalse(view.isEnabled(ParliamentHubAction.CLOSE_DIVISION));
        assertTrue(view.visibleActions().contains(ParliamentHubAction.CAST_AYE));
        assertTrue(view.visibleActions().contains(ParliamentHubAction.CAST_NAY));
    }

    @Test
    void monarchSeesAssentInLords() {
        ParliamentHubView view = new ParliamentHubView(
                NobleRank.KING,
                BillState.AWAITING_ASSENT,
                false,
                true,
                false,
                false,
                false,
                Optional.of("Finance Act 2026"));

        Set<ParliamentHubAction> visible = view.visibleActions();

        assertTrue(visible.contains(ParliamentHubAction.ASSENT));
        assertTrue(visible.contains(ParliamentHubAction.REJECT));
        assertTrue(view.isEnabled(ParliamentHubAction.ASSENT));
        assertTrue(view.isEnabled(ParliamentHubAction.REJECT));
    }

    @Test
    void mpSeesVoteButtonsDuringOpenDivision() {
        ParliamentHubView view = new ParliamentHubView(
                NobleRank.MP,
                BillState.DIVISION_OPEN,
                true,
                false,
                false,
                false,
                false,
                Optional.of("Budget Bill 2026"));

        Set<ParliamentHubAction> visible = view.visibleActions();

        assertTrue(visible.contains(ParliamentHubAction.VOTE_AYE));
        assertTrue(visible.contains(ParliamentHubAction.VOTE_NAY));
        assertTrue(visible.contains(ParliamentHubAction.VOTE_ABSTAIN));
        assertTrue(view.isEnabled(ParliamentHubAction.VOTE_AYE));
    }
}
