package dev.mrlemoos.kingdom.war.tribute;

/**
 * The unpaid remainder of an enacted <b>war tribute</b> after <b>decisive victory</b>, owed by
 * {@code debtorKingdomId} (the defeated kingdom) to {@code creditorKingdomId} (the victor). See
 * the War debt glossary entry in {@code CONTEXT.md}. Persists across peace until paid or
 * superseded by a later Act. Persisted under {@code war-debts} in {@code economy.yml}.
 */
public record WarDebt(String debtorKingdomId, String creditorKingdomId, double amount) {

    public WarDebt {
        if (debtorKingdomId == null || debtorKingdomId.isBlank()) {
            throw new IllegalArgumentException("debtorKingdomId is required.");
        }
        if (creditorKingdomId == null || creditorKingdomId.isBlank()) {
            throw new IllegalArgumentException("creditorKingdomId is required.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("War debt amount must be positive.");
        }
    }
}
