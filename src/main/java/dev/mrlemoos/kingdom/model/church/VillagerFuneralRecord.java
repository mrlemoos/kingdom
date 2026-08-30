package dev.mrlemoos.kingdom.model.church;

/**
 * A dead productive villager's wallet, frozen awaiting rites. A funeral inside the window releases
 * the balance to the treasury less the tithe; silence lets it escheat whole.
 */
public record VillagerFuneralRecord(double heldBalance, long diedOnDay) {

    public VillagerFuneralRecord {
        heldBalance = Math.max(0.0d, heldBalance);
    }

    public boolean isExpired(long today, int windowDays) {
        return today - diedOnDay >= windowDays;
    }
}
