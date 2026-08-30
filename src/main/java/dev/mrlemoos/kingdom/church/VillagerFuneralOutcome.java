package dev.mrlemoos.kingdom.church;

/**
 * What a villager's funeral released: the ruling, the treasury's share, and the tithe — which goes
 * to the priest's own wallet, or to the treasury when the cleric presided.
 */
public record VillagerFuneralOutcome(
        ChurchResult result, double treasuryShare, double tithe, boolean titheToTreasury) {

    public static VillagerFuneralOutcome refused(String message) {
        return new VillagerFuneralOutcome(ChurchResult.fail(message), 0.0d, 0.0d, true);
    }
}
