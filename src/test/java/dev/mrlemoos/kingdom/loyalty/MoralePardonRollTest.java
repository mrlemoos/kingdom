package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Who the court may pardon, and who has nothing to pardon. */
class MoralePardonRollTest {

    private static final UUID STEADY = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SHAKEN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROUTED = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CLOSED = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID FOREIGNER = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private static Map<UUID, MoraleTier> tiers() {
        Map<UUID, MoraleTier> tiers = new LinkedHashMap<>();
        tiers.put(STEADY, MoraleTier.STEADFAST);
        tiers.put(SHAKEN, MoraleTier.SHAKEN);
        tiers.put(ROUTED, MoraleTier.ROUT);
        tiers.put(FOREIGNER, MoraleTier.ROUT);
        return tiers;
    }

    @Test
    void onlyDegradedSubjectsOfTheRealmStandOnTheRoll() {
        List<MoralePardonRoll.Subject> roll =
                MoralePardonRoll.of(List.of(STEADY, SHAKEN, ROUTED, CLOSED), tiers());

        assertEquals(List.of(ROUTED, SHAKEN), roll.stream().map(MoralePardonRoll.Subject::playerId).toList());
    }

    @Test
    void aClosedTrackIsLeftClosedSoAPardonNeverOpensOne() {
        List<MoralePardonRoll.Subject> roll = MoralePardonRoll.of(List.of(CLOSED), tiers());

        assertTrue(roll.isEmpty());
    }

    @Test
    void aSteadfastSubjectHasNothingToPardon() {
        List<MoralePardonRoll.Subject> roll = MoralePardonRoll.of(List.of(STEADY), tiers());

        assertTrue(roll.isEmpty());
    }

    @Test
    void aSubjectOfAnotherRealmNeverStandsOnTheRoll() {
        List<MoralePardonRoll.Subject> roll = MoralePardonRoll.of(List.of(SHAKEN), tiers());

        assertEquals(1, roll.size());
        assertEquals(SHAKEN, roll.get(0).playerId());
    }

    @Test
    void theWorstStandAtTheHeadOfTheRoll() {
        Map<UUID, MoraleTier> tiers = new LinkedHashMap<>();
        tiers.put(STEADY, MoraleTier.SHAKEN);
        tiers.put(SHAKEN, MoraleTier.BREAKING);
        tiers.put(ROUTED, MoraleTier.ROUT);

        List<MoralePardonRoll.Subject> roll = MoralePardonRoll.of(List.of(STEADY, SHAKEN, ROUTED), tiers);

        assertEquals(
                List.of(MoraleTier.ROUT, MoraleTier.BREAKING, MoraleTier.SHAKEN),
                roll.stream().map(MoralePardonRoll.Subject::tier).toList());
    }

    @Test
    void theRealmNamesEachTier() {
        assertEquals("Rout", MoralePardonRoll.display(MoraleTier.ROUT));
        assertEquals("Breaking", MoralePardonRoll.display(MoraleTier.BREAKING));
        assertEquals("Shaken", MoralePardonRoll.display(MoraleTier.SHAKEN));
        assertEquals("Steadfast", MoralePardonRoll.display(MoraleTier.STEADFAST));
    }
}
