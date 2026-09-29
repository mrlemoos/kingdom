package dev.mrlemoos.kingdom.city.gui;

import java.util.List;

/**
 * Live realm facts read when the Gazette opens. Never persisted — recomputed each open.
 *
 * @param openBillTitle empty when the order paper is clear
 * @param nextElectionLabel human line for the next polling day or "none proclaimed"
 * @param wantedCount active or pending warrants in the realm
 * @param permitCount build permits on the register
 * @param treasuryBalance treasury Corona
 */
public record GazetteLiveState(
        String openBillTitle,
        String nextElectionLabel,
        int wantedCount,
        int permitCount,
        double treasuryBalance,
        String treatyLine,
        List<String> financeLines) {

    public GazetteLiveState(String openBillTitle, String nextElectionLabel, int wantedCount, int permitCount, double treasuryBalance) {
        this(openBillTitle, nextElectionLabel, wantedCount, permitCount, treasuryBalance, "", List.of());
    }

    public GazetteLiveState(String openBillTitle, String nextElectionLabel, int wantedCount, int permitCount,
            double treasuryBalance, String treatyLine) {
        this(openBillTitle, nextElectionLabel, wantedCount, permitCount, treasuryBalance, treatyLine, List.of());
    }

    public GazetteLiveState {
        openBillTitle = openBillTitle == null ? "" : openBillTitle;
        nextElectionLabel = nextElectionLabel == null ? "none proclaimed" : nextElectionLabel;
        treatyLine = treatyLine == null ? "" : treatyLine;
        financeLines = financeLines == null ? List.of() : List.copyOf(financeLines);
    }

    public List<String> lines() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>(List.of(
                "Open bill: " + (openBillTitle.isBlank() ? "none" : openBillTitle),
                "Next election: " + nextElectionLabel,
                "Wanted: " + wantedCount,
                "Build permits: " + permitCount,
                "Treasury: " + formatTreasury(treasuryBalance) + " Corona",
                treatyLine).stream().filter(line -> !line.isBlank()).toList());
        lines.addAll(financeLines);
        return List.copyOf(lines);
    }

    private static String formatTreasury(double balance) {
        if (Math.rint(balance) == balance) {
            return Long.toString((long) balance);
        }
        return Double.toString(balance);
    }
}
