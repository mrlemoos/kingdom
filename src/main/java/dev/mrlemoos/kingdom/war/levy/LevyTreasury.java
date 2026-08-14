package dev.mrlemoos.kingdom.war.levy;

/**
 * The treasury as the levy sees it: a spend that may run it dry. Returns what was actually taken,
 * which is at most the balance standing.
 */
@FunctionalInterface
public interface LevyTreasury {

    double debit(String kingdomId, double amount);
}
