package dev.mrlemoos.kingdom.economy.model;

public record GiltTerms(int faceValue, double couponRate, int maturityDays) {
    public GiltTerms {
        if (faceValue <= 0 || !Double.isFinite(couponRate) || couponRate < 0.0
                || couponRate > 0.01 || maturityDays < 7 || maturityDays > 90) {
            throw new IllegalArgumentException("Gilt needs whole positive face value, 0–1% coupon, and 7–90 day maturity.");
        }
    }
}
