package dev.mrlemoos.kingdom.economy.villager;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.election.TerritoryVillagerCommercePolicy;
import java.util.Objects;

public final class EmeraldVillagerTradeService {

    private final EmeraldVillagerTradeCalculator calculator;
    private final double commerceTaxRate;

    public EmeraldVillagerTradeService(EmeraldVillagerTradeCalculator calculator, double commerceTaxRate) {
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.commerceTaxRate = commerceTaxRate;
    }

    public boolean settle(EconomyService economyService, EmeraldVillagerTradeRequest request) {
        Objects.requireNonNull(economyService, "economyService");
        Objects.requireNonNull(request, "request");
        if (!TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                request.kingdomId(),
                request.treasuryLord(),
                request.seatedMp(),
                request.kingdomTaggedMp())) {
            return false;
        }

        EmeraldVillagerTradeSettlement settlement = calculator.settlement(request.emeraldCost(), commerceTaxRate);
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
        if (!TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                request.kingdomId(),
                request.treasuryLord(),
                request.seatedMp(),
                request.kingdomTaggedMp())) {
            return false;
        }

        EmeraldVillagerTradeSettlement settlement = calculator.settlementFromGross(coronaPrice, commerceTaxRate);
        if (settlement.grossCorona() <= 0.0) {
            return false;
        }

        return economyService.creditEmeraldVillagerCommerce(
                request.kingdomId().orElseThrow(),
                request.villagerId(),
                settlement);
    }
}
