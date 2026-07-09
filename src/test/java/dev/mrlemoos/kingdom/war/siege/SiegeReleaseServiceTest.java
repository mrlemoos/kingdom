package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.desertion.DesertionEvaluator;
import dev.mrlemoos.kingdom.war.desertion.InMemoryTreasonReviewStore;
import dev.mrlemoos.kingdom.war.desertion.MoraleStoreTrack;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Siege release: an officer, or the crown/a knight at a muster point, grants a fealty subject
 * permission to leave an active siege without a morale breach. A valid, unexpired release makes
 * departure lawful and consumes the grant; without one, departure is desertion via the shared
 * {@link DesertionEvaluator} breach table, honouring the standing force's stricter hardened
 * threshold.
 */
class SiegeReleaseServiceTest {

    private static final UUID SUBJECT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRANTER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String WAR_ID = "war-1";
    private static final long NOW = 1_700_000_000_000L;

    private InMemorySiegeReleaseStore store;
    private SiegeReleaseService service;
    private DesertionEvaluator desertionEvaluator;
    private MoraleStoreTrack moraleTrack;

    @BeforeEach
    void setUp() {
        store = new InMemorySiegeReleaseStore();
        service = new SiegeReleaseService(store, SiegeReleaseConfig.on(), () -> NOW);
        moraleTrack = new MoraleStoreTrack(new InMemoryMoraleStore(), MoraleConfig.enabled());
        desertionEvaluator = new DesertionEvaluator(moraleTrack, new InMemoryTreasonReviewStore(), () -> NOW);
        service.setDesertionEvaluator(desertionEvaluator);
    }

    @Test
    void grantingARecordsAnAuditableGrantWithExpiry() {
        WarResult result = service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "officer release in the field");

        assertInstanceOf(WarResult.Success.class, result);
        SiegeReleaseGrant grant = service.findGrant(SUBJECT, WAR_ID).orElseThrow();
        assertEquals(SUBJECT, grant.subjectId());
        assertEquals(WAR_ID, grant.warId());
        assertEquals(GRANTER, grant.grantedBy());
        assertEquals(NOW, grant.grantedAtMs());
        assertEquals(NOW + 10_000L, grant.expiresAtMs());
        assertEquals("officer release in the field", grant.note());
    }

    @Test
    void grantingWithoutAnExplicitDurationUsesTheConfiguredDefault() {
        service.grant(SUBJECT, WAR_ID, GRANTER, "muster point release");

        SiegeReleaseGrant grant = service.findGrant(SUBJECT, WAR_ID).orElseThrow();
        assertEquals(NOW + service.config().defaultDurationMs(), grant.expiresAtMs());
    }

    @Test
    void grantingWhenSiegeReleaseIsDisabledFails() {
        SiegeReleaseService disabled = new SiegeReleaseService(store, SiegeReleaseConfig.off(), () -> NOW);

        WarResult result = disabled.grant(SUBJECT, WAR_ID, GRANTER, "field release");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(disabled.findGrant(SUBJECT, WAR_ID).isEmpty());
    }

    @Test
    void grantingWithANonPositiveDurationFails() {
        WarResult result = service.grant(SUBJECT, WAR_ID, GRANTER, 0L, "field release");

        assertInstanceOf(WarResult.Failure.class, result);
        assertTrue(service.findGrant(SUBJECT, WAR_ID).isEmpty());
    }

    @Test
    void hasValidReleaseIsTrueBeforeExpiryAndFalseAfter() {
        service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "field release");

        assertTrue(service.hasValidRelease(SUBJECT, WAR_ID, NOW + 5_000L));
        assertFalse(service.hasValidRelease(SUBJECT, WAR_ID, NOW + 10_000L));
        assertFalse(service.hasValidRelease(SUBJECT, WAR_ID, NOW + 20_000L));
    }

    @Test
    void hasValidReleaseIsFalseWhenNeverGranted() {
        assertFalse(service.hasValidRelease(SUBJECT, WAR_ID, NOW));
    }

    @Test
    void revokingAGrantAheadOfExpiryEndsItsValidity() {
        service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "field release");

        WarResult result = service.revoke(SUBJECT, WAR_ID);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(service.hasValidRelease(SUBJECT, WAR_ID, NOW + 1_000L));
    }

    @Test
    void revokingAGrantThatDoesNotExistFails() {
        WarResult result = service.revoke(SUBJECT, WAR_ID);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void departureWithAValidReleaseIsLawfulAndCausesNoMoraleBreach() {
        service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "field release");

        SiegeDepartureResult result = service.evaluateDeparture(SUBJECT, WAR_ID, false, NOW + 1_000L);

        assertTrue(result.released());
        assertFalse(result.isDeserted());
        assertNull(result.desertionResult());
        assertEquals(MoraleTier.STEADFAST, moraleTrack.tierOf(SUBJECT));
    }

    @Test
    void departureWithAValidReleaseConsumesTheGrantSoItCannotCoverASecondDeparture() {
        service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "field release");

        service.evaluateDeparture(SUBJECT, WAR_ID, false, NOW + 1_000L);

        assertFalse(service.hasValidRelease(SUBJECT, WAR_ID, NOW + 2_000L));
    }

    @Test
    void departureWithoutAReleaseUnderStandardServiceDropsMoraleOneStep() {
        SiegeDepartureResult result = service.evaluateDeparture(SUBJECT, WAR_ID, false, NOW);

        assertFalse(result.released());
        assertTrue(result.isDeserted());
        assertEquals(MoraleTier.SHAKEN, result.desertionResult().moraleTier());
    }

    @Test
    void departureWithoutAReleaseUnderHardenedServiceReachesBreakingImmediately() {
        SiegeDepartureResult result = service.evaluateDeparture(SUBJECT, WAR_ID, true, NOW);

        assertFalse(result.released());
        assertTrue(result.isDeserted());
        assertEquals(MoraleTier.BREAKING, result.desertionResult().moraleTier());
    }

    @Test
    void departureWithAnExpiredReleaseIsTreatedAsUnreleased() {
        service.grant(SUBJECT, WAR_ID, GRANTER, 10_000L, "field release");

        SiegeDepartureResult result = service.evaluateDeparture(SUBJECT, WAR_ID, true, NOW + 10_000L);

        assertFalse(result.released());
        assertEquals(MoraleTier.BREAKING, result.desertionResult().moraleTier());
    }

    @Test
    void evaluatingAnUnreleasedDepartureWithoutADesertionEvaluatorConfiguredThrows() {
        SiegeReleaseService withoutEvaluator = new SiegeReleaseService(store, SiegeReleaseConfig.on(), () -> NOW);

        assertThrows(
                IllegalStateException.class, () -> withoutEvaluator.evaluateDeparture(SUBJECT, WAR_ID, false, NOW));
    }

    @Test
    void absenceExceedingTheHardenedThresholdIsReported() {
        long thresholdMs = service.config().hardenedThresholdMs();

        assertFalse(service.absenceExceedsHardenedThreshold(thresholdMs));
        assertTrue(service.absenceExceedsHardenedThreshold(thresholdMs + 1));
    }
}
