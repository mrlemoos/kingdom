package dev.mrlemoos.kingdom.economy.villager;

public record VillagerTradeEdge(
        String buyerProfession, String sellerProfession, Double spendPercent, Double flatCorona) {

    public VillagerTradeEdge {
        buyerProfession = buyerProfession.toLowerCase();
        sellerProfession = sellerProfession.toLowerCase();
    }

    public static VillagerTradeEdge spendPercent(String buyerProfession, String sellerProfession, double spendPercent) {
        return new VillagerTradeEdge(buyerProfession, sellerProfession, spendPercent, null);
    }

    public static VillagerTradeEdge flatCorona(String buyerProfession, String sellerProfession, double flatCorona) {
        return new VillagerTradeEdge(buyerProfession, sellerProfession, null, flatCorona);
    }

    public boolean isFlatCorona() {
        return flatCorona != null;
    }

    public boolean isSpendPercent() {
        return spendPercent != null;
    }

    public double paymentAmount(double buyerDailyIncome) {
        if (flatCorona != null) {
            return flatCorona;
        }
        if (spendPercent != null) {
            return buyerDailyIncome * spendPercent;
        }
        return 0.0;
    }
}
