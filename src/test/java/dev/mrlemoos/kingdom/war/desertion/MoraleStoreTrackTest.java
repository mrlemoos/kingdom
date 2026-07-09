package dev.mrlemoos.kingdom.war.desertion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The {@link MoraleTrack} adapter over Slice 4.1's persisted {@code MoraleStore}, exercising the
 * standalone military-track breach table (see {@link MoraleBreachKind}) that {@link
 * DesertionEvaluator} delegates to. Dual-track and treason-review behaviour is covered in
 * {@link DesertionEvaluatorTest}.
 */
class MoraleStoreTrackTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private MoraleStoreTrack track;

    @BeforeEach
    void setUp() {
        track = new MoraleStoreTrack(new InMemoryMoraleStore(), MoraleConfig.enabled());
    }

    @Test
    void defaultTierIsSteadfast() {
        assertEquals(MoraleTier.STEADFAST, track.tierOf(PLAYER));
    }

    @Test
    void dropToNeverImprovesTier() {
        track.dropTo(PLAYER, MoraleTier.BREAKING);

        track.dropTo(PLAYER, MoraleTier.SHAKEN);

        assertEquals(MoraleTier.BREAKING, track.tierOf(PLAYER));
    }

    @Test
    void applyBreachRefuseMusterDropsToShaken() {
        MoraleTier result = track.applyBreach(PLAYER, MoraleBreachKind.REFUSE_MUSTER, false);

        assertEquals(MoraleTier.SHAKEN, result);
    }

    @Test
    void applyBreachLeaveSiegeWithoutReleaseDropsOneStepStandard() {
        MoraleTier result = track.applyBreach(PLAYER, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, false);

        assertEquals(MoraleTier.SHAKEN, result);
    }

    @Test
    void applyBreachLeaveSiegeWithoutReleaseIsImmediateBreakingUnderHardenedService() {
        MoraleTier result = track.applyBreach(PLAYER, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, true);

        assertEquals(MoraleTier.BREAKING, result);
    }

    @Test
    void applyBreachFightingForEnemyDropsStraightToRout() {
        MoraleTier result = track.applyBreach(PLAYER, MoraleBreachKind.FIGHTING_FOR_ENEMY, false);

        assertEquals(MoraleTier.ROUT, result);
    }

    @Test
    void applyBreachDefectionDropsStraightToRout() {
        MoraleTier result = track.applyBreach(PLAYER, MoraleBreachKind.DEFECTION, false);

        assertEquals(MoraleTier.ROUT, result);
    }

    @Test
    void disabledConfigLeavesTierUnchanged() {
        MoraleStoreTrack disabled = new MoraleStoreTrack(new InMemoryMoraleStore(), MoraleConfig.disabled());

        disabled.applyBreach(PLAYER, MoraleBreachKind.FIGHTING_FOR_ENEMY, false);

        assertEquals(MoraleTier.STEADFAST, disabled.tierOf(PLAYER));
    }
}
