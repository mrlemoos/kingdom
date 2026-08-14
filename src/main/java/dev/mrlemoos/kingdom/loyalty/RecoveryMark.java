package dev.mrlemoos.kingdom.loyalty;

/**
 * The persisted recovery clock for one subject on one track: the tier the wait was marked at, and
 * the in-game day it began. A mark whose tier no longer matches the subject's current tier is
 * stale and is restarted on the next tick, so any fresh offence resets the wait.
 */
public record RecoveryMark<T extends Enum<T>>(T tier, long mcDay) {}
