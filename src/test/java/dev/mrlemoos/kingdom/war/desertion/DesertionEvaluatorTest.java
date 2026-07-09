package dev.mrlemoos.kingdom.war.desertion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Desertion breach table: refusing muster, leaving an active siege without a siege release, and
 * fighting for the enemy (battlefield treason or defection). Only defection is a dual-track
 * offence — it also lowers political loyalty. Traitor is never set here; that is exclusively
 * {@link LoyaltyService#convictTreason}'s job once a treason review flag leads to conviction.
 */
class DesertionEvaluatorTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String WAR_ID = "war-1";

    private MoraleStoreTrack moraleTrack;
    private InMemoryTreasonReviewStore treasonReviewStore;
    private DesertionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        moraleTrack = new MoraleStoreTrack(new InMemoryMoraleStore(), MoraleConfig.enabled());
        treasonReviewStore = new InMemoryTreasonReviewStore();
        evaluator = new DesertionEvaluator(moraleTrack, treasonReviewStore, () -> 1_700_000_000_000L);
    }

    @Test
    void refuseMusterDropsMoraleToShaken() {
        DesertionResult result = evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.REFUSE_MUSTER, false);

        assertEquals(MoraleTier.STEADFAST, result.previousMoraleTier());
        assertEquals(MoraleTier.SHAKEN, result.moraleTier());
        assertFalse(result.treasonReviewRaised());
        assertNull(result.politicalTierAfter());
        assertFalse(treasonReviewStore.isFlagged(PLAYER));
    }

    @Test
    void refuseMusterNeverImprovesAnAlreadyWorseTier() {
        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.FIGHTING_FOR_ENEMY, false);

        DesertionResult result = evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.REFUSE_MUSTER, false);

        assertEquals(MoraleTier.ROUT, result.moraleTier());
    }

    @Test
    void leaveSiegeWithoutReleaseDropsOneStepUnderStandardService() {
        DesertionResult result =
                evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, false);

        assertEquals(MoraleTier.SHAKEN, result.moraleTier());
    }

    @Test
    void secondLeaveSiegeWithoutReleaseUnderStandardServiceReachesBreaking() {
        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, false);

        DesertionResult result =
                evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, false);

        assertEquals(MoraleTier.BREAKING, result.moraleTier());
    }

    @Test
    void leaveSiegeWithoutReleaseIsImmediateBreakingOnFirstLeaveUnderHardenedService() {
        DesertionResult result =
                evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, true);

        assertEquals(MoraleTier.STEADFAST, result.previousMoraleTier());
        assertEquals(MoraleTier.BREAKING, result.moraleTier());
    }

    @Test
    void fightingForEnemyDropsToRoutAndRaisesTreasonReviewFlagWithoutPoliticalChange() {
        DesertionResult result =
                evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.FIGHTING_FOR_ENEMY, false);

        assertEquals(MoraleTier.ROUT, result.moraleTier());
        assertTrue(result.treasonReviewRaised());
        assertNull(result.politicalTierAfter());
        assertFalse(result.isDualTrack());
        assertTrue(treasonReviewStore.isFlagged(PLAYER));
        assertEquals(MoraleBreachKind.FIGHTING_FOR_ENEMY, treasonReviewStore.findFlag(PLAYER).orElseThrow().kind());
    }

    @Test
    void defectionIsDualTrackDroppingMoraleToRoutAndPoliticalTowardsDisloyal() {
        InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
        LoyaltyService loyaltyService = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        evaluator.setLoyaltyService(loyaltyService);

        DesertionResult result = evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.DEFECTION, false);

        assertEquals(MoraleTier.ROUT, result.moraleTier());
        assertTrue(result.treasonReviewRaised());
        assertTrue(result.isDualTrack());
        assertEquals(LoyaltyTier.DISLOYAL, result.politicalTierAfter());
        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(PLAYER));
        assertTrue(treasonReviewStore.isFlagged(PLAYER));
    }

    @Test
    void defectionNeverSetsTraitorRegardlessOfStartingLoyaltyTier() {
        InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
        LoyaltyService loyaltyService = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        evaluator.setLoyaltyService(loyaltyService);
        loyaltyService.recordActBreach(PLAYER);
        loyaltyService.recordActBreach(PLAYER);
        loyaltyService.recordActBreach(PLAYER);

        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.DEFECTION, false);

        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(PLAYER));
        assertFalse(loyaltyService.tierOf(PLAYER) == LoyaltyTier.TRAITOR);
    }

    @Test
    void defectionWithoutLoyaltyServiceStillRoutsAndFlagsButNoPoliticalTierRecorded() {
        DesertionResult result = evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.DEFECTION, false);

        assertEquals(MoraleTier.ROUT, result.moraleTier());
        assertTrue(result.treasonReviewRaised());
        assertNull(result.politicalTierAfter());
    }

    @Test
    void onlyTreasonConvictionEverAppliesTraitorNotDesertion() {
        InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
        LoyaltyService loyaltyService = new LoyaltyService(loyaltyStore, LoyaltyConfig.enabled());
        evaluator.setLoyaltyService(loyaltyService);

        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.DEFECTION, false);
        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.DEFECTION, false);
        evaluator.evaluate(PLAYER, WAR_ID, MoraleBreachKind.FIGHTING_FOR_ENEMY, false);

        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(PLAYER));

        loyaltyService.convictTreason(PLAYER);

        assertEquals(LoyaltyTier.TRAITOR, loyaltyService.tierOf(PLAYER));
    }
}
