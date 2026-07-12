package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.ParliamentService.AssentedActDraft;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import java.util.Optional;
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
            case BillPayload.War war -> EconomyResult.fail(
                    "War bills carry no economic effect. Use ParliamentEnactment.enactWar.");
            case BillPayload.Peace peace -> EconomyResult.fail(
                    "Peace bills carry no economic effect. Use ParliamentEnactment.enactPeace.");
        };
    }

    /**
     * Enactment path used after royal assent (parliament GUI / command). Routes fiscal and supply
     * bills through the treasury, and war/peace bills through the war services.
     */
    public static AssentedEnactmentResult enactAssented(
            AssentedActDraft draft,
            EconomyService economyService,
            WarService warService,
            DemobilisationService demobilisationService,
            int maxMints) {
        return switch (draft.payload()) {
            case BillPayload.War ignored -> {
                if (warService == null) {
                    yield AssentedEnactmentResult.fail("War service is not available.");
                }
                WarResult warResult = enactWar(draft, warService);
                yield toAssentedResult(warResult);
            }
            case BillPayload.Peace ignored -> {
                if (warService == null || demobilisationService == null) {
                    yield AssentedEnactmentResult.fail("Peace demobilisation is not available.");
                }
                WarResult peaceResult = enactPeace(draft, warService, demobilisationService);
                yield toAssentedResult(peaceResult);
            }
            default -> {
                EconomyResult economyResult = enact(draft, economyService, maxMints);
                yield switch (economyResult) {
                    case EconomyResult.Success success -> AssentedEnactmentResult.ok(success.message());
                    case EconomyResult.Failure failure -> AssentedEnactmentResult.fail(failure.message());
                };
            }
        };
    }

    private static AssentedEnactmentResult toAssentedResult(WarResult result) {
        return switch (result) {
            case WarResult.Success success -> AssentedEnactmentResult.ok(success.message());
            case WarResult.Failure failure -> AssentedEnactmentResult.fail(failure.message());
        };
    }

    /**
     * War bills do not touch the treasury — enact them via the WarService instead of {@link #enact}.
     */
    public static WarResult enactWar(AssentedActDraft draft, WarService warService) {
        if (!(draft.payload() instanceof BillPayload.War war)) {
            return WarResult.fail("Bill is not a war bill.");
        }
        return warService.enactWarBill(draft.kingdomId(), war);
    }

    /**
     * Peace bills do not touch the treasury — enactment ends the named war and demobilises via
     * {@link DemobilisationService} instead of {@link #enact}.
     */
    public static WarResult enactPeace(
            AssentedActDraft draft, WarService warService, DemobilisationService demobilisationService) {
        if (!(draft.payload() instanceof BillPayload.Peace peace)) {
            return WarResult.fail("Bill is not a peace bill.");
        }
        Optional<ActiveWar> war = warService.findActiveWar(peace.warId());
        if (war.isEmpty()) {
            return WarResult.fail("No such active war.");
        }
        return demobilisationService.demobilise(war.get());
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
