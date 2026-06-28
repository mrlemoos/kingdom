package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class VillagerPremierInauguralService {

    private final KingdomService kingdomService;
    private final EconomyService economyService;
    private final ElectionService electionService;
    private final ParliamentService parliamentService;
    private final ProfessionVoteBias professionVoteBias;
    private final int inauguralFiscalDelayMcDays;

    public VillagerPremierInauguralService(
            KingdomService kingdomService,
            EconomyService economyService,
            ElectionService electionService,
            ParliamentService parliamentService,
            ProfessionVoteBias professionVoteBias,
            ElectionConfig electionConfig) {
        this.kingdomService = kingdomService;
        this.economyService = economyService;
        this.electionService = electionService;
        this.parliamentService = parliamentService;
        this.professionVoteBias = professionVoteBias != null ? professionVoteBias : ProfessionVoteBias.defaults();
        this.inauguralFiscalDelayMcDays = electionConfig != null
                ? electionConfig.inauguralFiscalDelayMcDays()
                : ElectionConfig.defaults().inauguralFiscalDelayMcDays();
    }

    public ElectionResult appointAfterGeneralElection(String kingdomId, Map<String, Integer> professionCounts) {
        ElectionResult appointed = electionService.appointVillagerPremier(kingdomId, professionCounts);
        if (!(appointed instanceof ElectionResult.Success success)) {
            return appointed;
        }
        kingdomService.getKingdom(kingdomId).orElseThrow().getElectionState().setPendingInauguralFiscal(true);
        return ElectionResult.ok(success.message()
                + " The inaugural fiscal package will be tabled in "
                + inauguralFiscalDelayMcDays
                + " in-game days.");
    }

    public Optional<ElectionResult> tryBeginDueInauguralFiscal(String kingdomId, long currentMcDay) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        KingdomElectionState electionState = kingdom.get().getElectionState();
        if (!electionState.pendingInauguralFiscal()) {
            return Optional.empty();
        }
        if (electionState.premierVillagerSeatIndex().isEmpty()) {
            electionState.setPendingInauguralFiscal(false);
            return Optional.empty();
        }
        if (currentMcDay < electionState.lastGeneralElectionMcDay() + inauguralFiscalDelayMcDays) {
            return Optional.empty();
        }

        ElectionResult result = beginInauguralFiscal(kingdomId);
        if (result instanceof ElectionResult.Success) {
            electionState.setPendingInauguralFiscal(false);
        }
        return Optional.of(result);
    }

    public ElectionResult beginInauguralFiscal(String kingdomId) {
        OptionalInt premierSeat = kingdomService.getKingdom(kingdomId)
                .map(Kingdom::getElectionState)
                .map(KingdomElectionState::premierVillagerSeatIndex)
                .orElse(OptionalInt.empty());
        if (premierSeat.isEmpty()) {
            return ElectionResult.fail("No Premier villager is appointed.");
        }

        int seatIndex = premierSeat.getAsInt();
        KingdomElectionState electionState = kingdomService.getKingdom(kingdomId).orElseThrow().getElectionState();
        String profession = electionState.seat(seatIndex)
                .flatMap(seat -> seat.profession())
                .orElse("none");

        FiscalRates enacted = economyService.kingdomEconomies().containsKey(kingdomId)
                ? economyService.kingdomEconomies().get(kingdomId).activeRates()
                : FiscalRates.defaults();
        FiscalRates manifesto = VillagerPremierFiscalManifesto.proposeForProfession(
                enacted, profession, professionVoteBias);

        ParliamentResult tabled = parliamentService.tableFiscalForVillagerPremier(
                kingdomId, seatIndex, manifesto, "Inaugural Finance Act");
        if (tabled instanceof ParliamentResult.Failure failure) {
            return ElectionResult.fail(failure.message());
        }

        ParliamentResult division = parliamentService.runRealmHandledDivision(kingdomId, seatIndex);
        if (division instanceof ParliamentResult.Failure failure) {
            return ElectionResult.fail(failure.message());
        }

        electionState.setPendingInauguralBudget(true);
        return ElectionResult.ok("Inaugural fiscal bill tabled and sent to the Lords for royal assent.");
    }

    public void tablePendingBudgetAfterAssent(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            KingdomElectionState electionState = kingdom.getElectionState();
            if (!electionState.pendingInauguralBudget()) {
                return;
            }
            OptionalInt premierSeat = electionState.premierVillagerSeatIndex();
            if (premierSeat.isEmpty()) {
                electionState.setPendingInauguralBudget(false);
                return;
            }

            double budgetCap = VillagerPremierBudgetCap.fromTreasury(economyService.getTreasuryBalance(kingdomId));
            int seatIndex = premierSeat.getAsInt();

            ParliamentResult tabled = parliamentService.tableBudgetForVillagerPremier(
                    kingdomId, seatIndex, budgetCap, "Inaugural Budget Act");
            if (tabled instanceof ParliamentResult.Success) {
                parliamentService.runRealmHandledDivision(kingdomId, seatIndex);
            }
            electionState.setPendingInauguralBudget(false);
        });
    }

    public void clearPendingBudgetOnBillFailure(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            kingdom.getElectionState().setPendingInauguralBudget(false);
        });
    }
}
