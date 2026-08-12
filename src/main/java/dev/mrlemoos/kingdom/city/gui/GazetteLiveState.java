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
        double treasuryBalance) {

    public GazetteLiveState {
        openBillTitle = openBillTitle == null ? "" : openBillTitle;
        nextElectionLabel = nextElectionLabel == null ? "none proclaimed" : nextElectionLabel;
    }

    public List<String> lines() {
        return List.of(
                "Open bill: " + (openBillTitle.isBlank() ? "none" : openBillTitle),
                "Next election: " + nextElectionLabel,
                "Wanted: " + wantedCount,
                "Build permits: " + permitCount,
                "Treasury: " + formatTreasury(treasuryBalance) + " Corona");
    }

    private static String formatTreasury(double balance) {
        if (Math.rint(balance) == balance) {
            return Long.toString((long) balance);
        }
        return Double.toString(balance);
    }
}
