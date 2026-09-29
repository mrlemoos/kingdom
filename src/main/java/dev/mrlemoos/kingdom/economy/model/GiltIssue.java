package dev.mrlemoos.kingdom.economy.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One non-transferable sale and its personal holdings. Corona remain fractional after sale. */
public final class GiltIssue {
    private final GiltTerms terms;
    private final UUID issuingMonarch;
    private final long openedDay;
    private long closedDay;
    private final Map<UUID, Double> principal;
    private final Map<UUID, Double> duePrincipal;
    private final Map<UUID, Double> totalPaid;

    public GiltIssue(GiltTerms terms, UUID issuingMonarch, long openedDay) {
        this(terms, issuingMonarch, openedDay, -1L, Map.of(), Map.of());
    }

    public GiltIssue(GiltTerms terms, UUID issuingMonarch, long openedDay, long closedDay,
            Map<UUID, Double> principal, Map<UUID, Double> duePrincipal) {
        this(terms, issuingMonarch, openedDay, closedDay, principal, duePrincipal, Map.of());
    }

    public GiltIssue(GiltTerms terms, UUID issuingMonarch, long openedDay, long closedDay,
            Map<UUID, Double> principal, Map<UUID, Double> duePrincipal, Map<UUID, Double> totalPaid) {
        this.terms = terms;
        this.issuingMonarch = issuingMonarch;
        this.openedDay = openedDay;
        this.closedDay = closedDay;
        this.principal = new HashMap<>(principal);
        this.duePrincipal = new HashMap<>(duePrincipal);
        this.totalPaid = new HashMap<>(totalPaid);
    }

    public GiltTerms terms() { return terms; }
    public UUID issuingMonarch() { return issuingMonarch; }
    public long openedDay() { return openedDay; }
    public long closedDay() { return closedDay; }
    public boolean isOpen() { return closedDay < 0; }
    public long maturityDay() { return closedDay < 0 ? -1 : closedDay + terms.maturityDays(); }
    public Map<UUID, Double> holdings() { return Map.copyOf(principal); }
    public Map<UUID, Double> duePrincipal() { return Map.copyOf(duePrincipal); }
    public double holding(UUID holder) { return principal.getOrDefault(holder, 0.0); }
    public Map<UUID, Double> payments() { return Map.copyOf(totalPaid); }
    public double paid(UUID holder) { return totalPaid.getOrDefault(holder, 0.0); }
    public void recordPayment(UUID holder, double amount) {
        if (amount > 0.0) totalPaid.merge(holder, amount, Double::sum);
    }
    public double principal() { return principal.values().stream().mapToDouble(Double::doubleValue).sum(); }
    public double remainingFace() {
        if (!isOpen()) return 0.0;
        return Math.max(0.0, terms.faceValue() - principal());
    }

    public void buy(UUID buyer, int face) {
        if (!isOpen() || face <= 0 || face > remainingFace()) {
            throw new IllegalArgumentException("Gilt sale cannot fill this purchase.");
        }
        principal.merge(buyer, (double) face, Double::sum);
    }

    public void close(long day) {
        if (!isOpen()) return;
        double gift = remainingFace();
        if (gift > 0.0) principal.merge(issuingMonarch, gift, Double::sum);
        closedDay = day;
    }

    public void capitalise(UUID holder, double amount) {
        if (amount <= 0.0) return;
        principal.merge(holder, amount, Double::sum);
        duePrincipal.merge(holder, amount, Double::sum);
    }

    public void mature(long day) {
        if (closedDay >= 0 && day >= maturityDay()) {
            duePrincipal.clear();
            duePrincipal.putAll(principal);
        }
    }

    public void repay(UUID holder, double amount) {
        if (amount <= 0.0) return;
        principal.computeIfPresent(holder, (id, balance) -> balance - amount <= 1e-10 ? null : balance - amount);
        duePrincipal.computeIfPresent(holder, (id, balance) -> balance - amount <= 1e-10 ? null : balance - amount);
    }
}
