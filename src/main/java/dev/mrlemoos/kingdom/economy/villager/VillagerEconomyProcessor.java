package dev.mrlemoos.kingdom.economy.villager;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.income.VillagerContribution;
import dev.mrlemoos.kingdom.economy.income.VillagerGdpCalculator;
import dev.mrlemoos.kingdom.economy.model.CreditResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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
        return processKingdomDay(
                kingdomId,
                participants,
                economyService,
                economyConfig,
                villagerConfig,
                epochDay,
                random,
                SeasonProfile.defaults(Season.SPRING));
    }

    /** The day's account as the season in force asks it: outdoor trades swing, indoor ones hold. */
    public VillagerEconomyDayResult processKingdomDay(
            String kingdomId,
            List<VillagerEconomicParticipant> participants,
            EconomyService economyService,
            EconomyConfig economyConfig,
            VillagerEconomyConfig villagerConfig,
            long epochDay,
            Random random,
            SeasonProfile season) {
        return processKingdomDay(
                kingdomId, participants, economyService, economyConfig, villagerConfig, epochDay, random, season, null);
    }

    /**
     * As above, with the day's levy upkeep charged on the treasury once the day's yield and its
     * taxes are in and before the escheat of frozen wallets — the only point in the day's account
     * where a spend can run the treasury dry without robbing the villagers of their own takings.
     */
    public VillagerEconomyDayResult processKingdomDay(
            String kingdomId,
            List<VillagerEconomicParticipant> participants,
            EconomyService economyService,
            EconomyConfig economyConfig,
            VillagerEconomyConfig villagerConfig,
            long epochDay,
            Random random,
            SeasonProfile season,
            Consumer<String> levyUpkeepCharge) {
        return processKingdomDay(
                kingdomId,
                participants,
                economyService,
                economyConfig,
                villagerConfig,
                epochDay,
                random,
                season,
                levyUpkeepCharge,
                Map.of());
    }

    /**
     * As above, with each villager's own share of the day's yield scaled by what the winter has cost
     * it: a villager left beyond the reach of a burning hearth yields less than one kept warm.
     */
    public VillagerEconomyDayResult processKingdomDay(
            String kingdomId,
            List<VillagerEconomicParticipant> participants,
            EconomyService economyService,
            EconomyConfig economyConfig,
            VillagerEconomyConfig villagerConfig,
            long epochDay,
            Random random,
            SeasonProfile season,
            Consumer<String> levyUpkeepCharge,
            Map<UUID, Double> villagerYieldFactors) {
        Map<UUID, Double> yieldFactors = villagerYieldFactors == null ? Map.of() : villagerYieldFactors;
        double totalGdp = 0.0;
        double incomeTax = 0.0;
        for (VillagerEconomicParticipant participant : participants) {
            double gross = dailyIncomeFor(participant, economyConfig, season)
                    * yieldFactor(yieldFactors, participant.villagerId());
            if (gross > 0.0) {
                CreditResult credit = economyService.creditVillagerGdp(kingdomId, participant.villagerId(), gross);
                totalGdp += gross;
                incomeTax += credit.tax();
            }
        }
        economyService.setLastDailyGdp(kingdomId, totalGdp);

        Set<UUID> activeIds = new HashSet<>();
        for (VillagerEconomicParticipant participant : participants) {
            activeIds.add(participant.villagerId());
        }
        economyService.syncVillagerWalletActivity(kingdomId, activeIds, epochDay);

        List<VillagerTradeSettlement> plannedTrades = tradeService.planTrades(
                villagerConfig.tradeEdges(),
                participants,
                economyConfig,
                villagerConfig.villagerCommerceTaxRate(),
                villagerConfig.settlementsPerEdge(),
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
        economyService.applyVillagerWalletInterest(kingdomId);
        if (levyUpkeepCharge != null) {
            levyUpkeepCharge.accept(kingdomId);
        }
        economyService.escheatFrozenWallets(
                kingdomId, epochDay, villagerConfig.frozenWalletEscheatMcDays());

        return new VillagerEconomyDayResult(totalGdp, settledTrades, incomeTax);
    }

    /** What the villager keeps of the day's gross; the whole of it unless the cold has taken a share. */
    private static double yieldFactor(Map<UUID, Double> factors, UUID villagerId) {
        Double factor = factors.get(villagerId);
        return factor == null ? 1.0 : Math.max(0.0, factor.doubleValue());
    }

    private static double dailyIncomeFor(
            VillagerEconomicParticipant participant, EconomyConfig config, SeasonProfile season) {
        return VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution(participant.profession(), participant.tierIndex())), config, season);
    }
}
