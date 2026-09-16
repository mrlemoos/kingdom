package dev.mrlemoos.kingdom.economy.villager;

public record VillagerEconomyDayResult(double totalGdpCredited, int tradesSettled, double incomeTaxCollected) {

    public VillagerEconomyDayResult(double totalGdpCredited, int tradesSettled) {
        this(totalGdpCredited, tradesSettled, 0.0);
    }
}
