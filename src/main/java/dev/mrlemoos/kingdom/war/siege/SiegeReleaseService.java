package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.desertion.DesertionEvaluator;
import dev.mrlemoos.kingdom.war.desertion.MoraleBreachKind;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Siege release: permission for a fealty subject to leave an active siege without a morale
 * breach (see the <b>Siege release</b> glossary entry in {@code CONTEXT.md}). Granting, checking,
 * and evaluating a departure are all wired through here, delegating unreleased-departure breach
 * assessment to the shared {@link DesertionEvaluator} rather than duplicating the breach table.
 *
 * <p><b>Consume-on-departure:</b> a valid release is spent the moment {@link #evaluateDeparture}
 * finds it — one grant covers exactly one lawful leave, matching the field-permission nature of
 * the release (it is not a standing pass for the rest of the siege).
 */
public final class SiegeReleaseService {

    private final SiegeReleaseStore store;
    private final SiegeReleaseConfig config;
    private final Supplier<Long> clockMs;
    private DesertionEvaluator desertionEvaluator;

    public SiegeReleaseService(SiegeReleaseStore store, SiegeReleaseConfig config) {
        this(store, config, System::currentTimeMillis);
    }

    public SiegeReleaseService(SiegeReleaseStore store, SiegeReleaseConfig config, Supplier<Long> clockMs) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    public SiegeReleaseConfig config() {
        return config;
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) wiring the shared breach table for the unreleased-departure path. Without this
     * set, {@link #evaluateDeparture} throws on an unreleased departure — desertion cannot be
     * assessed without it.
     */
    public void setDesertionEvaluator(DesertionEvaluator desertionEvaluator) {
        this.desertionEvaluator = desertionEvaluator;
    }

    /** Grants a siege release using the configured default duration. */
    public WarResult grant(UUID subjectId, String warId, UUID grantedBy, String note) {
        return grant(subjectId, warId, grantedBy, config.defaultDurationMs(), note);
    }

    /**
     * Grants a siege release for {@code subjectId} against {@code warId}, valid for
     * {@code durationMs} from now. Fails when siege release is disabled or the duration is not
     * positive. Re-granting for the same subject and war replaces any existing grant.
     */
    public WarResult grant(UUID subjectId, String warId, UUID grantedBy, long durationMs, String note) {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        Objects.requireNonNull(grantedBy, "grantedBy");
        if (!config.enabled()) {
            return WarResult.fail("Siege release is disabled.");
        }
        if (durationMs <= 0) {
            return WarResult.fail("Siege release duration must be positive.");
        }
        long nowMs = clockMs.get();
        store.save(new SiegeReleaseGrant(subjectId, warId, grantedBy, nowMs, nowMs + durationMs, note));
        return WarResult.ok("Siege release granted.");
    }

    /** Revokes a subject's siege release for a war ahead of its natural expiry. */
    public WarResult revoke(UUID subjectId, String warId) {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        if (store.find(subjectId, warId).isEmpty()) {
            return WarResult.fail("No siege release found to revoke.");
        }
        store.revoke(subjectId, warId);
        return WarResult.ok("Siege release revoked.");
    }

    /** True when {@code subjectId} currently holds an unexpired siege release for {@code warId}. */
    public boolean hasValidRelease(UUID subjectId, String warId, long nowMs) {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        Optional<SiegeReleaseGrant> grant = store.find(subjectId, warId);
        return grant.isPresent() && grant.get().isValidAt(nowMs);
    }

    public Optional<SiegeReleaseGrant> findGrant(UUID subjectId, String warId) {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        return store.find(subjectId, warId);
    }

    /**
     * Evaluates a subject's departure from an active siege. With a valid, unexpired release, the
     * grant is consumed and this returns a no-breach result without touching
     * {@link DesertionEvaluator}. Without one, delegates to the shared {@link DesertionEvaluator}
     * breach table for {@link MoraleBreachKind#LEAVE_SIEGE_WITHOUT_RELEASE}, honouring
     * {@code hardenedService}'s stricter siege-absence rule — callers should only invoke this for
     * a hardened subject once absence has exceeded the hardened threshold (see
     * {@link #absenceExceedsHardenedThreshold}).
     */
    public SiegeDepartureResult evaluateDeparture(UUID subjectId, String warId, boolean hardenedService, long nowMs) {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        if (hasValidRelease(subjectId, warId, nowMs)) {
            store.revoke(subjectId, warId);
            return SiegeDepartureResult.released("Siege left lawfully under a granted release.");
        }
        if (desertionEvaluator == null) {
            throw new IllegalStateException(
                    "DesertionEvaluator is not configured; call setDesertionEvaluator before evaluating an "
                            + "unreleased siege departure.");
        }
        return SiegeDepartureResult.deserted(desertionEvaluator.evaluate(
                subjectId, warId, MoraleBreachKind.LEAVE_SIEGE_WITHOUT_RELEASE, hardenedService));
    }

    /**
     * True when {@code absentMs} of siege absence exceeds the hardened-service threshold (1
     * mc-day by default) — the point past which the standing force's stricter absence rule bites.
     * Ordinary service has no absence threshold of its own; the desertion breach table already
     * covers it one tier at a time via {@link MoraleBreachKind#LEAVE_SIEGE_WITHOUT_RELEASE}.
     */
    public boolean absenceExceedsHardenedThreshold(long absentMs) {
        return absentMs > config.hardenedThresholdMs();
    }
}
