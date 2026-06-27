package dev.leo.kingdom.economy.villager;

import dev.leo.kingdom.economy.income.EconomyConfig;
import dev.leo.kingdom.economy.income.VillagerContribution;
import dev.leo.kingdom.economy.income.VillagerGdpCalculator;
import dev.leo.kingdom.economy.service.EconomyService;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VillagerEconomyProcessor {

    private final VillagerTradeService tradeService = new VillagerTradeService();

    public VillagerEconomyDayResult processKingdomDay(
            String kingdomId,
            List<VillagerEconomicParticipant> participants,
            EconomyService economyService,
            EconomyConfig economyConfig,
            VillagerEconomyConfig villagerConfig,
            long epochDay,
            Random random) {
        double totalGdp = 0.0;
        for (VillagerEconomicParticipant participant : participants) {
            double gross = dailyIncomeFor(participant, economyConfig);
            if (gross > 0.0) {
                economyService.creditVillagerGdp(kingdomId, participant.villagerId(), gross);
                totalGdp += gross;
            }
        }
        economyService.setLastDailyGdp(kingdomId, totalGdp);

        Set<UUID> activeIds = participants.stream()
                .map(VillagerEconomicParticipant::villagerId)
                .collect(Collectors.toCollection(HashSet::new));
        economyService.syncVillagerWalletActivity(kingdomId, activeIds, epochDay);

        List<VillagerTradeSettlement> plannedTrades = tradeService.planTrades(
                villagerConfig.tradeEdges(),
                participants,
                economyConfig,
                villagerConfig.villagerCommerceTaxRate(),
                random);
        int settledTrades = 0;
        for (VillagerTradeSettlement trade : plannedTrades) {
            if (economyService.settleVillagerTrade(
                    kingdomId,
                    trade.buyerId(),
                    trade.sellerId(),
                    trade.payment(),
                    villagerConfig.villagerCommerceTaxRate())) {
                settledTrades++;
            }
        }
        economyService.setLastDayTradesSettled(kingdomId, settledTrades);
        economyService.escheatFrozenWallets(
                kingdomId, epochDay, villagerConfig.frozenWalletEscheatMcDays());

        return new VillagerEconomyDayResult(totalGdp, settledTrades);
    }

    private static double dailyIncomeFor(VillagerEconomicParticipant participant, EconomyConfig config) {
        return VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution(participant.profession(), participant.tierIndex())), config);
    }
}
