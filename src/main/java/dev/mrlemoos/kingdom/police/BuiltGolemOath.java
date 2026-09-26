package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.NobleRank;

/** Whether an iron golem built by a subject takes the oath as a patrol golem. */
public enum BuiltGolemOath {
    SWORN,
    /** Built outside the builder's own realm: it stays vanilla, and nothing is said. */
    OUTSIDE_TERRITORY,
    /** The builder may not deploy officers: it stays vanilla, and nothing is said. */
    NOT_AUTHORISED,
    /** The builder may, but the watch is full: it stays vanilla, and the builder is told. */
    CAPPED;

    public static BuiltGolemOath decide(
            NobleRank builderRank, boolean operator, boolean inOwnTerritory, int patrolGolems, int patrolCap) {
        if (!inOwnTerritory) {
            return OUTSIDE_TERRITORY;
        }
        if (!PoliceAuthority.canDeployGolems(builderRank, operator)) {
            return NOT_AUTHORISED;
        }
        return patrolGolems >= patrolCap ? CAPPED : SWORN;
    }
}
