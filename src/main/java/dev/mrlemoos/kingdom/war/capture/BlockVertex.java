package dev.mrlemoos.kingdom.war.capture;

/**
 * A block-coordinate vertex used to describe a proposed WorldGuard region boundary. Two-dimensional
 * (X/Z only) as capture and merge planning does not depend on vertical bounds.
 */
public record BlockVertex(int blockX, int blockZ) {}
