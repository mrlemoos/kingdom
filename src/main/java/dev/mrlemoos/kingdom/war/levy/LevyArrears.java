package dev.mrlemoos.kingdom.war.levy;

/**
 * Levy upkeep a treasury could not meet, with the realm day on which it last paid anything towards
 * the levy and whether the realm has had its one public warning for this run of arrears.
 */
public record LevyArrears(double amount, long lastPaidDay, boolean warned) {}
