package dev.mrlemoos.kingdom.storage;

import dev.mrlemoos.kingdom.economy.model.FiscalProposal;
import dev.mrlemoos.kingdom.economy.model.EstateHolding;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.GiltIssue;
import dev.mrlemoos.kingdom.economy.model.GiltTerms;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.model.TreasuryBudget;
import dev.mrlemoos.kingdom.economy.model.VillagerWalletState;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.war.tribute.WarDebt;
import dev.mrlemoos.kingdom.war.tribute.WarDebtStore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlEconomyStore {

    private final JavaPlugin plugin;
    private final File economyFile;
    private WarDebtStore warDebtStore;

    public YamlEconomyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economyFile = new File(plugin.getDataFolder(), "economy.yml");
    }

    public void setWarDebtStore(WarDebtStore warDebtStore) {
        this.warDebtStore = warDebtStore;
    }

    public void loadInto(EconomyService service) {
        if (!economyFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        applyConfiguration(config, service);
        if (warDebtStore != null) {
            warDebtStore.replaceAll(readWarDebts(config.getConfigurationSection("war-debts")));
        }
    }

    public void saveFrom(EconomyService service) {
        FileConfiguration config = new YamlConfiguration();
        writeConfiguration(config, service);
        if (warDebtStore != null) {
            writeWarDebts(config, "war-debts", warDebtStore.allDebtsView());
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }
            config.save(economyFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save economy data.", ex);
        }
    }

    static void writeConfiguration(FileConfiguration config, EconomyService service) {
        for (Map.Entry<UUID, Double> entry : service.wallets().entrySet()) {
            config.set("wallets." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Map<UUID, VillagerWalletState>> kingdomEntry : service.villagerWallets().entrySet()) {
            String kingdomPath = "villager-wallets." + kingdomEntry.getKey();
            for (Map.Entry<UUID, VillagerWalletState> walletEntry : kingdomEntry.getValue().entrySet()) {
                String walletPath = kingdomPath + "." + walletEntry.getKey();
                VillagerWalletState wallet = walletEntry.getValue();
                config.set(walletPath + ".balance", wallet.balance());
                wallet.frozenSinceEpochDay()
                        .ifPresent(day -> config.set(walletPath + ".frozen-since-epoch-day", day));
            }
        }

        for (Map.Entry<String, KingdomEconomy> entry : service.kingdomEconomies().entrySet()) {
            String kingdomPath = "kingdoms." + entry.getKey();
            KingdomEconomy economy = entry.getValue();
            config.set(kingdomPath + ".treasury", economy.treasuryBalance());
            config.set(kingdomPath + ".total-tax-revenue", economy.totalTaxRevenue());
            config.set(kingdomPath + ".total-gdp-revenue", economy.totalGdpRevenue());
            config.set(kingdomPath + ".last-daily-gdp", economy.lastDailyGdp());
            config.set(kingdomPath + ".last-day-trades-settled", economy.lastDayTradesSettled());
            if (economy.lastFinanceDay() != Long.MIN_VALUE) {
                config.set(kingdomPath + ".last-finance-day", economy.lastFinanceDay());
            }
            config.set(kingdomPath + ".last-national-debt", economy.lastNationalDebt());
            config.set(kingdomPath + ".last-debt-change", economy.lastDebtChange());
            writeFiscalRates(config, kingdomPath + ".active-rates", economy.activeRates());
            economy.pendingProposal().ifPresent(proposal -> writeProposal(config, kingdomPath + ".pending-proposal", proposal));
            config.set(kingdomPath + ".budget.approved", economy.budget().approvedAmount());
            config.set(kingdomPath + ".budget.spent", economy.budget().spentAmount());
            config.set(kingdomPath + ".gilt.authorised-day", economy.giltAuthorisedDay());
            if (economy.giltAuthority() != null) {
                writeGiltTerms(config, kingdomPath + ".gilt.authority", economy.giltAuthority());
            }
            config.set(kingdomPath + ".gilt.last-settled-day", economy.lastGiltSettledDay());
            config.set(kingdomPath + ".gilt.default", economy.giltDefault());
            config.set(kingdomPath + ".ground-rent.total-revenue", economy.totalGroundRentRevenue());
            economy.groundRentArrears().forEach((debtor, amount) ->
                    config.set(kingdomPath + ".ground-rent.arrears." + debtor, amount));
            economy.pendingFinancePayments().forEach((player, amount) ->
                    config.set(kingdomPath + ".finance-notices.rent-paid." + player, amount));
            economy.pendingGiltReceipts().forEach((player, amount) ->
                    config.set(kingdomPath + ".finance-notices.gilt-received." + player, amount));
            int estateIndex = 0;
            for (EstateHolding holding : economy.estates().values()) {
                String estatePath = kingdomPath + ".ground-rent.estates." + estateIndex++;
                config.set(estatePath + ".world", holding.worldName());
                config.set(estatePath + ".x", holding.x());
                config.set(estatePath + ".y", holding.y());
                config.set(estatePath + ".z", holding.z());
                config.set(estatePath + ".type", holding.type().configKey());
                config.set(estatePath + ".owner", holding.owner().toString());
            }
            int issueIndex = 0;
            for (GiltIssue issue : economy.giltIssues()) {
                String issuePath = kingdomPath + ".gilt.issues." + issueIndex++;
                writeGiltTerms(config, issuePath + ".terms", issue.terms());
                config.set(issuePath + ".monarch", issue.issuingMonarch().toString());
                config.set(issuePath + ".opened-day", issue.openedDay());
                config.set(issuePath + ".closed-day", issue.closedDay());
                issue.holdings().forEach((holder, amount) -> config.set(issuePath + ".holdings." + holder, amount));
                issue.duePrincipal().forEach((holder, amount) -> config.set(issuePath + ".due." + holder, amount));
                issue.payments().forEach((holder, amount) -> config.set(issuePath + ".paid." + holder, amount));
            }
            writeTerritoryWealthCounts(config, kingdomPath + ".territory-wealth", economy.territoryWealthCounts());
            writeMintLocations(config, kingdomPath + ".mints", economy.mintLocations());
        }
    }

    static void applyConfiguration(FileConfiguration config, EconomyService service) {
        Map<UUID, Double> wallets = new HashMap<>();
        ConfigurationSection walletSection = config.getConfigurationSection("wallets");
        if (walletSection != null) {
            for (String uuidString : walletSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    wallets.put(playerId, walletSection.getDouble(uuidString));
                } catch (IllegalArgumentException ex) {
                    // Skip invalid wallet keys.
                }
            }
        }

        Map<String, KingdomEconomy> kingdomEconomies = new HashMap<>();
        ConfigurationSection kingdomSection = config.getConfigurationSection("kingdoms");
        if (kingdomSection != null) {
            for (String kingdomId : kingdomSection.getKeys(false)) {
                ConfigurationSection entry = kingdomSection.getConfigurationSection(kingdomId);
                if (entry == null) {
                    continue;
                }
                kingdomEconomies.put(kingdomId, readKingdomEconomy(entry));
            }
        }

        Map<String, Map<UUID, VillagerWalletState>> villagerWallets = readVillagerWallets(config);

        service.replaceState(wallets, villagerWallets, kingdomEconomies);
    }

    private static Map<String, Map<UUID, VillagerWalletState>> readVillagerWallets(FileConfiguration config) {
        Map<String, Map<UUID, VillagerWalletState>> villagerWallets = new HashMap<>();
        ConfigurationSection root = config.getConfigurationSection("villager-wallets");
        if (root == null) {
            return villagerWallets;
        }
        for (String kingdomId : root.getKeys(false)) {
            ConfigurationSection kingdomSection = root.getConfigurationSection(kingdomId);
            if (kingdomSection == null) {
                continue;
            }
            Map<UUID, VillagerWalletState> kingdomWallets = new HashMap<>();
            for (String villagerIdString : kingdomSection.getKeys(false)) {
                try {
                    UUID villagerId = UUID.fromString(villagerIdString);
                    ConfigurationSection walletSection = kingdomSection.getConfigurationSection(villagerIdString);
                    if (walletSection == null) {
                        continue;
                    }
                    double balance = walletSection.getDouble("balance");
                    Long frozenSince = walletSection.contains("frozen-since-epoch-day")
                            ? walletSection.getLong("frozen-since-epoch-day")
                            : null;
                    kingdomWallets.put(villagerId, new VillagerWalletState(balance, frozenSince));
                } catch (IllegalArgumentException ex) {
                    // Skip invalid villager wallet keys.
                }
            }
            villagerWallets.put(kingdomId, kingdomWallets);
        }
        return villagerWallets;
    }

    private static KingdomEconomy readKingdomEconomy(ConfigurationSection entry) {
        double treasury = entry.getDouble("treasury");
        double totalTaxRevenue = entry.getDouble("total-tax-revenue");
        double totalGdpRevenue = entry.getDouble("total-gdp-revenue");
        double lastDailyGdp = entry.getDouble("last-daily-gdp");
        int lastDayTradesSettled = entry.getInt("last-day-trades-settled");
        FiscalRates activeRates = readFiscalRates(entry.getConfigurationSection("active-rates"));
        FiscalProposal pendingProposal = readProposal(entry.getConfigurationSection("pending-proposal"));
        TreasuryBudget budget = readBudget(entry.getConfigurationSection("budget"));
        TerritoryWealthCounts territoryWealthCounts =
                readTerritoryWealthCounts(entry.getConfigurationSection("territory-wealth"));
        List<MintLocation> mints = readMintLocations(entry.getConfigurationSection("mints"));
        KingdomEconomy economy = new KingdomEconomy(
                treasury, totalTaxRevenue, totalGdpRevenue, lastDailyGdp, activeRates, pendingProposal, budget, mints, territoryWealthCounts);
        economy.setLastDayTradesSettled(lastDayTradesSettled);
        economy.setLastFinanceDay(entry.getLong("last-finance-day", Long.MIN_VALUE));
        economy.setLastNationalDebt(entry.getDouble("last-national-debt"));
        economy.setLastDebtChange(entry.getDouble("last-debt-change"));
        readGiltHoldings(entry.getConfigurationSection("finance-notices.rent-paid"))
                .forEach(economy::addFinancePayment);
        readGiltHoldings(entry.getConfigurationSection("finance-notices.gilt-received"))
                .forEach(economy::addGiltReceipt);
        ConfigurationSection rent = entry.getConfigurationSection("ground-rent");
        if (rent != null) {
            economy.setTotalGroundRentRevenue(rent.getDouble("total-revenue"));
            readGiltHoldings(rent.getConfigurationSection("arrears")).forEach(economy::setGroundRentArrears);
            ConfigurationSection estates = rent.getConfigurationSection("estates");
            if (estates != null) {
                for (String key : estates.getKeys(false)) {
                    ConfigurationSection holding = estates.getConfigurationSection(key);
                    if (holding == null) continue;
                    WealthBlockType.fromConfigKey(holding.getString("type"))
                            .filter(WealthBlockType::isEstate)
                            .ifPresent(type -> economy.putEstate(new EstateHolding(
                                    holding.getString("world"), holding.getInt("x"), holding.getInt("y"),
                                    holding.getInt("z"), type, UUID.fromString(holding.getString("owner")))));
                }
            }
        }
        ConfigurationSection gilt = entry.getConfigurationSection("gilt");
        if (gilt != null) {
            ConfigurationSection authority = gilt.getConfigurationSection("authority");
            economy.setGiltAuthority(authority == null ? null : readGiltTerms(authority), gilt.getLong("authorised-day", -1));
            economy.setLastGiltSettledDay(gilt.getLong("last-settled-day", -1));
            economy.setGiltDefault(gilt.getBoolean("default"));
            ConfigurationSection issues = gilt.getConfigurationSection("issues");
            if (issues != null) {
                for (String key : issues.getKeys(false)) {
                    ConfigurationSection issue = issues.getConfigurationSection(key);
                    if (issue == null || issue.getConfigurationSection("terms") == null) continue;
                    economy.addGiltIssue(new GiltIssue(
                            readGiltTerms(issue.getConfigurationSection("terms")),
                            UUID.fromString(issue.getString("monarch")),
                            issue.getLong("opened-day"),
                            issue.getLong("closed-day", -1),
                            readGiltHoldings(issue.getConfigurationSection("holdings")),
                            readGiltHoldings(issue.getConfigurationSection("due")),
                            readGiltHoldings(issue.getConfigurationSection("paid"))));
                }
            }
        }
        return economy;
    }

    private static void writeGiltTerms(FileConfiguration config, String path, GiltTerms terms) {
        config.set(path + ".face", terms.faceValue());
        config.set(path + ".coupon", terms.couponRate());
        config.set(path + ".maturity-days", terms.maturityDays());
    }

    private static GiltTerms readGiltTerms(ConfigurationSection section) {
        return new GiltTerms(section.getInt("face"), section.getDouble("coupon"), section.getInt("maturity-days"));
    }

    private static Map<UUID, Double> readGiltHoldings(ConfigurationSection section) {
        Map<UUID, Double> holdings = new HashMap<>();
        if (section == null) return holdings;
        for (String key : section.getKeys(false)) {
            try {
                holdings.put(UUID.fromString(key), section.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed old holdings instead of dropping the rest of the economy.
            }
        }
        return holdings;
    }

    private static void writeFiscalRates(FileConfiguration config, String path, FiscalRates rates) {
        config.set(path + ".base", rates.baseRate());
        config.set(path + ".foreign-surcharge", rates.foreignSurcharge());
        config.set(path + ".transfer-fee", rates.transferFee());
        config.set(path + ".cross-kingdom-transfer-fee", rates.crossKingdomTransferFee());
        config.set(path + ".villager-wallet-interest", rates.villagerWalletInterest());
        config.set(path + ".tariff", rates.tariff());
        config.set(path + ".overdraft-interest", rates.overdraftInterest());
        config.set(path + ".ground-rent", rates.groundRent());
        for (Map.Entry<NobleRank, Double> modifier : rates.rankModifiers().entrySet()) {
            config.set(path + ".rank-modifiers." + modifier.getKey().name().toLowerCase(), modifier.getValue());
        }
    }

    private static FiscalRates readFiscalRates(ConfigurationSection section) {
        if (section == null) {
            return FiscalRates.defaults();
        }

        Map<NobleRank, Double> rankModifiers = new EnumMap<>(NobleRank.class);
        ConfigurationSection modifierSection = section.getConfigurationSection("rank-modifiers");
        if (modifierSection != null) {
            for (String rankName : modifierSection.getKeys(false)) {
                try {
                    rankModifiers.put(NobleRank.fromCommand(rankName), modifierSection.getDouble(rankName));
                } catch (IllegalArgumentException ex) {
                    // Skip unknown rank modifiers.
                }
            }
        }

        return new FiscalRates(
                section.getDouble("base", FiscalRates.defaults().baseRate()),
                section.getDouble("foreign-surcharge", FiscalRates.defaults().foreignSurcharge()),
                section.getDouble("transfer-fee", FiscalRates.defaults().transferFee()),
                section.getDouble("cross-kingdom-transfer-fee", FiscalRates.defaults().crossKingdomTransferFee()),
                section.getDouble("villager-wallet-interest", FiscalRates.defaults().villagerWalletInterest()),
                section.getDouble("tariff", FiscalRates.defaults().tariff()),
                rankModifiers,
                section.getDouble("overdraft-interest", FiscalRates.defaults().overdraftInterest()),
                section.getDouble("ground-rent", FiscalRates.defaults().groundRent()));
    }

    private static void writeProposal(FileConfiguration config, String path, FiscalProposal proposal) {
        config.set(path + ".proposer", proposal.proposerId().toString());
        config.set(path + ".timestamp", proposal.timestampMillis());
        writeFiscalRates(config, path + ".rates", proposal.proposedRates());
    }

    private static FiscalProposal readProposal(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String proposerId = section.getString("proposer");
        if (proposerId == null) {
            return null;
        }
        return new FiscalProposal(
                readFiscalRates(section.getConfigurationSection("rates")),
                UUID.fromString(proposerId),
                section.getLong("timestamp"));
    }

    private static TreasuryBudget readBudget(ConfigurationSection section) {
        if (section == null) {
            return new TreasuryBudget();
        }
        return new TreasuryBudget(section.getDouble("approved"), section.getDouble("spent"));
    }

    private static void writeTerritoryWealthCounts(
            FileConfiguration config, String path, TerritoryWealthCounts counts) {
        for (var entry : counts.snapshot().entrySet()) {
            config.set(path + "." + entry.getKey().configKey(), entry.getValue());
        }
    }

    private static TerritoryWealthCounts readTerritoryWealthCounts(ConfigurationSection section) {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        if (section == null) {
            return counts;
        }
        for (String key : section.getKeys(false)) {
            WealthBlockType.fromConfigKey(key).ifPresent(type -> counts.set(type, section.getInt(key)));
        }
        return counts;
    }

    private static void writeMintLocations(FileConfiguration config, String path, List<MintLocation> locations) {
        for (int index = 0; index < locations.size(); index++) {
            MintLocation location = locations.get(index);
            String mintPath = path + "." + index;
            config.set(mintPath + ".world", location.worldName());
            config.set(mintPath + ".x", location.x());
            config.set(mintPath + ".y", location.y());
            config.set(mintPath + ".z", location.z());
            config.set(mintPath + ".yaw", (double) location.yaw());
            if (location.treasuryLordUuid() != null) {
                config.set(mintPath + ".treasury-lord-uuid", location.treasuryLordUuid());
            }
        }
    }

    private static List<MintLocation> readMintLocations(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }

        List<MintLocation> locations = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            if (worldName == null) {
                continue;
            }
            locations.add(new MintLocation(
                    worldName,
                    entry.getInt("x"),
                    entry.getInt("y"),
                    entry.getInt("z"),
                    (float) entry.getDouble("yaw"),
                    entry.getString("treasury-lord-uuid")));
        }
        return locations;
    }

    static void writeWarDebts(FileConfiguration config, String path, Collection<WarDebt> debts) {
        config.set(path, null);
        if (debts == null) {
            return;
        }
        for (WarDebt debt : debts) {
            config.set(path + "." + debt.debtorKingdomId() + "." + debt.creditorKingdomId(), debt.amount());
        }
    }

    static List<WarDebt> readWarDebts(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<WarDebt> debts = new ArrayList<>();
        for (String debtorId : section.getKeys(false)) {
            ConfigurationSection creditors = section.getConfigurationSection(debtorId);
            if (creditors == null) {
                continue;
            }
            for (String creditorId : creditors.getKeys(false)) {
                double amount = creditors.getDouble(creditorId);
                if (amount > 0) {
                    debts.add(new WarDebt(debtorId, creditorId, amount));
                }
            }
        }
        return debts;
    }
}
