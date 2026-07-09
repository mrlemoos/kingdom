package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
