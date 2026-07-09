package dev.mrlemoos.kingdom.war.tribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link WarDebtStore}. YAML persistence in {@code economy.yml} follows in a later
 * slice per {@code docs/build-order.md} Slice 6.7.
 */
public final class InMemoryWarDebtStore implements WarDebtStore {

    private final Map<String, Map<String, Double>> debtsByDebtor = new LinkedHashMap<>();

    @Override
    public void recordDebt(String debtorKingdomId, String creditorKingdomId, double amount) {
        if (amount <= 0) {
            return;
        }
        debtsByDebtor
                .computeIfAbsent(debtorKingdomId, ignored -> new LinkedHashMap<>())
                .merge(creditorKingdomId, amount, Double::sum);
    }

    @Override
    public double debtOwed(String debtorKingdomId, String creditorKingdomId) {
        Map<String, Double> creditors = debtsByDebtor.get(debtorKingdomId);
        if (creditors == null) {
            return 0.0;
        }
        return creditors.getOrDefault(creditorKingdomId, 0.0);
    }

    @Override
    public double totalDebtOwed(String debtorKingdomId) {
        Map<String, Double> creditors = debtsByDebtor.get(debtorKingdomId);
        if (creditors == null) {
            return 0.0;
        }
        return creditors.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public double reduceDebt(String debtorKingdomId, String creditorKingdomId, double amount) {
        if (amount <= 0) {
            return 0.0;
        }
        Map<String, Double> creditors = debtsByDebtor.get(debtorKingdomId);
        if (creditors == null) {
            return 0.0;
        }
        double outstanding = creditors.getOrDefault(creditorKingdomId, 0.0);
        if (outstanding <= 0) {
            return 0.0;
        }
        double reduced = Math.min(outstanding, amount);
        double remaining = outstanding - reduced;
        if (remaining <= 0) {
            creditors.remove(creditorKingdomId);
            if (creditors.isEmpty()) {
                debtsByDebtor.remove(debtorKingdomId);
            }
        } else {
            creditors.put(creditorKingdomId, remaining);
        }
        return reduced;
    }

    @Override
    public Collection<WarDebt> allDebtsView() {
        List<WarDebt> debts = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> debtorEntry : debtsByDebtor.entrySet()) {
            for (Map.Entry<String, Double> creditorEntry : debtorEntry.getValue().entrySet()) {
                debts.add(new WarDebt(debtorEntry.getKey(), creditorEntry.getKey(), creditorEntry.getValue()));
            }
        }
        return List.copyOf(debts);
    }
}
