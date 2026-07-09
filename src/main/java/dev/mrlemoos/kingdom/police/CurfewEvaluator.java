package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates movement against enacted curfew conduct provisions using world-time facts.
 * Inside the configured window → allowed; outside → optional Act breach.
 */
public final class CurfewEvaluator {

    private final CurfewEnforcementConfig config;

    public CurfewEvaluator(CurfewEnforcementConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public Optional<ActBreach> evaluate(MovementFacts facts, List<AssentedAct> activeActs) {
        if (!config.enabled() || facts == null || activeActs == null || activeActs.isEmpty()) {
            return Optional.empty();
        }
        if (config.isInsideWindow(facts.worldTimeTick())) {
            return Optional.empty();
        }
        for (AssentedAct act : activeActs) {
            if (act == null) {
                continue;
            }
            if (hasCurfew(act.conductProvisions())) {
                return Optional.of(new ActBreach(
                        facts.jurisdictionKingdomId(), act.billId(), ConductKind.CURFEW));
            }
        }
        return Optional.empty();
    }

    public CurfewEnforcementConfig config() {
        return config;
    }

    private static boolean hasCurfew(List<ConductProvision> provisions) {
        if (provisions == null) {
            return false;
        }
        for (ConductProvision provision : provisions) {
            if (provision != null && provision.kind() == ConductKind.CURFEW) {
                return true;
            }
        }
        return false;
    }
}
