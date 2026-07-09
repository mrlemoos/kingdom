package dev.mrlemoos.kingdom.model.parliament;

/**
 * A conduct provision carried by a bill or enacted Act — separate from fiscal rate fields.
 * Parameterised kinds (curfew windows, war-limit details) land in later slices.
 */
public record ConductProvision(ConductKind kind) {

    public ConductProvision {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
    }
}
