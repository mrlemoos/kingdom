package dev.mrlemoos.kingdom.economy.service;

import java.util.Map;
import java.util.UUID;

public record PlayerTaxResult(
        double sharePerMember, double collected, double shortfall, Map<UUID, Payment> payments) {

    public PlayerTaxResult {
        payments = Map.copyOf(payments);
    }

    public Payment paymentFor(UUID playerId) {
        return payments.getOrDefault(playerId, new Payment(0.0, 0.0));
    }

    public record Payment(double paid, double shortfall) {}
}
