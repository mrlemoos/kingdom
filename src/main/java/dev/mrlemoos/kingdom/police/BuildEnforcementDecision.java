package dev.mrlemoos.kingdom.police;

import java.util.Optional;

/**
 * Domain decision for a block action under build conduct provisions.
 * {@code denied} cancels the Bukkit event; {@code breach} is present when a new
 * Act-breach should be filed (debounced so clusters fire once).
 */
public record BuildEnforcementDecision(boolean denied, Optional<ActBreach> breach) {

    public BuildEnforcementDecision {
        breach = breach == null ? Optional.empty() : breach;
    }

    public static BuildEnforcementDecision allow() {
        return new BuildEnforcementDecision(false, Optional.empty());
    }

    public static BuildEnforcementDecision deny(Optional<ActBreach> breach) {
        return new BuildEnforcementDecision(true, breach);
    }
}
