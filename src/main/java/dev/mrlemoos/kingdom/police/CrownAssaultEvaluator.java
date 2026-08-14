package dev.mrlemoos.kingdom.police;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Whether a player-attributed hit on the Crown should open a flagrant treason warrant.
 */
public final class CrownAssaultEvaluator {

    private final CrownAssaultConfig config;

    public CrownAssaultEvaluator(CrownAssaultConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public Optional<CrownAssault> evaluate(CrownAssaultFacts facts) {
        if (facts == null) {
            return Optional.empty();
        }
        if (!facts.playerAttributed()
                || !facts.inJurisdiction()
                || !facts.victimIsCrownOfThisKingdom()
                || facts.assailantImmune()
                || !facts.policeReady()
                || facts.alreadyOpen()) {
            return Optional.empty();
        }
        OptionalDouble nearest = facts.nearestPatrolGolemBlocksFromVictim();
        if (nearest.isEmpty() || nearest.getAsDouble() > config.witnessRangeBlocks()) {
            return Optional.empty();
        }
        return Optional.of(new CrownAssault(facts.jurisdictionKingdomId()));
    }

    public CrownAssaultConfig config() {
        return config;
    }
}
