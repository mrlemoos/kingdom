package dev.mrlemoos.kingdom.economy.model;

import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KingdomEconomy {

    private double treasuryBalance;
    private double totalTaxRevenue;
    private double totalGdpRevenue;
    private double lastDailyGdp;
    private int lastDayTradesSettled;
    private FiscalRates activeRates;
    private FiscalProposal pendingProposal;
    private final TreasuryBudget budget;
    private final List<MintLocation> mintLocations;
    private final TerritoryWealthCounts territoryWealthCounts;
    private final List<GiltIssue> giltIssues = new ArrayList<>();
    private GiltTerms giltAuthority;
    private long giltAuthorisedDay = -1;
    private long lastGiltSettledDay = -1;
    private boolean giltDefault;
    private final Map<String, EstateHolding> estates = new HashMap<>();
    private final Map<UUID, Double> groundRentArrears = new HashMap<>();
    private final Map<UUID, Double> pendingFinancePayments = new HashMap<>();
    private final Map<UUID, Double> pendingGiltReceipts = new HashMap<>();
    private double totalGroundRentRevenue;
    private long lastFinanceDay = Long.MIN_VALUE;
    private double lastNationalDebt;
    private double lastDebtChange;

    public KingdomEconomy() {
        this(0.0, 0.0, 0.0, 0.0, FiscalRates.defaults(), null, new TreasuryBudget(), new ArrayList<>(), new TerritoryWealthCounts());
    }

    public KingdomEconomy(
            double treasuryBalance,
            FiscalRates activeRates,
            FiscalProposal pendingProposal,
            TreasuryBudget budget,
            List<MintLocation> mintLocations) {
        this(treasuryBalance, 0.0, 0.0, 0.0, activeRates, pendingProposal, budget, mintLocations, new TerritoryWealthCounts());
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
        this(treasuryBalance, totalTaxRevenue, totalGdpRevenue, lastDailyGdp, activeRates, pendingProposal, budget, mintLocations, new TerritoryWealthCounts());
    }

    public KingdomEconomy(
            double treasuryBalance,
            double totalTaxRevenue,
            double totalGdpRevenue,
            double lastDailyGdp,
            FiscalRates activeRates,
            FiscalProposal pendingProposal,
            TreasuryBudget budget,
            List<MintLocation> mintLocations,
            TerritoryWealthCounts territoryWealthCounts) {
        this.treasuryBalance = treasuryBalance;
        this.totalTaxRevenue = totalTaxRevenue;
        this.totalGdpRevenue = totalGdpRevenue;
        this.lastDailyGdp = lastDailyGdp;
        this.lastDayTradesSettled = 0;
        this.activeRates = activeRates;
        this.pendingProposal = pendingProposal;
        this.budget = budget;
        this.mintLocations = new ArrayList<>(mintLocations);
        this.territoryWealthCounts = territoryWealthCounts != null ? territoryWealthCounts : new TerritoryWealthCounts();
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

    public int lastDayTradesSettled() {
        return lastDayTradesSettled;
    }

    public void setLastDayTradesSettled(int lastDayTradesSettled) {
        this.lastDayTradesSettled = Math.max(0, lastDayTradesSettled);
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

    public TerritoryWealthCounts territoryWealthCounts() {
        return territoryWealthCounts;
    }

    public List<GiltIssue> giltIssues() { return List.copyOf(giltIssues); }
    public void addGiltIssue(GiltIssue issue) { giltIssues.add(issue); }
    public GiltTerms giltAuthority() { return giltAuthority; }
    public long giltAuthorisedDay() { return giltAuthorisedDay; }
    public void setGiltAuthority(GiltTerms terms, long day) {
        giltAuthority = terms;
        giltAuthorisedDay = terms == null ? -1 : day;
    }
    public long lastGiltSettledDay() { return lastGiltSettledDay; }
    public void setLastGiltSettledDay(long day) { lastGiltSettledDay = day; }
    public boolean giltDefault() { return giltDefault; }
    public void setGiltDefault(boolean value) { giltDefault = value; }
    public Map<String, EstateHolding> estates() { return Map.copyOf(estates); }
    public void putEstate(EstateHolding holding) { estates.put(holding.key(), holding); }
    public void removeEstate(String key) { estates.remove(key); }
    public Map<UUID, Double> groundRentArrears() { return Map.copyOf(groundRentArrears); }
    public double groundRentArrears(UUID owner) { return groundRentArrears.getOrDefault(owner, 0.0); }
    public void setGroundRentArrears(UUID owner, double amount) {
        if (amount <= 1e-10) groundRentArrears.remove(owner);
        else groundRentArrears.put(owner, amount);
    }
    public double totalGroundRentRevenue() { return totalGroundRentRevenue; }
    public void setTotalGroundRentRevenue(double amount) { totalGroundRentRevenue = amount; }
    public void recordGroundRentRevenue(double amount) { totalGroundRentRevenue += amount; }
    public Map<UUID, Double> pendingFinancePayments() { return Map.copyOf(pendingFinancePayments); }
    public void addFinancePayment(UUID player, double amount) {
        pendingFinancePayments.merge(player, amount, Double::sum);
    }
    public Double takeFinancePayment(UUID player) { return pendingFinancePayments.remove(player); }
    public Map<UUID, Double> pendingGiltReceipts() { return Map.copyOf(pendingGiltReceipts); }
    public void addGiltReceipt(UUID player, double amount) { pendingGiltReceipts.merge(player, amount, Double::sum); }
    public Double takeGiltReceipt(UUID player) { return pendingGiltReceipts.remove(player); }
    public long lastFinanceDay() { return lastFinanceDay; }
    public void setLastFinanceDay(long day) { lastFinanceDay = day; }
    public double lastNationalDebt() { return lastNationalDebt; }
    public void setLastNationalDebt(double amount) { lastNationalDebt = amount; }
    public double lastDebtChange() { return lastDebtChange; }
    public void setLastDebtChange(double amount) { lastDebtChange = amount; }

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
