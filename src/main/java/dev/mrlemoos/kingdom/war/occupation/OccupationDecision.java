package dev.mrlemoos.kingdom.war.occupation;

import java.util.Objects;
import java.util.Optional;

/**
 * Domain decision for a build action under {@code Occupation} rules (see the glossary entry in
 * {@code CONTEXT.md}). {@code allowed} is {@code true} when the occupation overlay itself raises
 * no objection; {@code reason} is present on denial so callers (e.g. a future Phase 2 listener or
 * {@code BuildConductEnforcer}) can report or log why. This decision is scoped purely to
 * occupation — it does not replace or short-circuit normal build conduct enforcement.
 */
public record OccupationDecision(boolean allowed, Optional<String> reason) {

    public OccupationDecision {
        reason = reason == null ? Optional.empty() : reason;
    }

    public static OccupationDecision allow() {
        return new OccupationDecision(true, Optional.empty());
    }

    public static OccupationDecision deny(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        return new OccupationDecision(false, Optional.of(reason));
    }
}
