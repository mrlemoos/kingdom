package dev.leo.kingdom.economy.model;

public class TreasuryBudget {

    private double approvedAmount;
    private double spentAmount;

    public TreasuryBudget() {
        this(0.0, 0.0);
    }

    public TreasuryBudget(double approvedAmount, double spentAmount) {
        this.approvedAmount = approvedAmount;
        this.spentAmount = spentAmount;
    }

    public double approvedAmount() {
        return approvedAmount;
    }

    public double spentAmount() {
        return spentAmount;
    }

    public void approve(double amount) {
        this.approvedAmount = amount;
        this.spentAmount = 0.0;
    }

    public boolean canSpend(double amount) {
        return amount > 0 && spentAmount + amount <= approvedAmount;
    }

    public void recordSpend(double amount) {
        if (!canSpend(amount)) {
            throw new IllegalArgumentException("Spend exceeds approved treasury budget.");
        }
        spentAmount += amount;
    }
}
