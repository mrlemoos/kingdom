package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.parliament.VoteChoice;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VillagerPremierFiscalManifestoTest {

    private static final FiscalRates ENACTED = new FiscalRates(
            0.10,
            0.05,
            0.03,
            0.08,
            rankModifiers(-0.02, -0.03));

    @Test
    void ayeProfessionLowersEveryRateFieldByOnePoint() {
        FiscalRates manifesto = VillagerPremierFiscalManifesto.propose(
                ENACTED, VoteChoice.AYE);

        assertEquals(0.09, manifesto.baseRate(), 1e-9);
        assertEquals(0.04, manifesto.foreignSurcharge(), 1e-9);
        assertEquals(0.02, manifesto.transferFee(), 1e-9);
        assertEquals(0.07, manifesto.crossKingdomTransferFee(), 1e-9);
        assertEquals(-0.03, manifesto.rankModifier(NobleRank.PREMIER), 1e-9);
        assertEquals(-0.04, manifesto.rankModifier(NobleRank.PRINCE), 1e-9);
    }

    @Test
    void nayProfessionRaisesEveryRateFieldByOnePoint() {
        FiscalRates manifesto = VillagerPremierFiscalManifesto.propose(
                ENACTED, VoteChoice.NAY);

        assertEquals(0.11, manifesto.baseRate(), 1e-9);
        assertEquals(0.06, manifesto.foreignSurcharge(), 1e-9);
        assertEquals(0.04, manifesto.transferFee(), 1e-9);
        assertEquals(0.09, manifesto.crossKingdomTransferFee(), 1e-9);
        assertEquals(-0.01, manifesto.rankModifier(NobleRank.PREMIER), 1e-9);
        assertEquals(-0.02, manifesto.rankModifier(NobleRank.PRINCE), 1e-9);
    }

    @Test
    void abstainLeavesEnactedRatesUnchanged() {
        FiscalRates manifesto = VillagerPremierFiscalManifesto.propose(
                ENACTED, VoteChoice.ABSTAIN);

        assertEquals(ENACTED.baseRate(), manifesto.baseRate(), 1e-9);
        assertEquals(ENACTED.foreignSurcharge(), manifesto.foreignSurcharge(), 1e-9);
        assertEquals(ENACTED.transferFee(), manifesto.transferFee(), 1e-9);
        assertEquals(ENACTED.crossKingdomTransferFee(), manifesto.crossKingdomTransferFee(), 1e-9);
        assertEquals(ENACTED.rankModifier(NobleRank.PREMIER), manifesto.rankModifier(NobleRank.PREMIER), 1e-9);
    }

    @Test
    void derivesBiasFromPremierProfession() {
        ProfessionVoteBias bias = ProfessionVoteBias.defaults();

        FiscalRates farmer = VillagerPremierFiscalManifesto.proposeForProfession(
                ENACTED, "farmer", bias);
        FiscalRates librarian = VillagerPremierFiscalManifesto.proposeForProfession(
                ENACTED, "librarian", bias);

        assertEquals(0.11, farmer.baseRate(), 1e-9);
        assertEquals(0.09, librarian.baseRate(), 1e-9);
    }

    private static Map<NobleRank, Double> rankModifiers(double premier, double prince) {
        Map<NobleRank, Double> modifiers = new EnumMap<>(NobleRank.class);
        modifiers.put(NobleRank.PREMIER, premier);
        modifiers.put(NobleRank.PRINCE, prince);
        return modifiers;
    }
}
