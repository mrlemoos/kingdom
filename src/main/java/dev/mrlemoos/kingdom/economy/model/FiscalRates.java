package dev.mrlemoos.kingdom.economy.model;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.EnumMap;
import java.util.Map;

public record FiscalRates(
        double baseRate,
        double foreignSurcharge,
        double transferFee,
        double crossKingdomTransferFee,
        double villagerWalletInterest,
        double tariff,
        Map<NobleRank, Double> rankModifiers,
        double overdraftInterest,
        double groundRent) {

    public FiscalRates {
        if (!Double.isFinite(overdraftInterest) || overdraftInterest < 0.0 || overdraftInterest > 0.01
                || !Double.isFinite(groundRent) || groundRent < 0.0 || groundRent > 0.01) {
            throw new IllegalArgumentException("Debt interest and ground rent must be between 0% and 1%.");
        }
        rankModifiers = Map.copyOf(rankModifiers);
    }

    public FiscalRates(double baseRate, double foreignSurcharge, double transferFee,
            double crossKingdomTransferFee, double villagerWalletInterest, double tariff,
            Map<NobleRank, Double> rankModifiers) {
        this(baseRate, foreignSurcharge, transferFee, crossKingdomTransferFee,
                villagerWalletInterest, tariff, rankModifiers, 0.001, 0.001);
    }

    public static FiscalRates defaults() {
        Map<NobleRank, Double> modifiers = new EnumMap<>(NobleRank.class);
        modifiers.put(NobleRank.KING, -0.05);
        modifiers.put(NobleRank.QUEEN, -0.05);
        modifiers.put(NobleRank.PRINCE, -0.03);
        modifiers.put(NobleRank.PREMIER, -0.02);
        modifiers.put(NobleRank.KNIGHT, 0.02);
        return new FiscalRates(0.10, 0.05, 0.03, 0.08, 0.0, 0.0, modifiers);
    }

    public double rankModifier(NobleRank rank) {
        if (rank == null) {
            return 0.0;
        }
        return rankModifiers.getOrDefault(rank, 0.0);
    }
}
