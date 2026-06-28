package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.EnumMap;
import java.util.Map;

public final class VillagerPremierFiscalManifesto {

    private static final double POINT = 0.01;

    private VillagerPremierFiscalManifesto() {}

    public static FiscalRates proposeForProfession(
            FiscalRates enacted, String profession, ProfessionVoteBias bias) {
        VoteChoice choice = bias.resolve(BillType.FISCAL, profession);
        return propose(enacted, choice);
    }

    public static FiscalRates propose(FiscalRates enacted, VoteChoice bias) {
        if (enacted == null) {
            return FiscalRates.defaults();
        }
        if (bias == null || bias == VoteChoice.ABSTAIN) {
            return enacted;
        }
        double delta = bias == VoteChoice.AYE ? -POINT : POINT;
        Map<NobleRank, Double> adjustedModifiers = new EnumMap<>(NobleRank.class);
        for (Map.Entry<NobleRank, Double> entry : enacted.rankModifiers().entrySet()) {
            adjustedModifiers.put(entry.getKey(), entry.getValue() + delta);
        }
        return new FiscalRates(
                enacted.baseRate() + delta,
                enacted.foreignSurcharge() + delta,
                enacted.transferFee() + delta,
                enacted.crossKingdomTransferFee() + delta,
                adjustedModifiers);
    }
}
