package dev.mrlemoos.kingdom.model.parliament;

/**
 * Kind of behaviour rule that may be embedded in an enacted Act as a conduct provision.
 * {@link #TREASON} and {@link #GRAIN_THEFT} are warrant charges only — never Act conduct
 * provisions.
 * PvP restrictions are intentionally absent under open PvP.
 */
public enum ConductKind {
    BUILD_BAN,
    CURFEW,
    WAR_LIMIT,
    TREASON,
    GRAIN_THEFT
}
