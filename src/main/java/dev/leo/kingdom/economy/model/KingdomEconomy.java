package dev.leo.kingdom.economy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class KingdomEconomy {

    private double treasuryBalance;
    private double totalTaxRevenue;
    private double totalGdpRevenue;
    private double lastDailyGdp;
    private FiscalRates activeRates;
    private FiscalProposal pendingProposal;
    private final TreasuryBudget budget;
    private final List<MintLocation> mintLocations;

    public KingdomEconomy() {
        this(0.0, 0.0, 0.0, 0.0, FiscalRates.defaults(), null, new TreasuryBudget(), new ArrayList<>());
    }

    public KingdomEconomy(
            double treasuryBalance,
            FiscalRates activeRates,
            FiscalProposal pendingProposal,
            TreasuryBudget budget,
            List<MintLocation> mintLocations) {
        this(treasuryBalance, 0.0, 0.0, 0.0, activeRates, pendingProposal, budget, mintLocations);
    }

    public KingdomEconomy(
            double treasuryBalance,
            double totalTaxRevenue,
            double totalGdpRevenue,
            double lastDailyGdp,
            FiscalRates activeRates,
            FiscalProposal pendingProposal,
            TreasuryBudget budget,
            List<MintLocation> mintLocations) {
        this.treasuryBalance = treasuryBalance;
        this.totalTaxRevenue = totalTaxRevenue;
        this.totalGdpRevenue = totalGdpRevenue;
        this.lastDailyGdp = lastDailyGdp;
        this.activeRates = activeRates;
        this.pendingProposal = pendingProposal;
        this.budget = budget;
        this.mintLocations = new ArrayList<>(mintLocations);
    }

    public double treasuryBalance() {
        return treasuryBalance;
    }

    public void setTreasuryBalance(double treasuryBalance) {
        this.treasuryBalance = treasuryBalance;
    }

    public double totalTaxRevenue() {
        return totalTaxRevenue;
    }

    public void recordTaxRevenue(double amount) {
        if (amount > 0) {
            totalTaxRevenue += amount;
        }
    }

    public double totalGdpRevenue() {
        return totalGdpRevenue;
    }

    public void recordGdpRevenue(double amount) {
        if (amount > 0) {
            totalGdpRevenue += amount;
        }
    }

    public double lastDailyGdp() {
        return lastDailyGdp;
    }

    public void setLastDailyGdp(double lastDailyGdp) {
        this.lastDailyGdp = Math.max(0.0, lastDailyGdp);
    }

    public FiscalRates activeRates() {
        return activeRates;
    }

    public void setActiveRates(FiscalRates activeRates) {
        this.activeRates = activeRates;
    }

    public Optional<FiscalProposal> pendingProposal() {
        return Optional.ofNullable(pendingProposal);
    }

    public void setPendingProposal(FiscalProposal pendingProposal) {
        this.pendingProposal = pendingProposal;
    }

    public void clearPendingProposal() {
        this.pendingProposal = null;
    }

    public TreasuryBudget budget() {
        return budget;
    }

    public List<MintLocation> mintLocations() {
        return List.copyOf(mintLocations);
    }

    public void addMintLocation(MintLocation location) {
        mintLocations.add(location);
    }

    public int mintCount() {
        return mintLocations.size();
    }

    public boolean hasMintAt(MintLocation location) {
        return mintLocations.stream().anyMatch(existing -> sameCoordinates(existing, location));
    }

    public void replaceMintLocation(MintLocation location, MintLocation updated) {
        for (int index = 0; index < mintLocations.size(); index++) {
            if (sameCoordinates(mintLocations.get(index), location)) {
                mintLocations.set(index, updated);
                return;
            }
        }
    }

    public Optional<MintLocation> findMintByLordUuid(UUID lordUuid) {
        if (lordUuid == null) {
            return Optional.empty();
        }
        return mintLocations.stream()
                .filter(mint -> mint.lordEntityId().filter(lordUuid::equals).isPresent())
                .findFirst();
    }

    private static boolean sameCoordinates(MintLocation left, MintLocation right) {
        return left.worldName().equals(right.worldName())
                && left.x() == right.x()
                && left.y() == right.y()
                && left.z() == right.z();
    }
}
