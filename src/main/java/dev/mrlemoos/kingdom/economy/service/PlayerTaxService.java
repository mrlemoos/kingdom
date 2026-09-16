package dev.mrlemoos.kingdom.economy.service;

import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Collects each subject's equal share of the income tax already paid by the realm's villagers. */
public final class PlayerTaxService {

    private final EconomyService economyService;
    private LoyaltyService loyaltyService;
    private LongSupplier currentMcDay;

    public PlayerTaxService(EconomyService economyService) {
        this.economyService = Objects.requireNonNull(economyService, "economyService");
    }

    public void setServiceCreditHook(LoyaltyService loyaltyService, LongSupplier currentMcDay) {
        this.loyaltyService = loyaltyService;
        this.currentMcDay = currentMcDay;
    }

    public PlayerTaxResult settle(String kingdomId, Collection<UUID> memberIds, double villagerIncomeTaxYield) {
        if (kingdomId == null || kingdomId.isBlank() || memberIds == null || memberIds.isEmpty()
                || !Double.isFinite(villagerIncomeTaxYield) || villagerIncomeTaxYield <= 0.0) {
            return new PlayerTaxResult(0.0, 0.0, 0.0, Map.of());
        }
        Collection<UUID> members = new LinkedHashSet<>(memberIds);
        members.remove(null);
        if (members.isEmpty()) {
            return new PlayerTaxResult(0.0, 0.0, 0.0, Map.of());
        }
        double share = villagerIncomeTaxYield / members.size();
        double collected = 0.0;
        Map<UUID, PlayerTaxResult.Payment> payments = new LinkedHashMap<>();
        for (UUID playerId : members) {
            double paid = Math.min(share, economyService.getWalletBalance(playerId));
            if (paid > 0.0) {
                economyService.debitWallet(playerId, paid);
                collected += paid;
                if (loyaltyService != null && currentMcDay != null) {
                    loyaltyService.recordServiceCredit(playerId, currentMcDay.getAsLong());
                }
            }
            payments.put(playerId, new PlayerTaxResult.Payment(paid, share - paid));
        }
        if (collected > 0.0) {
            economyService.creditTreasuryFromPlayerTax(kingdomId, collected);
        }
        return new PlayerTaxResult(share, collected, share * members.size() - collected, payments);
    }
}
