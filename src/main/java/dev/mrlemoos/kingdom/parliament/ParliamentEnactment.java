package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.wealth.EstateBlockPlacer;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.ParliamentService.AssentedActDraft;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.treaty.TreatyResult;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import java.util.Optional;
import java.util.UUID;

public final class ParliamentEnactment {

    private ParliamentEnactment() {}

    public static EconomyResult enact(AssentedActDraft draft, EconomyService economyService, int maxMints) {
        return enact(draft, economyService, maxMints, null);
    }

    public static EconomyResult enact(
            AssentedActDraft draft, EconomyService economyService, int maxMints, EstateBlockPlacer estatePlacer) {
        return switch (draft.payload()) {
            case BillPayload.Fiscal fiscal -> economyService.applyFiscalRates(draft.kingdomId(), fiscal.rates());
            case BillPayload.Budget budget -> economyService.enactBudget(draft.kingdomId(), budget.amount());
            case BillPayload.SpendMint mint -> economyService.placeMint(
                    draft.kingdomId(), mint.mintLocation(), mint.cost(), maxMints);
            case BillPayload.SpendPublicWork work -> economyService.placePublicWork(
                    draft.kingdomId(),
                    work.worldName(),
                    work.x(),
                    work.y(),
                    work.z(),
                    work.estateType(),
                    work.cost(),
                    estatePlacer);
            case BillPayload.SpendStipend stipend -> enactStipend(
                    economyService, draft.kingdomId(), stipend.recipientId(), stipend.amount());
            case BillPayload.War war -> EconomyResult.fail(
                    "War bills carry no economic effect. Use ParliamentEnactment.enactWar.");
            case BillPayload.Peace peace -> EconomyResult.fail(
                    "Peace bills carry no economic effect. Use ParliamentEnactment.enactPeace.");
            case BillPayload.Treaty treaty -> EconomyResult.fail(
                    "Treaty bills carry no economic effect. Use ParliamentEnactment.enactAssented.");
            case BillPayload.NoConfidence motion -> EconomyResult.fail(
                    "A motion of no confidence is decided in the Commons and enacts nothing.");
            case BillPayload.Referendum referendum -> EconomyResult.fail(
                    "A referendum is advisory: the realm's answer enacts nothing.");
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
        return enactAssented(draft, economyService, warService, demobilisationService, maxMints, null);
    }

    public static AssentedEnactmentResult enactAssented(
            AssentedActDraft draft,
            EconomyService economyService,
            WarService warService,
            DemobilisationService demobilisationService,
            int maxMints,
            EstateBlockPlacer estatePlacer) {
        return enactAssented(draft, economyService, warService, demobilisationService, null, maxMints, estatePlacer);
    }

    public static AssentedEnactmentResult enactAssented(
            AssentedActDraft draft,
            EconomyService economyService,
            WarService warService,
            DemobilisationService demobilisationService,
            TreatyService treatyService,
            int maxMints,
            EstateBlockPlacer estatePlacer) {
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
            case BillPayload.Treaty treaty -> {
                if (treatyService == null) {
                    yield AssentedEnactmentResult.fail("Treaty service is not available.");
                }
                TreatyResult result = treaty.repeal()
                        ? treatyService.repeal(draft.kingdomId(), treaty.counterpartKingdomId(), treaty.kind())
                        : treatyService.assent(draft.kingdomId(), treaty.counterpartKingdomId(), treaty.kind());
                yield switch (result) {
                    case TreatyResult.Success success -> AssentedEnactmentResult.ok(success.message());
                    case TreatyResult.Failure failure -> AssentedEnactmentResult.fail(failure.message());
                };
            }
            default -> {
                EconomyResult economyResult = enact(draft, economyService, maxMints, estatePlacer);
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
