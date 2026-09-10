package dev.mrlemoos.kingdom.war.occupation;

/** Build overlay for a captured chunk. DEFER means ordinary permits and Acts still apply. */
public enum OccupationBuildOutcome {
    DEFER,
    ALLOW_OCCUPIER,
    DENY
}
