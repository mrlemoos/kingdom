package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import org.junit.jupiter.api.Test;

class ProfessionVoteBiasTest {

    private final ProfessionVoteBias bias = ProfessionVoteBias.defaults();

    @Test
    void farmerLeansNayOnFiscal() {
        assertEquals(VoteChoice.NAY, bias.resolve(BillType.FISCAL, "farmer"));
    }

    @Test
    void librarianLeansAyeOnFiscal() {
        assertEquals(VoteChoice.AYE, bias.resolve(BillType.FISCAL, "librarian"));
    }

    @Test
    void unknownProfessionAbstains() {
        assertEquals(VoteChoice.ABSTAIN, bias.resolve(BillType.BUDGET, "fletcher"));
    }
}
