package dev.leo.kingdom.economy.villager;

import dev.leo.kingdom.economy.income.EconomyConfig;
import dev.leo.kingdom.economy.income.VillagerGdpCalculator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class VillagerTradeService {

    public List<VillagerTradeSettlement> planTrades(
            List<VillagerTradeEdge> edges,
            List<VillagerEconomicParticipant> participants,
            EconomyConfig config,
            double commerceTaxRate,
            Random random) {
        Map<String, List<VillagerEconomicParticipant>> byProfession = groupByProfession(participants);
        List<VillagerTradeSettlement> settlements = new ArrayList<>();

        for (VillagerTradeEdge edge : edges) {
            if (!byProfession.containsKey(edge.sellerProfession())) {
                continue;
            }
            List<VillagerEconomicParticipant> buyers = byProfession.get(edge.buyerProfession());
            List<VillagerEconomicParticipant> sellers = byProfession.get(edge.sellerProfession());
            if (buyers == null || buyers.isEmpty() || sellers == null || sellers.isEmpty()) {
                continue;
            }

            VillagerEconomicParticipant buyer = buyers.get(random.nextInt(buyers.size()));
            VillagerEconomicParticipant seller = sellers.get(random.nextInt(sellers.size()));
            double buyerDailyIncome = dailyIncomeFor(buyer, config);
            double payment = buyerDailyIncome * edge.spendPercent();
            if (payment <= 0.0) {
                continue;
            }
            double commerceTax = payment * commerceTaxRate;
            settlements.add(new VillagerTradeSettlement(edge, buyer.villagerId(), seller.villagerId(), payment, commerceTax));
        }

        return settlements;
    }

    private static Map<String, List<VillagerEconomicParticipant>> groupByProfession(
            List<VillagerEconomicParticipant> participants) {
        Map<String, List<VillagerEconomicParticipant>> grouped = new HashMap<>();
        for (VillagerEconomicParticipant participant : participants) {
            grouped.computeIfAbsent(participant.profession().toLowerCase(), ignored -> new ArrayList<>())
                    .add(participant);
        }
        return grouped;
    }

    private static double dailyIncomeFor(VillagerEconomicParticipant participant, EconomyConfig config) {
        return VillagerGdpCalculator.calculateDailyGdp(
                List.of(new dev.leo.kingdom.economy.income.VillagerContribution(
                        participant.profession(), participant.tierIndex())),
                config);
    }
}
