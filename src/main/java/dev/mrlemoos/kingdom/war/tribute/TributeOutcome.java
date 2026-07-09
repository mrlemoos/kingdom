package dev.mrlemoos.kingdom.war.tribute;

/**
 * The result of applying a <b>war tribute</b>: {@code transferred} Corona moved from the defeated
 * kingdom's treasury into the victor's, and {@code debtRecorded} is the shortfall (if any) newly
 * recorded as <b>war debt</b>.
 */
public record TributeOutcome(double transferred, double debtRecorded) {}
