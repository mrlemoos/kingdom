package dev.mrlemoos.kingdom.economy.service;

import dev.mrlemoos.kingdom.economy.TaxCalculator;
import dev.mrlemoos.kingdom.economy.model.CreditResult;
import dev.mrlemoos.kingdom.economy.model.VillagerWalletState;
import dev.mrlemoos.kingdom.economy.villager.EmeraldVillagerTradeSettlement;
import dev.mrlemoos.kingdom.economy.villager.VillagerIncomeTaxCalculator;
import dev.mrlemoos.kingdom.economy.model.FiscalProposal;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.EstateHolding;
import dev.mrlemoos.kingdom.economy.model.GiltIssue;
import dev.mrlemoos.kingdom.economy.model.GiltTerms;
import dev.mrlemoos.kingdom.economy.model.IncomeLocation;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.wealth.EstateBlockPlacer;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthCalculator;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.math.BigDecimal;

public class EconomyService {

    private final double startingTreasury;
    private LoyaltyService loyaltyService;
    private LongSupplier currentMcDay;
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

    /**
     * Optional hook (nullable, mirroring the war domain's setters) so paying income tax counts as
     * an act of service and brings a subject's loyalty recovery forward. Unset, tax is tax.
     */
    public void setServiceCreditHook(LoyaltyService loyaltyService, LongSupplier currentMcDay) {
        this.loyaltyService = loyaltyService;
        this.currentMcDay = currentMcDay;
    }

    public double getWalletBalance(UUID playerId) {
        return wallets.getOrDefault(playerId, 0.0);
    }

    public double getTreasuryBalance(String kingdomId) {
        return economyFor(kingdomId).treasuryBalance();
    }

    public double getOverdraft(String kingdomId) {
        return Math.max(0.0, -getTreasuryBalance(kingdomId));
    }

    public double getNationalDebt(String kingdomId) {
        return getOverdraft(kingdomId) + getGiltPrincipal(kingdomId);
    }

    public double getGiltPrincipal(String kingdomId) {
        return economyFor(kingdomId).giltIssues().stream().mapToDouble(GiltIssue::principal).sum();
    }

    public double getGiltHolding(String kingdomId, UUID holder) {
        return economyFor(kingdomId).giltIssues().stream().mapToDouble(issue -> issue.holding(holder)).sum();
    }

    public boolean isGiltDefault(String kingdomId) {
        return economyFor(kingdomId).giltDefault();
    }

    /** Removes one persisted, combined settlement notice after its player can read it. */
    public String takeFinanceSummary(String kingdomId, UUID player) {
        KingdomEconomy economy = kingdomEconomies.get(kingdomId);
        if (economy == null) return null;
        Double rentPaid = economy.takeFinancePayment(player);
        Double giltReceived = economy.takeGiltReceipt(player);
        if (rentPaid == null && giltReceived == null) return null;
        List<String> details = new ArrayList<>();
        if (rentPaid != null) {
            details.add("ground rent paid " + money(rentPaid) + ", arrears "
                    + money(economy.groundRentArrears(player))
                    + "; visit the Lord Mayor to pay arrears or reapply for a permit");
        }
        if (giltReceived != null) {
            double due = economy.giltIssues().stream()
                    .mapToDouble(issue -> issue.duePrincipal().getOrDefault(player, 0.0)).sum();
            details.add("gilt payments received " + money(giltReceived) + ", overdue principal "
                    + money(due) + "; visit the issuing mint to inspect your holding");
        }
        return "Finance settlement for " + kingdomId + ": " + String.join("; ", details) + ".";
    }

    private static String money(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString() + " Corona";
    }

    public void recordEstatePlacement(String kingdomId, String world, int x, int y, int z,
            WealthBlockType type, UUID owner) {
        if (type != null && type.isEstate() && owner != null) {
            economyFor(kingdomId).putEstate(new EstateHolding(world, x, y, z, type, owner));
        }
    }

    public void removeEstate(String kingdomId, String world, int x, int y, int z) {
        economyFor(kingdomId).removeEstate(EstateHolding.key(world, x, y, z));
    }

    public void releaseEstateOwner(String kingdomId, UUID owner) {
        KingdomEconomy economy = economyFor(kingdomId);
        for (EstateHolding holding : economy.estates().values()) {
            if (holding.owner().equals(owner)) economy.removeEstate(holding.key());
        }
    }

    public boolean isOwnedEstate(String kingdomId, String world, int x, int y, int z, UUID owner) {
        EstateHolding holding = economyFor(kingdomId).estates().get(EstateHolding.key(world, x, y, z));
        return holding != null && holding.owner().equals(owner);
    }

    public double getGroundRentArrears(String kingdomId, UUID owner) {
        return economyFor(kingdomId).groundRentArrears(owner);
    }

    public double getTotalGroundRentArrears(String kingdomId) {
        return economyFor(kingdomId).groundRentArrears().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double payGroundRentArrears(String kingdomId, UUID owner) {
        KingdomEconomy economy = economyFor(kingdomId);
        double paid = Math.min(economy.groundRentArrears(owner), getWalletBalance(owner));
        if (paid > 0.0) {
            debitWallet(owner, paid);
            creditTreasury(kingdomId, paid);
            economy.recordGroundRentRevenue(paid);
            economy.setGroundRentArrears(owner, economy.groundRentArrears(owner) - paid);
        }
        if (paid > 0.0) economy.addFinancePayment(owner, paid);
        return paid;
    }

    /** Call after player income tax. Absent owners lose future rent, not earlier arrears. */
    public Map<UUID, Double> settleGroundRent(String kingdomId, boolean hasCapital,
            Map<UUID, NobleRank> members, RealmWealthRates values) {
        KingdomEconomy economy = economyFor(kingdomId);
        Map<UUID, Double> newArrears = new HashMap<>();
        for (UUID debtor : economy.groundRentArrears().keySet()) {
            if (members.containsKey(debtor)) payGroundRentArrears(kingdomId, debtor);
        }
        for (EstateHolding holding : economy.estates().values()) {
            NobleRank rank = members.get(holding.owner());
            if (!members.containsKey(holding.owner())) {
                economy.removeEstate(holding.key());
                continue;
            }
            if (!hasCapital || rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE) {
                continue;
            }
            double due = values.coronaValue(holding.type()) * economy.activeRates().groundRent();
            if (due <= 0.0) continue;
            double paid = Math.min(due, getWalletBalance(holding.owner()));
            economy.addFinancePayment(holding.owner(), paid);
            if (paid > 0.0) {
                debitWallet(holding.owner(), paid);
                creditTreasury(kingdomId, paid);
                economy.recordGroundRentRevenue(paid);
            }
            double shortfall = due - paid;
            if (shortfall > 0.0) {
                economy.setGroundRentArrears(holding.owner(), economy.groundRentArrears(holding.owner()) + shortfall);
                newArrears.merge(holding.owner(), shortfall, Double::sum);
            }
        }
        return Map.copyOf(newArrears);
    }

    public EconomyResult openGiltSale(String kingdomId, UUID monarch, long realmDay) {
        KingdomEconomy economy = economyFor(kingdomId);
        GiltTerms terms = economy.giltAuthority();
        if (terms == null || realmDay > economy.giltAuthorisedDay() + 7) {
            return EconomyResult.fail("Gilt authority has lapsed. Parliament must pass a new BUDGET Act.");
        }
        if (economy.mintCount() == 0) {
            return EconomyResult.fail("Site a mint before opening a gilt sale.");
        }
        if (economy.giltIssues().stream().anyMatch(GiltIssue::isOpen)) {
            return EconomyResult.fail("A gilt sale is already open.");
        }
        economy.addGiltIssue(new GiltIssue(terms, monarch, realmDay));
        economy.setGiltAuthority(null, -1);
        return EconomyResult.ok("Gilt sale opened. Unsold face will be gifted to the issuing monarch when it closes.");
    }

    public EconomyResult buyGilt(String kingdomId, UUID buyer, int face, long realmDay) {
        KingdomEconomy economy = economyFor(kingdomId);
        GiltIssue open = economy.giltIssues().stream().filter(GiltIssue::isOpen).findFirst().orElse(null);
        if (open == null || realmDay >= open.openedDay() + 7) {
            return EconomyResult.fail("No gilt sale is open at this mint.");
        }
        if (face <= 0 || face > open.remainingFace() || getWalletBalance(buyer) < face) {
            return EconomyResult.fail("Choose a whole Corona amount within the remaining face and your wallet.");
        }
        debitWallet(buyer, face);
        open.buy(buyer, face);
        creditTreasury(kingdomId, face);
        if (open.remainingFace() == 0.0) open.close(realmDay);
        return EconomyResult.ok("Bought " + face + " Corona of gilts.");
    }

    /** Pay current coupons before overdue principal, pro rata across all issues and holders. */
    public boolean settleGilts(String kingdomId, long realmDay) {
        KingdomEconomy economy = economyFor(kingdomId);
        if (realmDay <= economy.lastGiltSettledDay()) return false;
        boolean previouslyDefaulted = economy.giltDefault();
        for (GiltIssue issue : economy.giltIssues()) {
            if (issue.isOpen() && realmDay >= issue.openedDay() + 7) issue.close(issue.openedDay() + 7);
        }
        List<GiltPayment> coupons = new ArrayList<>();
        double couponTotal = 0.0;
        for (GiltIssue issue : economy.giltIssues()) {
            if (issue.isOpen() || realmDay <= issue.closedDay()) continue;
            for (var holding : issue.holdings().entrySet()) {
                double coupon = holding.getValue() * issue.terms().couponRate();
                if (coupon > 0.0) {
                    coupons.add(new GiltPayment(issue, holding.getKey(), coupon));
                    couponTotal += coupon;
                }
            }
        }
        double couponScale = couponTotal == 0.0 ? 1.0 : Math.min(1.0, Math.max(0.0, economy.treasuryBalance()) / couponTotal);
        for (GiltPayment payment : coupons) {
            double paid = debitTreasury(kingdomId, payment.amount() * couponScale);
            if (paid > 0.0) {
                creditWalletDirect(payment.holder(), paid);
                economy.addGiltReceipt(payment.holder(), paid);
                payment.issue().recordPayment(payment.holder(), paid);
            }
            payment.issue().capitalise(payment.holder(), payment.amount() - paid);
            if (paid == 0.0) economy.addGiltReceipt(payment.holder(), 0.0);
        }
        List<GiltPayment> principals = new ArrayList<>();
        double principalTotal = 0.0;
        for (GiltIssue issue : economy.giltIssues()) {
            issue.mature(realmDay);
            for (var due : issue.duePrincipal().entrySet()) {
                principals.add(new GiltPayment(issue, due.getKey(), due.getValue()));
                principalTotal += due.getValue();
            }
        }
        double principalScale = principalTotal == 0.0 ? 1.0 : Math.min(1.0, Math.max(0.0, economy.treasuryBalance()) / principalTotal);
        for (GiltPayment payment : principals) {
            double paid = debitTreasury(kingdomId, payment.amount() * principalScale);
            if (paid > 0.0) {
                creditWalletDirect(payment.holder(), paid);
                payment.issue().repay(payment.holder(), paid);
                payment.issue().recordPayment(payment.holder(), paid);
            }
            economy.addGiltReceipt(payment.holder(), paid);
        }
        economy.setGiltDefault(economy.giltIssues().stream().anyMatch(issue -> !issue.duePrincipal().isEmpty()));
        economy.setLastGiltSettledDay(realmDay);
        return !previouslyDefaulted && economy.giltDefault();
    }

    private record GiltPayment(GiltIssue issue, UUID holder, double amount) {}

    public void closeDebtDay(String kingdomId) {
        KingdomEconomy economy = economyFor(kingdomId);
        if (economy.treasuryBalance() < 0.0) {
            economy.setTreasuryBalance(economy.treasuryBalance()
                    * (1.0 + economy.activeRates().overdraftInterest()));
        }
    }

    public void finishFinanceDay(String kingdomId, long realmDay) {
        KingdomEconomy economy = economyFor(kingdomId);
        if (realmDay <= economy.lastFinanceDay()) return;
        closeDebtDay(kingdomId);
        double total = getNationalDebt(kingdomId);
        economy.setLastDebtChange(total - economy.lastNationalDebt());
        economy.setLastNationalDebt(total);
        economy.setLastFinanceDay(realmDay);
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
            // A subject who pays their tax while out of favour earns service credit; the service
            // itself refuses the credit at Faithful, Traitor, or with no clock running.
            if (loyaltyService != null && currentMcDay != null) {
                loyaltyService.recordServiceCredit(playerId, currentMcDay.getAsLong());
            }
        }

        return result;
    }

    public void creditTreasury(String kingdomId, double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Treasury credit cannot be negative.");
        }
        KingdomEconomy economy = economyFor(kingdomId);
        economy.setTreasuryBalance(economy.treasuryBalance() + amount);
    }

    public void creditTreasuryFromPlayerTax(String kingdomId, double amount) {
        if (amount <= 0.0) {
            return;
        }
        creditTreasury(kingdomId, amount);
        economyFor(kingdomId).recordTaxRevenue(amount);
    }

    /**
     * Debits Corona from a kingdom treasury for an unbudgeted transfer such as a war tribute,
     * transferring only what is available rather than failing outright. Callers such as {@link
     * dev.mrlemoos.kingdom.war.tribute.WarTributeService} treat any shortfall (requested amount
     * minus the returned transferred amount) as a debt owed by the kingdom.
     *
     * @return the Corona actually debited, which may be less than {@code amount} (or {@code 0.0})
     *     when the treasury balance is insufficient
     */
    public double debitTreasury(String kingdomId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return 0.0;
        }
        KingdomEconomy economy = economyFor(kingdomId);
        double available = Math.max(0.0, economy.treasuryBalance());
        double transferred = Math.min(available, amount);
        if (transferred > 0) {
            economy.setTreasuryBalance(available - transferred);
        }
        return transferred;
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

    public Optional<Long> getVillagerWalletFrozenSince(String kingdomId, UUID villagerId) {
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId);
        return wallet != null ? wallet.frozenSinceEpochDay() : Optional.empty();
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

    public CreditResult creditVillagerGdp(String kingdomId, UUID villagerId, double gross) {
        if (gross <= 0) {
            return new CreditResult(0.0, 0.0, 0.0, 1.0);
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
        return result;
    }

    public void creditVillagerWalletDirect(String kingdomId, UUID villagerId, double amount) {
        if (amount <= 0) {
            return;
        }
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId, true);
        wallet.setBalance(wallet.balance() + amount);
        wallet.markActive();
    }

    /**
     * Applies the enacted villager wallet interest rate to productive (non-frozen) wallets.
     * Positive credits come from the treasury (untaxed, pro-rata on shortfall); negative charges
     * wallets into the treasury without reducing any wallet below zero.
     */
    public void applyVillagerWalletInterest(String kingdomId) {
        KingdomEconomy economy = economyFor(kingdomId);
        double rate = economy.activeRates().villagerWalletInterest();
        if (rate == 0.0) {
            return;
        }

        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null || kingdomWallets.isEmpty()) {
            return;
        }

        if (rate > 0.0) {
            applyPositiveVillagerWalletInterest(kingdomId, economy, kingdomWallets, rate);
        } else {
            applyNegativeVillagerWalletInterest(kingdomId, kingdomWallets, rate);
        }
    }

    private void applyPositiveVillagerWalletInterest(
            String kingdomId,
            KingdomEconomy economy,
            Map<UUID, VillagerWalletState> kingdomWallets,
            double rate) {
        Map<UUID, Double> dueByVillager = new HashMap<>();
        double totalDue = 0.0;
        for (Map.Entry<UUID, VillagerWalletState> entry : kingdomWallets.entrySet()) {
            VillagerWalletState wallet = entry.getValue();
            if (wallet.isFrozen() || wallet.balance() <= 0.0) {
                continue;
            }
            double due = wallet.balance() * rate;
            if (due <= 0.0) {
                continue;
            }
            dueByVillager.put(entry.getKey(), due);
            totalDue += due;
        }
        if (totalDue <= 0.0) {
            return;
        }

        double available = Math.min(Math.max(0.0, economy.treasuryBalance()), totalDue);
        if (available <= 0.0) {
            return;
        }
        economy.setTreasuryBalance(economy.treasuryBalance() - available);
        double scale = available / totalDue;
        for (Map.Entry<UUID, Double> entry : dueByVillager.entrySet()) {
            creditVillagerWalletDirect(kingdomId, entry.getKey(), entry.getValue() * scale);
        }
    }

    private void applyNegativeVillagerWalletInterest(
            String kingdomId, Map<UUID, VillagerWalletState> kingdomWallets, double rate) {
        double charged = 0.0;
        for (VillagerWalletState wallet : kingdomWallets.values()) {
            if (wallet.isFrozen() || wallet.balance() <= 0.0) {
                continue;
            }
            double charge = Math.min(wallet.balance(), wallet.balance() * Math.abs(rate));
            if (charge <= 0.0) {
                continue;
            }
            wallet.setBalance(wallet.balance() - charge);
            charged += charge;
        }
        if (charged > 0.0) {
            creditTreasury(kingdomId, charged);
        }
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
        syncVillagerWalletActivity(kingdomId, activeVillagerIds, Set.copyOf(kingdomWallets.keySet()), epochDay);
    }

    /** Unloaded villagers are unknown, not unproductive. Only observed villagers may newly freeze. */
    public void syncVillagerWalletActivity(
            String kingdomId, Set<UUID> activeVillagerIds, Set<UUID> observedVillagerIds, long epochDay) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null) {
            return;
        }
        for (Map.Entry<UUID, VillagerWalletState> entry : kingdomWallets.entrySet()) {
            if (activeVillagerIds.contains(entry.getKey())) {
                entry.getValue().markActive();
            } else if (observedVillagerIds.contains(entry.getKey()) && entry.getValue().balance() > 0.0) {
                entry.getValue().markFrozen(epochDay);
            }
        }
    }

    /** A known death freezes its wallet immediately when no church takes the estate. */
    public void freezeVillagerWallet(String kingdomId, UUID villagerId, long epochDay) {
        VillagerWalletState wallet = villagerWalletFor(kingdomId, villagerId);
        if (wallet != null && wallet.balance() > 0.0) {
            wallet.markFrozen(epochDay);
        }
    }

    /**
     * Takes a villager's wallet off the ledger and hands back what stood in it — used when the
     * villager dies and its estate passes to the church's keeping.
     */
    public double takeVillagerWallet(String kingdomId, UUID villagerId) {
        Map<UUID, VillagerWalletState> kingdomWallets = villagerWallets.get(kingdomId);
        if (kingdomWallets == null || villagerId == null) {
            return 0.0;
        }
        VillagerWalletState wallet = kingdomWallets.remove(villagerId);
        return wallet == null ? 0.0 : wallet.balance();
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
                rates) - getGiltPrincipal(kingdomId);
    }

    public void creditWalletDirect(UUID playerId, double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Direct wallet credit cannot be negative.");
        }
        if (amount > 0) {
            wallets.merge(playerId, amount, Double::sum);
        }
    }

    /**
     * Debits Corona from a player wallet. Returns false if the balance is insufficient.
     */
    public boolean debitWallet(UUID playerId, double amount) {
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
        return enactBudget(kingdomId, amount, null, -1L);
    }

    public EconomyResult enactBudget(String kingdomId, double amount, GiltTerms giltTerms, long realmDay) {
        if (!Double.isFinite(amount) || amount < 0) {
            return EconomyResult.fail("Approved budget cannot be negative.");
        }
        if (giltTerms != null && giltTerms.faceValue() > amount) {
            return EconomyResult.fail("Gilt face value cannot exceed this Act's spending allowance.");
        }
        KingdomEconomy economy = economyFor(kingdomId);
        economy.budget().approve(amount);
        economy.setGiltAuthority(giltTerms, realmDay);
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

    public EconomyResult placePublicWork(
            String kingdomId,
            String worldName,
            int x,
            int y,
            int z,
            WealthBlockType estateType,
            double cost,
            EstateBlockPlacer placer) {
        if (estateType == null || !estateType.isEstate()) {
            return EconomyResult.fail("Public work type must be an estate block (beacon, conduit, or lodestone).");
        }
        if (worldName == null || worldName.isBlank()) {
            return EconomyResult.fail("Public work location is required.");
        }
        if (cost < 0) {
            return EconomyResult.fail("Public work cost cannot be negative.");
        }
        if (placer == null) {
            return EconomyResult.fail("Estate block placer is required.");
        }

        EconomyResult spendResult = spendFromBudget(kingdomId, cost);
        if (spendResult instanceof EconomyResult.Failure) {
            return spendResult;
        }

        if (!placer.place(worldName, x, y, z, estateType)) {
            creditTreasury(kingdomId, cost);
            economyFor(kingdomId).budget().reverseSpend(cost);
            return EconomyResult.fail("Could not place the estate block.");
        }
        return EconomyResult.ok("Public work placed.");
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
