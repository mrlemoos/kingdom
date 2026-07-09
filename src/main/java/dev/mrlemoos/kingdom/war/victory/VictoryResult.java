package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import java.util.Objects;

/**
 * The result of evaluating an {@link ActiveWar}'s enacted war aim for decisive victory (see the
 * Decisive victory glossary entry in {@code CONTEXT.md}): either the aim is not yet satisfied, or
 * it is, and the war has decisively ended.
 */
public sealed interface VictoryResult permits VictoryResult.NotMet, VictoryResult.Victory {

    record NotMet(String message) implements VictoryResult {
        public NotMet {
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    /**
     * Decisive victory metadata: the victor is always the attacker and the defeated party the
     * defender, since the enacted war aim is defined from the attacker's perspective (capturing
     * the defender's territory or capital).
     */
    record Victory(String message, ActiveWar war) implements VictoryResult {
        public Victory {
            Objects.requireNonNull(message, "message must not be null");
            Objects.requireNonNull(war, "war must not be null");
        }

        public String victorKingdomId() {
            return war.attackerKingdomId();
        }

        public String defeatedKingdomId() {
            return war.defenderKingdomId();
        }
    }

    static NotMet notMet(String message) {
        return new NotMet(message);
    }

    static Victory victory(String message, ActiveWar war) {
        return new Victory(message, war);
    }
}
