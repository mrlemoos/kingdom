package dev.leo.kingdom.storage;

import dev.leo.kingdom.economy.model.FiscalProposal;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.KingdomEconomy;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.economy.model.TreasuryBudget;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.leo.kingdom.economy.wealth.WealthBlockType;
import dev.leo.kingdom.model.NobleRank;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public YamlEconomyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economyFile = new File(plugin.getDataFolder(), "economy.yml");
    }

    public void loadInto(EconomyService service) {
        if (!economyFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        applyConfiguration(config, service);
    }

    public void saveFrom(EconomyService service) {
        FileConfiguration config = new YamlConfiguration();
        writeConfiguration(config, service);

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

        for (Map.Entry<String, KingdomEconomy> entry : service.kingdomEconomies().entrySet()) {
            String kingdomPath = "kingdoms." + entry.getKey();
            KingdomEconomy economy = entry.getValue();
            config.set(kingdomPath + ".treasury", economy.treasuryBalance());
            config.set(kingdomPath + ".total-tax-revenue", economy.totalTaxRevenue());
            config.set(kingdomPath + ".total-gdp-revenue", economy.totalGdpRevenue());
            config.set(kingdomPath + ".last-daily-gdp", economy.lastDailyGdp());
            writeFiscalRates(config, kingdomPath + ".active-rates", economy.activeRates());
            economy.pendingProposal().ifPresent(proposal -> writeProposal(config, kingdomPath + ".pending-proposal", proposal));
            config.set(kingdomPath + ".budget.approved", economy.budget().approvedAmount());
            config.set(kingdomPath + ".budget.spent", economy.budget().spentAmount());
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

        service.replaceState(wallets, kingdomEconomies);
    }

    private static KingdomEconomy readKingdomEconomy(ConfigurationSection entry) {
        double treasury = entry.getDouble("treasury");
        double totalTaxRevenue = entry.getDouble("total-tax-revenue");
        double totalGdpRevenue = entry.getDouble("total-gdp-revenue");
        double lastDailyGdp = entry.getDouble("last-daily-gdp");
        FiscalRates activeRates = readFiscalRates(entry.getConfigurationSection("active-rates"));
        FiscalProposal pendingProposal = readProposal(entry.getConfigurationSection("pending-proposal"));
        TreasuryBudget budget = readBudget(entry.getConfigurationSection("budget"));
        TerritoryWealthCounts territoryWealthCounts =
                readTerritoryWealthCounts(entry.getConfigurationSection("territory-wealth"));
        List<MintLocation> mints = readMintLocations(entry.getConfigurationSection("mints"));
        return new KingdomEconomy(
                treasury, totalTaxRevenue, totalGdpRevenue, lastDailyGdp, activeRates, pendingProposal, budget, mints, territoryWealthCounts);
    }

    private static void writeFiscalRates(FileConfiguration config, String path, FiscalRates rates) {
        config.set(path + ".base", rates.baseRate());
        config.set(path + ".foreign-surcharge", rates.foreignSurcharge());
        config.set(path + ".transfer-fee", rates.transferFee());
        config.set(path + ".cross-kingdom-transfer-fee", rates.crossKingdomTransferFee());
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
                rankModifiers);
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
                    entry.getString("treasury-lord-uuid")));
        }
        return locations;
    }
}
