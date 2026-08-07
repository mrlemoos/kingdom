package dev.mrlemoos.kingdom.parliament.gui;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class ParliamentHubView {

    private final NobleRank rank;
    private final BillState billState;
    private final boolean inCommons;
    private final boolean inLords;
    private final boolean divisionTied;
    private final boolean castingVoteSet;
    private final boolean hasPreparedMint;
    private final boolean electionActive;
    private final boolean pendingResignation;
    private final boolean canResolveResignation;
    private final Optional<String> billTitle;
    private final Optional<String> resignationSummary;

    public ParliamentHubView(
            NobleRank rank,
            BillState billState,
            boolean inCommons,
            boolean inLords,
            boolean divisionTied,
            boolean castingVoteSet,
            boolean hasPreparedMint,
            Optional<String> billTitle) {
        this(
                rank,
                billState,
                inCommons,
                inLords,
                divisionTied,
                castingVoteSet,
                hasPreparedMint,
                false,
                false,
                false,
                billTitle,
                Optional.empty());
    }

    public ParliamentHubView(
            NobleRank rank,
            BillState billState,
            boolean inCommons,
            boolean inLords,
            boolean divisionTied,
            boolean castingVoteSet,
            boolean hasPreparedMint,
            boolean electionActive,
            boolean pendingResignation,
            boolean canResolveResignation,
            Optional<String> billTitle,
            Optional<String> resignationSummary) {
        this.rank = rank;
        this.billState = billState;
        this.inCommons = inCommons;
        this.inLords = inLords;
        this.divisionTied = divisionTied;
        this.castingVoteSet = castingVoteSet;
        this.hasPreparedMint = hasPreparedMint;
        this.electionActive = electionActive;
        this.pendingResignation = pendingResignation;
        this.canResolveResignation = canResolveResignation;
        this.billTitle = billTitle != null ? billTitle : Optional.empty();
        this.resignationSummary = resignationSummary != null ? resignationSummary : Optional.empty();
    }

    public NobleRank rank() {
        return rank;
    }

    public BillState billState() {
        return billState;
    }

    public boolean inCommons() {
        return inCommons;
    }

    public boolean inLords() {
        return inLords;
    }

    public boolean divisionTied() {
        return divisionTied;
    }

    public boolean castingVoteSet() {
        return castingVoteSet;
    }

    public boolean hasPreparedMint() {
        return hasPreparedMint;
    }

    public boolean electionActive() {
        return electionActive;
    }

    public boolean pendingResignation() {
        return pendingResignation;
    }

    public boolean canResolveResignation() {
        return canResolveResignation;
    }

    public Optional<String> billTitle() {
        return billTitle;
    }

    public Optional<String> resignationSummary() {
        return resignationSummary;
    }

    public boolean closeDivisionBlocked() {
        return billState == BillState.DIVISION_OPEN && divisionTied && !castingVoteSet;
    }

    public Set<ParliamentHubAction> visibleActions() {
        Set<ParliamentHubAction> actions = EnumSet.noneOf(ParliamentHubAction.class);

        if (inCommons) {
            if (rank == NobleRank.PREMIER && billState == null && !electionActive) {
                actions.add(ParliamentHubAction.TABLE_FISCAL);
                actions.add(ParliamentHubAction.TABLE_BUDGET);
                actions.add(ParliamentHubAction.TABLE_SPEND_MINT);
                actions.add(ParliamentHubAction.TABLE_SPEND_STIPEND);
                actions.add(ParliamentHubAction.STIPEND_OTHER);
                actions.add(ParliamentHubAction.BUDGET_PRESET);
                actions.add(ParliamentHubAction.CUSTOM_AMOUNT);
            }
            if (rank == NobleRank.SPEAKER) {
                if (billState == BillState.TABLED) {
                    actions.add(ParliamentHubAction.OPEN_DIVISION);
                }
                if (billState == BillState.DIVISION_OPEN) {
                    actions.add(ParliamentHubAction.CLOSE_DIVISION);
                    if (divisionTied && !castingVoteSet) {
                        actions.add(ParliamentHubAction.CAST_AYE);
                        actions.add(ParliamentHubAction.CAST_NAY);
                    }
                }
            }
            if (rank == NobleRank.MP && billState == BillState.DIVISION_OPEN) {
                actions.add(ParliamentHubAction.VOTE_AYE);
                actions.add(ParliamentHubAction.VOTE_NAY);
                actions.add(ParliamentHubAction.VOTE_ABSTAIN);
            }
        }

        if (inLords && isMonarch(rank) && billState == BillState.AWAITING_ASSENT) {
            actions.add(ParliamentHubAction.ASSENT);
            actions.add(ParliamentHubAction.REJECT);
        }

        if (inLords && canResolveResignation && pendingResignation) {
            actions.add(ParliamentHubAction.REVIEW_RESIGNATION);
        }

        return Set.copyOf(actions);
    }

    /** Explains an empty hub so the GUI is never a silent wall of panes. */
    public Optional<String> statusHint() {
        if (!visibleActions().isEmpty()) {
            return Optional.empty();
        }
        if (isMonarch(rank) && billState == BillState.AWAITING_ASSENT && !inLords) {
            return Optional.of("Stand in the House of Lords to grant royal assent");
        }
        if (rank == NobleRank.PREMIER && electionActive) {
            return Optional.of("Parliament is dissolved until the election closes");
        }
        if (billState == null) {
            return Optional.of("No bill is before Parliament");
        }
        return Optional.of("No actions for your rank while the bill is "
                + billState.name().toLowerCase(java.util.Locale.UK).replace('_', ' '));
    }

    public boolean isEnabled(ParliamentHubAction action) {
        if (!visibleActions().contains(action)) {
            return false;
        }
        return switch (action) {
            case TABLE_SPEND_MINT -> hasPreparedMint;
            case CLOSE_DIVISION -> !closeDivisionBlocked();
            case OPEN_DIVISION -> billState == BillState.TABLED;
            case CAST_AYE, CAST_NAY -> divisionTied && !castingVoteSet;
            case ASSENT, REJECT -> billState == BillState.AWAITING_ASSENT;
            case VOTE_AYE, VOTE_NAY, VOTE_ABSTAIN -> billState == BillState.DIVISION_OPEN;
            case REVIEW_RESIGNATION -> pendingResignation && canResolveResignation;
            default -> true;
        };
    }

    private static boolean isMonarch(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }
}
