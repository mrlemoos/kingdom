package dev.mrlemoos.kingdom.economy.model;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.EnumMap;
import java.util.Map;

public record FiscalRates(
        double baseRate,
        double foreignSurcharge,
        double transferFee,
        double crossKingdomTransferFee,
        Map<NobleRank, Double> rankModifiers) {

    public FiscalRates {
        rankModifiers = Map.copyOf(rankModifiers);
    }

    public static FiscalRates defaults() {
        Map<NobleRank, Double> modifiers = new EnumMap<>(NobleRank.class);
        modifiers.put(NobleRank.KING, -0.05);
        modifiers.put(NobleRank.QUEEN, -0.05);
        modifiers.put(NobleRank.PRINCE, -0.03);
        modifiers.put(NobleRank.PREMIER, -0.02);
        modifiers.put(NobleRank.KNIGHT, 0.02);
        return new FiscalRates(0.10, 0.05, 0.03, 0.08, modifiers);
    }

    public double rankModifier(NobleRank rank) {
        if (rank == null) {
            return 0.0;
        }
        return rankModifiers.getOrDefault(rank, 0.0);
    }
}
