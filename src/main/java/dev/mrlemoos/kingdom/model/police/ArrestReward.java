package dev.mrlemoos.kingdom.model.police;

import java.util.Objects;
import java.util.UUID;

/**
 * Corona purse escrowed against an active warrant. Posted from a member wallet; paid to the
 * arresting constable on arrest, or refunded to the poster when a patrol golem detains or the
 * warrant path ends without arrest.
 */
public final class ArrestReward {

    private final UUID posterId;
    private double amount;

    public ArrestReward(UUID posterId, double amount) {
        this.posterId = Objects.requireNonNull(posterId, "posterId");
        if (amount <= 0) {
            throw new IllegalArgumentException("Arrest reward amount must be positive.");
        }
        this.amount = amount;
    }

    public UUID posterId() {
        return posterId;
    }

    public double amount() {
        return amount;
    }

    public void topUp(double extra) {
        if (extra <= 0) {
            throw new IllegalArgumentException("Arrest reward top-up must be positive.");
        }
        this.amount += extra;
    }
}
