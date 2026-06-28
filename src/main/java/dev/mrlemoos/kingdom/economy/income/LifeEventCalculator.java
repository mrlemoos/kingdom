package dev.mrlemoos.kingdom.economy.income;

public final class LifeEventCalculator {

    private final EconomyConfig config;

    public LifeEventCalculator(EconomyConfig config) {
        this.config = config;
    }

    public double calculateSleepReward(boolean inOwnKingdom) {
        return applyOwnKingdomMultiplier(config.sleepReward(), inOwnKingdom);
    }

    public double calculateEatReward(boolean inOwnKingdom) {
        return applyOwnKingdomMultiplier(config.eatReward(), inOwnKingdom);
    }

    public double calculateBuildReward(int blocksPlaced, double buildingEarnedToday, boolean inOwnKingdom) {
        if (blocksPlaced <= 0) {
            return 0.0;
        }
        double remainingCap = Math.max(0.0, config.buildDailyCap() - buildingEarnedToday);
        double baseReward = Math.min(blocksPlaced * config.buildRewardPerBlock(), remainingCap);
        return applyOwnKingdomMultiplier(baseReward, inOwnKingdom);
    }

    public double calculateSocialReward(boolean inOwnKingdom) {
        return applyOwnKingdomMultiplier(config.socialReward(), inOwnKingdom);
    }

    public double applyOwnKingdomMultiplier(double baseAmount, boolean inOwnKingdom) {
        if (baseAmount <= 0.0) {
            return 0.0;
        }
        return inOwnKingdom ? baseAmount * config.ownKingdomLifeEventMultiplier() : baseAmount;
    }
}
