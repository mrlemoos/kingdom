package dev.leo.kingdom.parliament;

import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.economy.service.EconomyResult;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.model.parliament.BillPayload;
import dev.leo.kingdom.service.ParliamentService.AssentedActDraft;
import java.util.UUID;

public final class ParliamentEnactment {

    private ParliamentEnactment() {}

    public static EconomyResult enact(AssentedActDraft draft, EconomyService economyService, int maxMints) {
        return switch (draft.payload()) {
            case BillPayload.Fiscal fiscal -> economyService.applyFiscalRates(draft.kingdomId(), fiscal.rates());
            case BillPayload.Budget budget -> economyService.enactBudget(draft.kingdomId(), budget.amount());
            case BillPayload.SpendMint mint -> economyService.placeMint(
                    draft.kingdomId(), mint.mintLocation(), mint.cost(), maxMints);
            case BillPayload.SpendStipend stipend -> enactStipend(
                    economyService, draft.kingdomId(), stipend.recipientId(), stipend.amount());
        };
    }

    private static EconomyResult enactStipend(
            EconomyService economyService, String kingdomId, UUID recipientId, double amount) {
        EconomyResult spend = economyService.spendFromBudget(kingdomId, amount);
        if (spend instanceof EconomyResult.Failure failure) {
            return failure;
        }
        economyService.creditWalletDirect(recipientId, amount);
        return EconomyResult.ok("Treasury stipend enacted.");
    }
}
