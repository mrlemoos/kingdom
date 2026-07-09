package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Military morale track only. Mirrors {@link LoyaltyService}, but the track is closed (no tier)
 * until opened by oath of service or a facts-only siege hostile action, unlike the political track
 * which always defaults to Faithful.
 */
class MoraleServiceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryMoraleStore store;
    private MoraleService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryMoraleStore();
        service = new MoraleService(store, MoraleConfig.enabled());
    }

    @Test
    void trackIsClosedByDefault() {
        assertTrue(service.tierOf(PLAYER).isEmpty());
    }

    @Test
    void openTrackOpensAtSteadfast() {
        MoraleResult result = service.openTrack(PLAYER);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, ((MoraleResult.Success) result).tier());
        assertTrue(((MoraleResult.Success) result).previous().isEmpty());
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void oathOfServiceIsAnAliasForOpenTrack() {
        MoraleResult result = service.oathOfService(PLAYER);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void reOpeningAnAlreadyOpenTrackDoesNotResetADegradedTier() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());

        MoraleResult result = service.openTrack(PLAYER);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.SHAKEN, ((MoraleResult.Success) result).tier());
        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void siegeHostileActionOpensAClosedTrackDirectlyAtShaken() {
        MoraleResult result = service.recordSiegeHostileAction(PLAYER, true);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.SHAKEN, ((MoraleResult.Success) result).tier());
        assertTrue(((MoraleResult.Success) result).previous().isEmpty());
        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void siegeHostileActionDegradesSteadfastToShaken() {
        service.openTrack(PLAYER);

        service.recordSiegeHostileAction(PLAYER, true);

        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void siegeHostileActionDegradesShakenToBreaking() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);

        service.recordSiegeHostileAction(PLAYER, true);

        assertEquals(MoraleTier.BREAKING, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void siegeHostileActionDegradesBreakingToRout() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);

        service.recordSiegeHostileAction(PLAYER, true);

        assertEquals(MoraleTier.ROUT, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void furtherSiegeHostileActionsAtRoutStayAtRout() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);

        service.recordSiegeHostileAction(PLAYER, true);

        assertEquals(MoraleTier.ROUT, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void hostileActionOutsideTheSiegeZoneIsANoOpAndLeavesTheTrackClosed() {
        MoraleResult result = service.recordSiegeHostileAction(PLAYER, false);

        assertInstanceOf(MoraleResult.Failure.class, result);
        assertTrue(service.tierOf(PLAYER).isEmpty());
    }

    @Test
    void disabledFlagLeavesOpenTrackANoOp() {
        MoraleService disabled = new MoraleService(store, MoraleConfig.disabled());

        MoraleResult result = disabled.openTrack(PLAYER);

        assertInstanceOf(MoraleResult.Disabled.class, result);
        assertTrue(disabled.tierOf(PLAYER).isEmpty());
    }

    @Test
    void disabledFlagLeavesSiegeHostileActionANoOp() {
        MoraleService disabled = new MoraleService(store, MoraleConfig.disabled());

        MoraleResult result = disabled.recordSiegeHostileAction(PLAYER, true);

        assertInstanceOf(MoraleResult.Disabled.class, result);
        assertTrue(disabled.tierOf(PLAYER).isEmpty());
    }

    @Test
    void tickRecoveryFailsWhenTrackIsClosed() {
        MoraleResult result = service.tickRecovery(PLAYER, 10L);

        assertInstanceOf(MoraleResult.Failure.class, result);
        assertTrue(service.tierOf(PLAYER).isEmpty());
    }

    @Test
    void tickRecoveryIsANoOpWhileAlreadySteadfast() {
        service.openTrack(PLAYER);

        MoraleResult result = service.tickRecovery(PLAYER, 10L);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void tickRecoveryStartsTheClockOnFirstCallWithoutPromoting() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);

        service.tickRecovery(PLAYER, 10L);

        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void tickRecoveryPromotesShakenToSteadfastAfterConfiguredDays() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.tickRecovery(PLAYER, 10L);
        int daysPerTier = MoraleConfig.enabled().recoveryMcDaysPerTier();

        MoraleResult result = service.tickRecovery(PLAYER, 10L + daysPerTier);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, ((MoraleResult.Success) result).tier());
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void tickRecoveryPromotesBreakingToShakenThenSteadfast() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        assertEquals(MoraleTier.BREAKING, service.tierOf(PLAYER).orElseThrow());
        int daysPerTier = MoraleConfig.enabled().recoveryMcDaysPerTier();
        service.tickRecovery(PLAYER, 0L);

        service.tickRecovery(PLAYER, daysPerTier);
        assertEquals(MoraleTier.SHAKEN, service.tierOf(PLAYER).orElseThrow());

        service.tickRecovery(PLAYER, 2L * daysPerTier);
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void routCannotTimeRecover() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        assertEquals(MoraleTier.ROUT, service.tierOf(PLAYER).orElseThrow());

        MoraleResult result = service.tickRecovery(PLAYER, 1_000_000L);

        assertInstanceOf(MoraleResult.Failure.class, result);
        assertEquals(MoraleTier.ROUT, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void pardonByCrownRestoresRoutToSteadfast() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);

        MoraleResult result = service.pardon(PLAYER, NobleRank.QUEEN);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, ((MoraleResult.Success) result).tier());
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void pardonByKnightRestoresRoutToSteadfast() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);

        MoraleResult result = service.pardon(PLAYER, NobleRank.KNIGHT);

        assertInstanceOf(MoraleResult.Success.class, result);
        assertEquals(MoraleTier.STEADFAST, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void pardonByUnauthorisedActorFails() {
        service.openTrack(PLAYER);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);
        service.recordSiegeHostileAction(PLAYER, true);

        MoraleResult result = service.pardon(PLAYER, NobleRank.MP);

        assertInstanceOf(MoraleResult.Failure.class, result);
        assertEquals(MoraleTier.ROUT, service.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void disabledFlagMakesTickRecoveryANoOp() {
        MoraleService disabled = new MoraleService(store, MoraleConfig.disabled());

        MoraleResult result = disabled.tickRecovery(PLAYER, 10L);

        assertInstanceOf(MoraleResult.Disabled.class, result);
    }

    @Test
    void disabledFlagMakesPardonANoOp() {
        MoraleService disabled = new MoraleService(store, MoraleConfig.disabled());

        MoraleResult result = disabled.pardon(PLAYER, NobleRank.QUEEN);

        assertInstanceOf(MoraleResult.Disabled.class, result);
    }
}
