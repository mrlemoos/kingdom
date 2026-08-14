package dev.mrlemoos.kingdom.hearth;

/** A hearth as the day's reckoning knows it: where it stands and what its container holds. */
public record HearthSite(int x, int y, int z, HearthFuelStore fuel) {}
