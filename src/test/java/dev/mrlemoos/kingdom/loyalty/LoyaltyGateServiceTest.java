package dev.mrlemoos.kingdom.loyalty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Civil effects of political loyalty tier: appointment bars, Commons vote bars, constable
 * scrutiny, and warrant/arrest eligibility. Purely a gate API keyed by {@link LoyaltyTier} (via
 * {@link LoyaltyService#tierOf}) — it never consults kingdom rank, so there is no noble loyalty
 * immunity by construction.
 */
class LoyaltyGateServiceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryLoyaltyStore store;
    private LoyaltyService loyaltyService;
    private LoyaltyGateService gate;

    @BeforeEach
    void setUp() {
        store = new InMemoryLoyaltyStore();
        loyaltyService = new LoyaltyService(store, LoyaltyConfig.enabled());
        gate = new LoyaltyGateService(loyaltyService);
    }

    @Test
    void faithfulPassesEveryGate() {
        assertTrue(gate.canVoteInCommons(PLAYER));
        assertTrue(gate.canReceiveCrownAppointment(PLAYER));
        assertTrue(gate.canHoldOffice(PLAYER));
        assertTrue(gate.canServeOnLevy(PLAYER));
        assertTrue(gate.canRetainCrownTrust(PLAYER));
        assertFalse(gate.requiresConstableScrutiny(PLAYER));
        assertFalse(gate.isWarrantEligible(PLAYER));
        assertFalse(gate.isArrestOnSightEligible(PLAYER, true, true));
    }

    @Test
    void doubtfulIsFlaggedForConstableScrutinyButKeepsVoteAndSeatedOffice() {
        store.putTier(PLAYER, LoyaltyTier.DOUBTFUL);

        assertTrue(gate.requiresConstableScrutiny(PLAYER));
        assertTrue(gate.canVoteInCommons(PLAYER));
        assertTrue(gate.canHoldOffice(PLAYER));
        assertTrue(gate.canServeOnLevy(PLAYER));
        assertTrue(gate.canRetainCrownTrust(PLAYER));
    }

    @Test
    void doubtfulIsBarredFromNewCrownAppointments() {
        store.putTier(PLAYER, LoyaltyTier.DOUBTFUL);

        assertFalse(gate.canReceiveCrownAppointment(PLAYER));
    }

    @Test
    void disloyalCannotVoteInCommons() {
        store.putTier(PLAYER, LoyaltyTier.DISLOYAL);

        assertFalse(gate.canVoteInCommons(PLAYER));
    }

    @Test
    void disloyalIsBarredFromNewCrownAppointmentsAndFromHoldingOffice() {
        store.putTier(PLAYER, LoyaltyTier.DISLOYAL);

        assertFalse(gate.canReceiveCrownAppointment(PLAYER));
        assertFalse(gate.canHoldOffice(PLAYER));
    }

    @Test
    void disloyalMayStillServeOnLevyAndRetainsCrownTrust() {
        store.putTier(PLAYER, LoyaltyTier.DISLOYAL);

        assertTrue(gate.canServeOnLevy(PLAYER));
        assertTrue(gate.canRetainCrownTrust(PLAYER));
    }

    @Test
    void disloyalBecomesWarrantEligibleAndRemainsUnderConstableScrutiny() {
        store.putTier(PLAYER, LoyaltyTier.DISLOYAL);

        assertTrue(gate.isWarrantEligible(PLAYER));
        assertTrue(gate.requiresConstableScrutiny(PLAYER));
    }

    @Test
    void disloyalIsNeverArrestOnSightEligibleRegardlessOfWarrantOrTreasonReport() {
        store.putTier(PLAYER, LoyaltyTier.DISLOYAL);

        assertFalse(gate.isArrestOnSightEligible(PLAYER, true, true));
    }

    @Test
    void traitorIsBarredFromOfficeLevyAndCrownTrust() {
        store.putTier(PLAYER, LoyaltyTier.TRAITOR);

        assertFalse(gate.canHoldOffice(PLAYER));
        assertFalse(gate.canServeOnLevy(PLAYER));
        assertFalse(gate.canRetainCrownTrust(PLAYER));
        assertFalse(gate.canReceiveCrownAppointment(PLAYER));
        assertFalse(gate.canVoteInCommons(PLAYER));
    }

    @Test
    void traitorIsArrestableWithAnActiveWarrant() {
        store.putTier(PLAYER, LoyaltyTier.TRAITOR);

        assertTrue(gate.isArrestOnSightEligible(PLAYER, true, false));
    }

    @Test
    void traitorIsArrestableOnFreshTreasonReportEvenWithoutAWarrant() {
        store.putTier(PLAYER, LoyaltyTier.TRAITOR);

        assertTrue(gate.isArrestOnSightEligible(PLAYER, false, true));
    }

    @Test
    void traitorIsNotArrestOnSightEligibleWithoutAWarrantOrFreshTreasonReport() {
        store.putTier(PLAYER, LoyaltyTier.TRAITOR);

        assertFalse(gate.isArrestOnSightEligible(PLAYER, false, false));
    }

    @Test
    void noNobleLoyaltyImmunityGatesApplyRegardlessOfRankBecauseTheGateNeverConsultsRank() {
        // The gate is keyed purely by LoyaltyTier via LoyaltyService — it has no rank or title
        // parameter anywhere in its API, so a King/Queen dropped to Disloyal is blocked exactly
        // the same way a Knight would be. Recording an Act breach against the same player twice
        // reaches Disloyal regardless of any noble rank tracked elsewhere in the kingdom model.
        loyaltyService.recordActBreach(PLAYER);
        loyaltyService.recordActBreach(PLAYER);

        assertFalse(gate.canVoteInCommons(PLAYER));
        assertFalse(gate.canHoldOffice(PLAYER));
        assertFalse(gate.canReceiveCrownAppointment(PLAYER));
    }

    @Test
    void tierOverloadsMatchPlayerIdOverloadsForEveryGate() {
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            store.putTier(PLAYER, tier);

            assertTrue(gate.canVoteInCommons(tier) == gate.canVoteInCommons(PLAYER));
            assertTrue(gate.canReceiveCrownAppointment(tier) == gate.canReceiveCrownAppointment(PLAYER));
            assertTrue(gate.canHoldOffice(tier) == gate.canHoldOffice(PLAYER));
            assertTrue(gate.canServeOnLevy(tier) == gate.canServeOnLevy(PLAYER));
            assertTrue(gate.canRetainCrownTrust(tier) == gate.canRetainCrownTrust(PLAYER));
            assertTrue(gate.requiresConstableScrutiny(tier) == gate.requiresConstableScrutiny(PLAYER));
            assertTrue(gate.isWarrantEligible(tier) == gate.isWarrantEligible(PLAYER));
        }
    }
}
