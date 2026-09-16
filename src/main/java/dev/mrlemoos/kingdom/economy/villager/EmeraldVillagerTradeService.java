package dev.mrlemoos.kingdom.economy.villager;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.election.TerritoryVillagerCommercePolicy;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import java.util.Objects;

public final class EmeraldVillagerTradeService {

    private final EmeraldVillagerTradeCalculator calculator;
    private final double commerceTaxRate;
    private TreatyService treatyService;

    public EmeraldVillagerTradeService(EmeraldVillagerTradeCalculator calculator, double commerceTaxRate) {
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.commerceTaxRate = commerceTaxRate;
    }

    public void setTreatyService(TreatyService treatyService) {
        this.treatyService = treatyService;
    }

    public boolean settle(EconomyService economyService, EmeraldVillagerTradeRequest request) {
        Objects.requireNonNull(economyService, "economyService");
        Objects.requireNonNull(request, "request");
        if (request.onStrike()) {
            return false;
        }
        if (!TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                request.kingdomId(),
                request.treasuryLord(),
                request.seatedMp(),
                request.kingdomTaggedMp())) {
            return false;
        }

        EmeraldVillagerTradeSettlement settlement = calculator.settlementFromGross(
                calculator.coronaEquivalent(request.emeraldCost()),
                commerceTaxRate,
                tariffFor(economyService, request),
                request.territoryMember());
        if (settlement.grossCorona() <= 0.0) {
            return false;
        }

        return economyService.creditEmeraldVillagerCommerce(
                request.kingdomId().orElseThrow(),
                request.villagerId(),
                settlement);
    }

    public boolean settleCoronaMerchant(
            EconomyService economyService, EmeraldVillagerTradeRequest request, int coronaPrice) {
        Objects.requireNonNull(economyService, "economyService");
        Objects.requireNonNull(request, "request");
        if (request.onStrike()) {
            return false;
        }
        if (!TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                request.kingdomId(),
                request.treasuryLord(),
                request.seatedMp(),
                request.kingdomTaggedMp())) {
            return false;
        }

        EmeraldVillagerTradeSettlement settlement = calculator.settlementFromGross(
                coronaPrice,
                commerceTaxRate,
                tariffFor(economyService, request),
                request.territoryMember());
        if (settlement.grossCorona() <= 0.0) {
            return false;
        }

        return economyService.creditEmeraldVillagerCommerce(
                request.kingdomId().orElseThrow(),
                request.villagerId(),
                settlement);
    }

    private double tariffFor(EconomyService economyService, EmeraldVillagerTradeRequest request) {
        if (request.territoryMember() || request.kingdomId().isEmpty()) {
            return 0.0;
        }
        if (treatyService != null
                && request.traderKingdomId().isPresent()
                && treatyService.waivesTariff(request.kingdomId().get(), request.traderKingdomId().get())) {
            return 0.0;
        }
        var economy = economyService.kingdomEconomies().get(request.kingdomId().get());
        return economy != null ? economy.activeRates().tariff() : 0.0;
    }
}
