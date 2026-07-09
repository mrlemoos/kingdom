package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoyaltyServiceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryLoyaltyStore store;
    private LoyaltyService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryLoyaltyStore();
        service = new LoyaltyService(store, LoyaltyConfig.enabled());
    }

    @Test
    void defaultTierIsFaithful() {
        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
    }

    @Test
    void firstActBreachLowersFaithfulToDoubtful() {
        LoyaltyResult result = service.recordActBreach(PLAYER);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.DOUBTFUL, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
    }

    @Test
    void secondActBreachLowersDoubtfulToDisloyal() {
        service.recordActBreach(PLAYER);

        LoyaltyResult result = service.recordActBreach(PLAYER);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.DISLOYAL, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.DISLOYAL, service.tierOf(PLAYER));
    }

    @Test
    void furtherActBreachesDoNotApplyTraitorWithoutConviction() {
        service.recordActBreach(PLAYER);
        service.recordActBreach(PLAYER);
        service.recordActBreach(PLAYER);

        assertEquals(LoyaltyTier.DISLOYAL, service.tierOf(PLAYER));
    }

    @Test
    void actBreachDoesNotClearTraitor() {
        service.convictTreason(PLAYER);

        service.recordActBreach(PLAYER);

        assertEquals(LoyaltyTier.TRAITOR, service.tierOf(PLAYER));
    }

    @Test
    void treasonConvictionAppliesTraitor() {
        service.recordActBreach(PLAYER);

        LoyaltyResult result = service.convictTreason(PLAYER);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.TRAITOR, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.TRAITOR, service.tierOf(PLAYER));
    }

    @Test
    void disabledFlagLeavesTierUnchangedOnBreach() {
        LoyaltyService disabled = new LoyaltyService(store, LoyaltyConfig.disabled());

        LoyaltyResult result = disabled.recordActBreach(PLAYER);

        assertInstanceOf(LoyaltyResult.Disabled.class, result);
        assertEquals(LoyaltyTier.FAITHFUL, disabled.tierOf(PLAYER));
    }

    @Test
    void tickRecoveryIsANoOpWhileAlreadyFaithful() {
        LoyaltyResult result = service.tickRecovery(PLAYER, 10L);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
    }

    @Test
    void tickRecoveryStartsTheClockOnFirstCallWithoutPromoting() {
        service.recordActBreach(PLAYER);

        service.tickRecovery(PLAYER, 10L);

        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
    }

    @Test
    void tickRecoveryDoesNotPromoteBeforeConfiguredDaysElapse() {
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 10L);

        service.tickRecovery(PLAYER, 11L);

        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
    }

    @Test
    void tickRecoveryPromotesDoubtfulToFaithfulAfterConfiguredDays() {
        service.recordActBreach(PLAYER);
        service.tickRecovery(PLAYER, 10L);

        LoyaltyResult result = service.tickRecovery(PLAYER, 10L + LoyaltyConfig.enabled().recoveryMcDaysPerTier());

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.FAITHFUL, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
    }

    @Test
    void tickRecoveryPromotesDisloyalToDoubtfulThenDoubtfulToFaithful() {
        service.recordActBreach(PLAYER);
        service.recordActBreach(PLAYER);
        int daysPerTier = LoyaltyConfig.enabled().recoveryMcDaysPerTier();
        service.tickRecovery(PLAYER, 0L);

        service.tickRecovery(PLAYER, daysPerTier);
        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));

        service.tickRecovery(PLAYER, daysPerTier);
        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));

        service.tickRecovery(PLAYER, 2L * daysPerTier);
        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
    }

    @Test
    void furtherOffenceResetsTheRecoveryClock() {
        service.recordActBreach(PLAYER);
        int daysPerTier = LoyaltyConfig.enabled().recoveryMcDaysPerTier();
        service.tickRecovery(PLAYER, 0L);

        service.recordActBreach(PLAYER);
        assertEquals(LoyaltyTier.DISLOYAL, service.tierOf(PLAYER));

        service.tickRecovery(PLAYER, daysPerTier);

        assertEquals(LoyaltyTier.DISLOYAL, service.tierOf(PLAYER));
    }

    @Test
    void traitorCannotTimeRecover() {
        service.convictTreason(PLAYER);

        LoyaltyResult result = service.tickRecovery(PLAYER, 1_000_000L);

        assertInstanceOf(LoyaltyResult.Failure.class, result);
        assertEquals(LoyaltyTier.TRAITOR, service.tierOf(PLAYER));
    }

    @Test
    void pardonByKingRestoresTraitorToFaithful() {
        service.convictTreason(PLAYER);

        LoyaltyResult result = service.pardon(PLAYER, NobleRank.KING, false);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.FAITHFUL, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.FAITHFUL, service.tierOf(PLAYER));
    }

    @Test
    void partialPardonByQueenRestoresTraitorToDoubtfulOnly() {
        service.convictTreason(PLAYER);

        LoyaltyResult result = service.pardon(PLAYER, NobleRank.QUEEN, true);

        assertInstanceOf(LoyaltyResult.Success.class, result);
        assertEquals(LoyaltyTier.DOUBTFUL, ((LoyaltyResult.Success) result).tier());
        assertEquals(LoyaltyTier.DOUBTFUL, service.tierOf(PLAYER));
    }

    @Test
    void pardonByNonCrownActorFails() {
        service.convictTreason(PLAYER);

        LoyaltyResult result = service.pardon(PLAYER, NobleRank.KNIGHT, false);

        assertInstanceOf(LoyaltyResult.Failure.class, result);
        assertEquals(LoyaltyTier.TRAITOR, service.tierOf(PLAYER));
    }

    @Test
    void disabledFlagMakesTickRecoveryANoOp() {
        LoyaltyService disabled = new LoyaltyService(store, LoyaltyConfig.disabled());

        LoyaltyResult result = disabled.tickRecovery(PLAYER, 10L);

        assertInstanceOf(LoyaltyResult.Disabled.class, result);
    }

    @Test
    void disabledFlagMakesPardonANoOp() {
        LoyaltyService disabled = new LoyaltyService(store, LoyaltyConfig.disabled());

        LoyaltyResult result = disabled.pardon(PLAYER, NobleRank.KING, false);

        assertInstanceOf(LoyaltyResult.Disabled.class, result);
    }
}
