package dev.mrlemoos.kingdom.hearth;

import java.util.UUID;

/**
 * A villager standing in a kingdom's territory on a winter day, and whether it is one of those who
 * never strike whatever the weather — a seated villager MP or a Lord of the Treasury.
 */
public record ColdSubject(UUID villagerId, double x, double y, double z, boolean strikeExempt) {}
