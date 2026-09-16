package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ProfessionVoteBiasTest {

    private static final List<String> VANILLA_PROFESSIONS = List.of(
            "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith",
            ProfessionConstituencyResolver.CITIZEN_PROFESSION);

    private static final List<BillType> DIVIDED_TYPES = List.of(
            BillType.FISCAL, BillType.BUDGET, BillType.SPEND_MINT, BillType.SPEND_STIPEND,
            BillType.SPEND_PUBLIC_WORK, BillType.WAR, BillType.PEACE, BillType.TREATY);

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
    void everyBenchTakesASideOnEveryDividedBill() {
        for (BillType type : DIVIDED_TYPES) {
            for (String profession : VANILLA_PROFESSIONS) {
                assertNotEquals(
                        VoteChoice.ABSTAIN, bias.resolve(type, profession), type + "/" + profession);
            }
        }
    }

    @Test
    void confidenceInThePremierIsLeftToTheElectedBenches() {
        assertEquals(VoteChoice.ABSTAIN, bias.resolve(BillType.NO_CONFIDENCE, "farmer"));
    }

    @Test
    void unknownProfessionAbstains() {
        assertEquals(VoteChoice.ABSTAIN, bias.resolve(BillType.BUDGET, "astronaut"));
    }

    @Test
    void configuredStancesOverlayTheDefaultsRatherThanReplacingThem() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("election.profession-vote-bias.FISCAL.farmer", "aye");

        ProfessionVoteBias configured = ProfessionVoteBias.fromPluginConfig(config);

        assertEquals(VoteChoice.AYE, configured.resolve(BillType.FISCAL, "farmer"));
        // A partial config must not silently mute every bench it omits.
        assertEquals(VoteChoice.AYE, configured.resolve(BillType.FISCAL, "mason"));
        assertEquals(VoteChoice.AYE, configured.resolve(BillType.BUDGET, "fletcher"));
    }
}
