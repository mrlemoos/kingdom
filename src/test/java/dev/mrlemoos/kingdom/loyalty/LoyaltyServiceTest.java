package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
