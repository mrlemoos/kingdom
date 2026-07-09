package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates player action facts against active conduct provisions for the jurisdiction kingdom.
 * Domain-only — Bukkit listeners pass facts in; no Bukkit types here.
 */
public final class ActBreachDetector {

    public Optional<ActBreach> evaluate(BlockActionFacts facts, List<AssentedAct> activeActs) {
        if (facts == null || activeActs == null || activeActs.isEmpty()) {
            return Optional.empty();
        }
        for (AssentedAct act : activeActs) {
            if (act == null) {
                continue;
            }
            Optional<ConductKind> matched = matchBuildBan(act.conductProvisions());
            if (matched.isPresent()) {
                return Optional.of(new ActBreach(
                        facts.jurisdictionKingdomId(), act.billId(), matched.get()));
            }
        }
        return Optional.empty();
    }

    private static Optional<ConductKind> matchBuildBan(List<ConductProvision> provisions) {
        if (provisions == null) {
            return Optional.empty();
        }
        for (ConductProvision provision : provisions) {
            if (provision != null && provision.kind() == ConductKind.BUILD_BAN) {
                return Optional.of(ConductKind.BUILD_BAN);
            }
        }
        return Optional.empty();
    }
}
