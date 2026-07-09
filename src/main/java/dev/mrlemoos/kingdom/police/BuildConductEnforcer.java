package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Evaluates block place/break against enacted build-ban provisions in jurisdiction.
 * Operators are exempt. Breach reporting is debounced per player+kingdom cluster.
 */
public final class BuildConductEnforcer {

    private final ActBreachDetector detector;
    private final BuildEnforcementConfig config;
    private final LongSupplier clockMs;
    private final Map<String, Long> lastBreachReportMs = new HashMap<>();

    public BuildConductEnforcer(
            ActBreachDetector detector, BuildEnforcementConfig config, LongSupplier clockMs) {
        this.detector = Objects.requireNonNull(detector, "detector");
        this.config = Objects.requireNonNull(config, "config");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    public BuildEnforcementDecision evaluate(
            BlockActionFacts facts,
            List<AssentedAct> activeActs,
            UUID actorId,
            boolean operator) {
        if (!config.enabled()) {
            return BuildEnforcementDecision.allow();
        }
        if (operator) {
            return BuildEnforcementDecision.allow();
        }
        if (facts == null || actorId == null) {
            return BuildEnforcementDecision.allow();
        }

        Optional<ActBreach> detected = detector.evaluate(facts, activeActs);
        if (detected.isEmpty()) {
            return BuildEnforcementDecision.allow();
        }

        Optional<ActBreach> reportable = debounce(actorId, facts.jurisdictionKingdomId(), detected.get());
        return BuildEnforcementDecision.deny(reportable);
    }

    public BuildEnforcementConfig config() {
        return config;
    }

    private Optional<ActBreach> debounce(UUID actorId, String kingdomId, ActBreach breach) {
        String key = actorId + "|" + kingdomId;
        long now = clockMs.getAsLong();
        Long last = lastBreachReportMs.get(key);
        if (last != null && now - last < config.debounceMs()) {
            return Optional.empty();
        }
        lastBreachReportMs.put(key, now);
        return Optional.of(breach);
    }
}
