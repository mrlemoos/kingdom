package dev.leo.kingdom.economy.model;

import java.util.Optional;

public final class VillagerWalletState {

    private double balance;
    private Long frozenSinceEpochDay;

    public VillagerWalletState() {
        this(0.0, null);
    }

    public VillagerWalletState(double balance, Long frozenSinceEpochDay) {
        this.balance = balance;
        this.frozenSinceEpochDay = frozenSinceEpochDay;
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Optional<Long> frozenSinceEpochDay() {
        return Optional.ofNullable(frozenSinceEpochDay);
    }

    public boolean isFrozen() {
        return frozenSinceEpochDay != null;
    }

    public void markActive() {
        frozenSinceEpochDay = null;
    }

    public void markFrozen(long epochDay) {
        if (frozenSinceEpochDay == null) {
            frozenSinceEpochDay = epochDay;
        }
    }
}
