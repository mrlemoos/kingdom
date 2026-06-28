package dev.mrlemoos.kingdom.economy.service;

import dev.mrlemoos.kingdom.economy.TaxCalculator;
import dev.mrlemoos.kingdom.economy.model.CreditResult;
import dev.mrlemoos.kingdom.economy.model.VillagerWalletState;
import dev.mrlemoos.kingdom.economy.villager.EmeraldVillagerTradeSettlement;
import dev.mrlemoos.kingdom.economy.villager.VillagerIncomeTaxCalculator;
import dev.mrlemoos.kingdom.economy.model.FiscalProposal;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.IncomeLocation;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthCalculator;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EconomyService {

    private final double startingTreasury;
    private final Map<UUID, Double> wallets = new HashMap<>();
    private final Map<String, Map<UUID, VillagerWalletState>> villagerWallets = new HashMap<>();
    private final Map<String, KingdomEconomy> kingdomEconomies = new HashMap<>();

    public EconomyService() {
        this(0.0);
    }

    public EconomyService(double startingTreasury) {
        if (startingTreasury < 0) {
            throw new IllegalArgumentException("Starting treasury cannot be negative.");
        }
        this.startingTreasury = startingTreasury;
    }

    public double getWalletBalance(UUID playerId) {
        return wallets.getOrDefault(playerId, 0.0);
    }

    public double getTreasuryBalance(String kingdomId) {
        return economyFor(kingdomId).treasuryBalance();
    }

    public CreditResult creditWallet(
            UUID playerId,
            double gross,
            IncomeLocation location,
            NobleRank rank,
            String playerKingdomId,
            String incomeKingdomIdOrNull,
            FiscalRates rates,
            double wildernessMultiplier) {
        IncomeLocation resolvedLocation = resolveIncomeLocation(location, playerKingdomId, incomeKingdomIdOrNull);
        CreditResult result = TaxCalculator.calculateCredit(gross, resolvedLocation, rank, rates, wildernessMultiplier);
        wallets.merge(playerId, result.net(), Double::sum);

        if (result.tax() > 0 && playerKingdomId != null) {
            creditTreasury(playerKingdomId, result.tax());
            economyFor(playerKingdomId).recordTaxRevenue(result.tax());
        }

        return result;
    }

    public void creditTreasury(String kingdomId, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Treasury credit cannot be negative.");
        }
        KingdomEconomy economy = economyFor(kingdomId);
        economy.setTreasuryBalance(economy.treasuryBalance() + amount);
    }

    public void recordGdpCredit(String kingdomId, double amount) {
        if (amount <= 0) {
            return;
        }
        creditTreasury(kingdomId, amount);
        economyFor(kingdomId).recordGdpRevenue(amount);
    }

    public void recordVillagerGdpCredited(String kingdomId, double grossAmount) {
        if (grossAmount <= 0) {
            return;
        }
        economyFor(kingdomId).recordGdpRevenue(grossAmount);
    }

    public double getVillagerWalletBalance(String kingdomId, UUID villagerId) {
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId);
        return wallet != null ? wallet.balance() : 0.0;
    }

    public boolean isVillagerWalletFrozen(String kingdomId, UUID villagerId) {
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId);
        return wallet != null && wallet.isFrozen();
    }

    public double getTotalActiveVillagerWalletBalance(String kingdomId) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null) {
            return 0.0;
        }
        double total = 0.0;
        for (VillagerWalletState wallet : kingdomWallets.values()) {
            if (!wallet.isFrozen()) {
                total += wallet.balance();
            }
        }
        return total;
    }

    public void creditVillagerGdp(String kingdomId, UUID villagerId, double gross) {
        if (gross <= 0) {
            return;
        }
        double baseRate = economyFor(kingdomId).activeRates().baseRate();
        CreditResult result = VillagerIncomeTaxCalculator.calculateCredit(gross, baseRate);
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId, true);
        wallet.setBalance(wallet.balance() + result.net());
        wallet.markActive();
        if (result.tax() > 0) {
            creditTreasury(kingdomId, result.tax());
            economyFor(kingdomId).recordTaxRevenue(result.tax());
        }
        recordVillagerGdpCredited(kingdomId, gross);
    }

    public void creditVillagerWalletDirect(String kingdomId, UUID villagerId, double amount) {
        if (amount <= 0) {
            return;
        }
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId, true);
        wallet.setBalance(wallet.balance() + amount);
        wallet.markActive();
    }

    public boolean creditEmeraldVillagerCommerce(
            String kingdomId, UUID villagerId, EmeraldVillagerTradeSettlement settlement) {
        if (kingdomId == null
                || kingdomId.isBlank()
                || villagerId == null
                || settlement == null
                || settlement.grossCorona() <= 0.0) {
            return false;
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (settlement.commerceTax() > 0) {
            creditTreasury(kingdomId, settlement.commerceTax());
            economy.recordTaxRevenue(settlement.commerceTax());
        }

        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId);
        if (wallet != null && !wallet.isFrozen()) {
            wallet.setBalance(wallet.balance() + settlement.netCorona());
            return true;
        }

        if (settlement.netCorona() > 0) {
            creditTreasury(kingdomId, settlement.netCorona());
        }
        return true;
    }

    public boolean settleVillagerTrade(
            String kingdomId, UUID buyerId, UUID sellerId, double payment, double commerceTaxRate) {
        if (payment <= 0) {
            return false;
        }
        VillagerWalletState buyer = villagerWalletFor(kingdomId, buyerId);
        if (buyer == null || buyer.isFrozen() || buyer.balance() < payment) {
            return false;
        }
        double tax = payment * Math.clamp(commerceTaxRate, 0.0, 1.0);
        double sellerReceives = payment - tax;
        buyer.setBalance(buyer.balance() - payment);
        VillagerWalletState seller = villagerWalletFor(kingdomId, sellerId, true);
        seller.setBalance(seller.balance() + sellerReceives);
        if (tax > 0) {
            creditTreasury(kingdomId, tax);
            economyFor(kingdomId).recordTaxRevenue(tax);
        }
        return true;
    }

    public void syncVillagerWalletActivity(String kingdomId, Set<UUID> activeVillagerIds, long epochDay) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null) {
            return;
        }
        for (Map.Entry<UUID, VillagerWalletState> entry : kingdomWallets.entrySet()) {
            if (activeVillagerIds.contains(entry.getKey())) {
                entry.getValue().markActive();
            } else if (entry.getValue().balance() > 0.0) {
                entry.getValue().markFrozen(epochDay);
            }
        }
    }

    public double escheatFrozenWallets(String kingdomId, long currentEpochDay, int escheatMcDays) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null || escheatMcDays <= 0) {
            return 0.0;
        }
        double escheated = 0.0;
        var iterator = kingdomWallets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, VillagerWalletState> entry = iterator.next();
            VillagerWalletState wallet = entry.getValue();
            if (!wallet.isFrozen()) {
                continue;
            }
            long frozenSince = wallet.frozenSinceEpochDay().orElse(currentEpochDay);
            if (currentEpochDay - frozenSince < escheatMcDays) {
                continue;
            }
            double balance = wallet.balance();
            if (balance > 0) {
                creditTreasury(kingdomId, balance);
                escheated += balance;
            }
            iterator.remove();
        }
        return escheated;
    }

    public void setLastDayTradesSettled(String kingdomId, int count) {
        economyFor(kingdomId).setLastDayTradesSettled(count);
    }

    public int getLastDayTradesSettled(String kingdomId) {
        return economyFor(kingdomId).lastDayTradesSettled();
    }

    public void setLastDailyGdp(String kingdomId, double amount) {
        economyFor(kingdomId).setLastDailyGdp(amount);
    }

    public double getLastDailyGdp(String kingdomId) {
        return economyFor(kingdomId).lastDailyGdp();
    }

    public double getTotalTaxRevenue(String kingdomId) {
        return economyFor(kingdomId).totalTaxRevenue();
    }

    public double getTotalGdpRevenue(String kingdomId) {
        return economyFor(kingdomId).totalGdpRevenue();
    }

    public TerritoryWealthCounts getTerritoryWealthCounts(String kingdomId) {
        return economyFor(kingdomId).territoryWealthCounts();
    }

    public void adjustTerritoryWealthBlock(String kingdomId, WealthBlockType blockType, int delta) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type is required.");
        }
        economyFor(kingdomId).territoryWealthCounts().adjust(blockType, delta);
    }

    public void replaceTerritoryWealthCounts(String kingdomId, TerritoryWealthCounts counts) {
        if (counts == null) {
            throw new IllegalArgumentException("Territory wealth counts are required.");
        }
        economyFor(kingdomId).territoryWealthCounts().replaceFrom(counts);
    }

    public double getMaterialReserveValue(String kingdomId, RealmWealthRates rates) {
        return RealmWealthCalculator.materialReserveValue(economyFor(kingdomId).territoryWealthCounts(), rates);
    }

    public double getEstateValue(String kingdomId, RealmWealthRates rates) {
        return RealmWealthCalculator.estateValue(economyFor(kingdomId).territoryWealthCounts(), rates);
    }

    public double getRealmWealth(String kingdomId, RealmWealthRates rates) {
        KingdomEconomy economy = economyFor(kingdomId);
        return RealmWealthCalculator.realmWealth(
                economy.treasuryBalance(),
                economy.territoryWealthCounts(),
                getTotalActiveVillagerWalletBalance(kingdomId),
                rates);
    }

    public void creditWalletDirect(UUID playerId, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Direct wallet credit cannot be negative.");
        }
        if (amount > 0) {
            wallets.merge(playerId, amount, Double::sum);
        }
    }

    public TransferResult transferCorona(
            UUID from,
            UUID to,
            double amount,
            String fromKingdom,
            String toKingdom,
            FiscalRates rates) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (getWalletBalance(from) < amount) {
            throw new IllegalArgumentException("Insufficient Corona in sender wallet.");
        }

        double feeRate = sameKingdom(fromKingdom, toKingdom) ? rates.transferFee() : rates.crossKingdomTransferFee();
        double fee = amount * feeRate;
        double received = amount - fee;

        wallets.merge(from, -amount, Double::sum);
        wallets.merge(to, received, Double::sum);

        if (fee > 0 && fromKingdom != null) {
            creditTreasury(fromKingdom, fee);
        }

        return new TransferResult(amount, received, fee);
    }

    public void depositFromNuggets(UUID playerId, int nuggetCount) {
        if (nuggetCount <= 0) {
            throw new IllegalArgumentException("Deposit requires at least one whole Corona nugget.");
        }
        wallets.merge(playerId, (double) nuggetCount, Double::sum);
    }

    public int withdrawWholeNuggets(UUID playerId) {
        double balance = getWalletBalance(playerId);
        int wholeNuggets = (int) Math.floor(balance);
        if (wholeNuggets > 0) {
            wallets.put(playerId, balance - wholeNuggets);
        }
        return wholeNuggets;
    }

    public boolean withdrawWholeCorona(UUID playerId, int amount) {
        if (amount <= 0) {
            return false;
        }
        double balance = getWalletBalance(playerId);
        if (balance < amount) {
            return false;
        }
        wallets.put(playerId, balance - amount);
        return true;
    }

    public EconomyResult submitProposal(
            String kingdomId, NobleRank proposerRank, UUID proposerId, FiscalRates proposedRates) {
        if (proposerRank != NobleRank.PREMIER) {
            return EconomyResult.fail("Only the Premier may submit a fiscal proposal.");
        }
        if (proposedRates == null) {
            return EconomyResult.fail("Proposed fiscal rates are required.");
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (economy.pendingProposal().isPresent()) {
            return EconomyResult.fail("A fiscal proposal is already pending approval.");
        }

        economy.setPendingProposal(new FiscalProposal(proposedRates, proposerId, System.currentTimeMillis()));
        return EconomyResult.ok("Fiscal proposal submitted.");
    }

    public EconomyResult approveProposal(String kingdomId, NobleRank approverRank) {
        return EconomyResult.fail("Fiscal rates must pass through Parliament. Use /kingdom parliament assent.");
    }

    public EconomyResult rejectProposal(String kingdomId, NobleRank approverRank) {
        if (approverRank != NobleRank.KING && approverRank != NobleRank.QUEEN) {
            return EconomyResult.fail("Only the King or Queen may reject a fiscal proposal.");
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (economy.pendingProposal().isEmpty()) {
            return EconomyResult.fail("No fiscal proposal is pending approval.");
        }

        economy.clearPendingProposal();
        return EconomyResult.ok("Fiscal proposal rejected.");
    }

    public EconomyResult applyFiscalRates(String kingdomId, FiscalRates rates) {
        if (rates == null) {
            return EconomyResult.fail("Fiscal rates are required.");
        }
        KingdomEconomy economy = economyFor(kingdomId);
        economy.setActiveRates(rates);
        economy.clearPendingProposal();
        return EconomyResult.ok("Fiscal rates enacted.");
    }

    public EconomyResult approveBudget(String kingdomId, double amount) {
        return EconomyResult.fail("Treasury budget must pass through Parliament. Use /kingdom parliament assent.");
    }

    public EconomyResult enactBudget(String kingdomId, double amount) {
        if (amount < 0) {
            return EconomyResult.fail("Approved budget cannot be negative.");
        }
        economyFor(kingdomId).budget().approve(amount);
        return EconomyResult.ok("Treasury budget enacted.");
    }

    public EconomyResult spendFromBudget(String kingdomId, double amount) {
        if (amount <= 0) {
            return EconomyResult.fail("Spend amount must be positive.");
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (!economy.budget().canSpend(amount)) {
            return EconomyResult.fail("Spend exceeds approved treasury budget.");
        }
        double treasury = economy.treasuryBalance();
        if (treasury < amount) {
            return EconomyResult.fail("Insufficient Corona in kingdom treasury (have "
                    + formatCorona(treasury) + ", need " + formatCorona(amount)
                    + "). Treasury grows from tax and villager GDP.");
        }

        economy.setTreasuryBalance(economy.treasuryBalance() - amount);
        economy.budget().recordSpend(amount);
        return EconomyResult.ok("Treasury spend recorded.");
    }

    public EconomyResult placeMint(String kingdomId, MintLocation location, double cost, int maxMints) {
        if (location == null) {
            return EconomyResult.fail("Mint location is required.");
        }
        if (cost < 0) {
            return EconomyResult.fail("Mint placement cost cannot be negative.");
        }
        if (maxMints < 0) {
            return EconomyResult.fail("Maximum mint count cannot be negative.");
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (economy.mintCount() >= maxMints) {
            return EconomyResult.fail("Kingdom has reached its mint limit.");
        }
        if (economy.hasMintAt(location)) {
            return EconomyResult.fail("A mint already exists at that location.");
        }

        EconomyResult spendResult = spendFromBudget(kingdomId, cost);
        if (spendResult instanceof EconomyResult.Failure) {
            return spendResult;
        }

        economy.addMintLocation(location);
        return EconomyResult.ok("Mint placed.");
    }

    public EconomyResult placeRoyalMint(String kingdomId, MintLocation location, int maxMints) {
        if (location == null) {
            return EconomyResult.fail("Mint location is required.");
        }
        if (maxMints < 0) {
            return EconomyResult.fail("Maximum mint count cannot be negative.");
        }

        KingdomEconomy economy = economyFor(kingdomId);
        if (economy.mintCount() >= maxMints) {
            return EconomyResult.fail("Kingdom has reached its mint limit.");
        }
        if (economy.hasMintAt(location)) {
            return EconomyResult.fail("A mint already exists at that location.");
        }

        economy.addMintLocation(location);
        return EconomyResult.ok("Mint placed.");
    }

    public EconomyResult creditTreasuryAdmin(String kingdomId, double amount) {
        if (amount <= 0) {
            return EconomyResult.fail("Treasury credit must be positive.");
        }
        creditTreasury(kingdomId, amount);
        return EconomyResult.ok("Credited " + formatCorona(amount) + " Corona to kingdom treasury.");
    }

    public void replaceState(
            Map<UUID, Double> newWallets,
            Map<String, Map<UUID, VillagerWalletState>> newVillagerWallets,
            Map<String, KingdomEconomy> newKingdomEconomies) {
        wallets.clear();
        wallets.putAll(newWallets);
        villagerWallets.clear();
        for (Map.Entry<String, Map<UUID, VillagerWalletState>> entry : newVillagerWallets.entrySet()) {
            villagerWallets.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        kingdomEconomies.clear();
        kingdomEconomies.putAll(newKingdomEconomies);
    }

    public Map<UUID, Double> wallets() {
        return Map.copyOf(wallets);
    }

    public Map<String, Map<UUID, VillagerWalletState>> villagerWallets() {
        Map<String, Map<UUID, VillagerWalletState>> copy = new HashMap<>();
        for (Map.Entry<String, Map<UUID, VillagerWalletState>> entry : villagerWallets.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    public Map<String, KingdomEconomy> kingdomEconomies() {
        return Map.copyOf(kingdomEconomies);
    }

    private KingdomEconomy economyFor(String kingdomId) {
        return kingdomEconomies.computeIfAbsent(kingdomId, ignored -> {
            KingdomEconomy economy = new KingdomEconomy();
            if (startingTreasury > 0) {
                economy.setTreasuryBalance(startingTreasury);
            }
            return economy;
        });
    }

    private VillagerWalletState villagerWalletFor(String kingdomId, UUID villagerId) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null) {
            return null;
        }
        return kingdomWallets.get(villagerId);
    }

    private VillagerWalletState villagerWalletFor(String kingdomId, UUID villagerId, boolean create) {
        if (!create) {
            return villagerWalletFor(kingdomId, villagerId);
        }
        return villagerWallets
                .computeIfAbsent(kingdomId, ignored -> new HashMap<>())
                .computeIfAbsent(villagerId, ignored -> new VillagerWalletState());
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(java.util.Locale.UK, "%.0f", amount);
        }
        return String.format(java.util.Locale.UK, "%.2f", amount);
    }

    private static IncomeLocation resolveIncomeLocation(
            IncomeLocation location, String playerKingdomId, String incomeKingdomIdOrNull) {
        if (location == IncomeLocation.WILDERNESS) {
            return IncomeLocation.WILDERNESS;
        }
        if (playerKingdomId == null || incomeKingdomIdOrNull == null) {
            return location;
        }
        if (playerKingdomId.equals(incomeKingdomIdOrNull)) {
            return IncomeLocation.OWN_KINGDOM;
        }
        return IncomeLocation.FOREIGN_KINGDOM;
    }

    private static boolean sameKingdom(String fromKingdom, String toKingdom) {
        return fromKingdom != null && fromKingdom.equals(toKingdom);
    }
}
