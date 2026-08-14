package dev.mrlemoos.kingdom.hearth;

/** The fuel a hearth's container holds, and the taking of it. */
public interface HearthFuelStore {

    int fuelCount();

    void consumeFuel(int amount);
}
