package dev.mrlemoos.kingdom.war.siege;

/** Current military presence in one war's defender territory. */
public record SiegePresence(int attackerCount, int defenderCount) {

    public static final SiegePresence EMPTY = new SiegePresence(0, 0);

    public SiegePresence {
        if (attackerCount < 0 || defenderCount < 0) {
            throw new IllegalArgumentException("Siege presence cannot be negative");
        }
    }
}
