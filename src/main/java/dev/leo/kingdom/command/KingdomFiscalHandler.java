package dev.leo.kingdom.command;

import dev.leo.kingdom.economy.model.FiscalProposal;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.KingdomEconomy;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.economy.service.EconomyResult;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.territory.TerritoryLocation;
import dev.leo.kingdom.economy.territory.TerritoryResolver;
import dev.leo.kingdom.mint.TreasuryLordService;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlEconomyStore;
import dev.leo.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomFiscalHandler {

    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final TerritoryResolver territoryResolver;
    private final TreasuryLordService treasuryLordService;
    private final JavaPlugin plugin;

    public KingdomFiscalHandler(
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            TerritoryResolver territoryResolver,
            TreasuryLordService treasuryLordService,
            JavaPlugin plugin) {
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.economyStore = economyStore;
        this.territoryResolver = territoryResolver;
        this.treasuryLordService = treasuryLordService;
        this.plugin = plugin;
    }

    public boolean handleFiscal(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(fiscalHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "propose" -> handleFiscalPropose(sender, args);
            case "approve" -> handleFiscalApprove(sender);
            case "reject" -> handleFiscalReject(sender);
            case "show" -> handleFiscalShow(sender);
            default -> {
                sender.sendMessage(fiscalHelp());
                yield true;
            }
        };
    }

    public boolean handleBudget(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(budgetHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "approve" -> handleBudgetApprove(sender, args);
            case "spend" -> handleBudgetSpend(sender, args);
            case "status" -> handleBudgetStatus(sender);
            default -> {
                sender.sendMessage(budgetHelp());
                yield true;
            }
        };
    }

    public boolean handleMint(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(mintHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "place" -> handleMintPlace(sender);
            case "list" -> handleMintList(sender);
            case "remove" -> handleMintRemove(sender);
            default -> {
                sender.sendMessage(mintHelp());
                yield true;
            }
        };
    }

    private boolean handleFiscalPropose(CommandSender sender, String[] args) {
        sender.sendMessage(error("Fiscal rates must be tabled in Parliament: /kingdom parliament table fiscal ..."));
        return true;
    }

    private boolean handleFiscalApprove(CommandSender sender) {
        sender.sendMessage(error("Fiscal rates require royal assent in Parliament: /kingdom parliament assent"));
        return true;
    }

    private boolean handleFiscalReject(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        NobleRank rank = membership.get().getRank();
        EconomyResult result = economyService.rejectProposal(membership.get().getKingdomId(), rank);
        sender.sendMessage(formatEconomy(result));
        if (result instanceof EconomyResult.Success) {
            economyStore.saveFrom(economyService);
        }
        return true;
    }

    private boolean handleFiscalShow(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        FiscalRates active = activeRates(kingdomId);
        sender.sendMessage(info("Active fiscal rates for your kingdom:"));
        sender.sendMessage(rateLine("Base tax", active.baseRate()));
        sender.sendMessage(rateLine("Foreign surcharge", active.foreignSurcharge()));
        sender.sendMessage(rateLine("Transfer fee", active.transferFee()));
        sender.sendMessage(rateLine("Cross-kingdom transfer fee", active.crossKingdomTransferFee()));

        Optional<FiscalProposal> pending = kingdomEconomy(kingdomId).pendingProposal();
        if (pending.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No fiscal proposal is pending.");
            return true;
        }

        FiscalRates proposed = pending.get().proposedRates();
        sender.sendMessage(info("Pending proposal:"));
        sender.sendMessage(rateLine("Base tax", proposed.baseRate()));
        sender.sendMessage(rateLine("Foreign surcharge", proposed.foreignSurcharge()));
        sender.sendMessage(rateLine("Transfer fee", proposed.transferFee()));
        sender.sendMessage(rateLine("Cross-kingdom transfer fee", proposed.crossKingdomTransferFee()));
        OfflinePlayer proposer = Bukkit.getOfflinePlayer(pending.get().proposerId());
        String proposerName = proposer.getName() != null ? proposer.getName() : pending.get().proposerId().toString();
        sender.sendMessage(ChatColor.GRAY + "Proposed by: " + ChatColor.WHITE + proposerName);
        return true;
    }

    private boolean handleBudgetApprove(CommandSender sender, String[] args) {
        sender.sendMessage(error("Treasury budget must be tabled in Parliament: /kingdom parliament table budget <amount>"));
        return true;
    }

    private boolean handleBudgetSpend(CommandSender sender, String[] args) {
        sender.sendMessage(error("Treasury spending must be tabled in Parliament: /kingdom parliament table spend ..."));
        return true;
    }

    private boolean handleBudgetStatus(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        var budget = kingdomEconomy(membership.get().getKingdomId()).budget();
        double treasury = economyService.getTreasuryBalance(membership.get().getKingdomId());
        double approved = budget.approvedAmount();
        double spent = budget.spentAmount();
        double remaining = approved - spent;
        sender.sendMessage(info("Treasury budget:"));
        sender.sendMessage(ChatColor.GRAY + "Treasury balance: " + ChatColor.WHITE + formatCorona(treasury));
        sender.sendMessage(ChatColor.GRAY + "Approved: " + ChatColor.WHITE + formatCorona(approved));
        sender.sendMessage(ChatColor.GRAY + "Spent: " + ChatColor.WHITE + formatCorona(spent));
        sender.sendMessage(ChatColor.GRAY + "Remaining: " + ChatColor.WHITE + formatCorona(remaining));
        return true;
    }

    private boolean handleMintPlace(CommandSender sender) {
        sender.sendMessage(error("Mint placement requires a supply bill: /kingdom parliament prepare mint then table spend mint"));
        return true;
    }

    public void respawnTreasuryLords() {
        treasuryLordService.respawnAllLords();
    }

    private boolean handleMintList(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }

        List<MintLocation> mints = kingdomEconomy(membership.get().getKingdomId()).mintLocations();
        if (mints.isEmpty()) {
            sender.sendMessage(info("Your kingdom has no mints."));
            return true;
        }
        sender.sendMessage(info("Kingdom mints:"));
        for (MintLocation mint : mints) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + mint.worldName()
                    + ChatColor.GRAY + " @ " + mint.x() + ", " + mint.y() + ", " + mint.z());
        }
        return true;
    }

    private boolean handleMintRemove(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!isRoyal(membership.get().getRank())) {
            sender.sendMessage(error("Only the King or Queen may remove a mint."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        KingdomEconomy economy = kingdomEconomy(kingdomId);
        List<MintLocation> mints = new ArrayList<>(economy.mintLocations());
        if (mints.isEmpty()) {
            sender.sendMessage(error("Your kingdom has no mints to remove."));
            return true;
        }

        Location playerLoc = player.get().getLocation();
        MintLocation nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (MintLocation mint : mints) {
            if (!mint.worldName().equals(playerLoc.getWorld().getName())) {
                continue;
            }
            double distance = squaredDistance(playerLoc, mint);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = mint;
            }
        }

        if (nearest == null) {
            sender.sendMessage(error("No mints found in this world."));
            return true;
        }

        treasuryLordService.despawnLord(nearest);

        mints.remove(nearest);
        KingdomEconomy updated = new KingdomEconomy(
                economy.treasuryBalance(),
                economy.totalTaxRevenue(),
                economy.totalGdpRevenue(),
                economy.lastDailyGdp(),
                economy.activeRates(),
                economy.pendingProposal().orElse(null),
                economy.budget(),
                mints);
        Map<String, KingdomEconomy> kingdomEconomies = new HashMap<>(economyService.kingdomEconomies());
        kingdomEconomies.put(kingdomId, updated);
        economyService.replaceState(economyService.wallets(), kingdomEconomies);
        economyStore.saveFrom(economyService);

        sender.sendMessage(success("Removed mint at " + nearest.x() + ", " + nearest.y() + ", " + nearest.z() + "."));
        return true;
    }

    public boolean handleTreasury(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(treasuryHelp());
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "credit" -> handleTreasuryCredit(sender, args);
            default -> {
                sender.sendMessage(treasuryHelp());
                yield true;
            }
        };
    }

    private boolean handleTreasuryCredit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kingdom.admin")) {
            sender.sendMessage(error("You do not have permission to credit a treasury."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom treasury credit <kingdom> <amount>"));
            return true;
        }
        String kingdomId = Kingdom.normaliseId(args[1]);
        if (kingdomService.getKingdom(kingdomId).isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }
        try {
            double amount = Double.parseDouble(args[2]);
            EconomyResult result = economyService.creditTreasuryAdmin(kingdomId, amount);
            sender.sendMessage(formatEconomy(result));
            if (result instanceof EconomyResult.Success) {
                economyStore.saveFrom(economyService);
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(error("Amount must be a number."));
        }
        return true;
    }

    private String treasuryHelp() {
        return ChatColor.GOLD + "Treasury commands:"
                + "\n" + ChatColor.YELLOW + "/kingdom treasury credit <kingdom> <amount>"
                + ChatColor.GRAY + " — add Corona to a kingdom treasury (admin)";
    }

    private String mintTerritoryError(String kingdomId, Location location, TerritoryLocation territory) {
        List<String> regions = WorldGuardBridge.regionsAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        if (regions.isEmpty()) {
            return "No WorldGuard region at this lectern. Stand inside your kingdom's /rg region.";
        }
        if (territory.type() == TerritoryLocation.IncomeLocation.FOREIGN_KINGDOM) {
            String other = territory.kingdomId().orElse("another kingdom");
            return "This lectern is in " + other + "'s territory, not yours.";
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        String linked = kingdom.flatMap(kingdomService::territoryLabel).orElse("not set");
        return "WorldGuard region '" + regions.get(0) + "' is not linked to your kingdom ("
                + linked + "). Ask an admin to run /kingdom setregion.";
    }

    private Block findLecternBlock(Player player) {
        Block atFeet = player.getLocation().getBlock();
        if (atFeet.getType() == Material.LECTERN) {
            return atFeet;
        }
        Block below = atFeet.getRelative(0, -1, 0);
        if (below.getType() == Material.LECTERN) {
            return below;
        }
        Block target = player.getTargetBlockExact(5);
        if (target != null && target.getType() == Material.LECTERN) {
            return target;
        }
        return null;
    }

    private KingdomEconomy kingdomEconomy(String kingdomId) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        return economy != null ? economy : new KingdomEconomy();
    }

    private FiscalRates activeRates(String kingdomId) {
        return kingdomEconomy(kingdomId).activeRates();
    }

    private static double squaredDistance(Location playerLoc, MintLocation mint) {
        double dx = playerLoc.getX() - mint.x();
        double dy = playerLoc.getY() - mint.y();
        double dz = playerLoc.getZ() - mint.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(error("Only players may use this command."));
        return Optional.empty();
    }

    private Optional<PlayerMembership> requireMembership(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom first."));
            return Optional.empty();
        }
        return membership;
    }

    private static boolean hasRank(PlayerMembership membership, NobleRank required) {
        return membership.getRank() == required;
    }

    private static boolean isRoyal(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    private String fiscalHelp() {
        return info("Fiscal commands:")
                + "\n" + ChatColor.YELLOW + "/kingdom fiscal show"
                + "\n" + ChatColor.GRAY + " — view active and pending rates (pending via Parliament)";
    }

    private String budgetHelp() {
        return info("Budget commands:")
                + "\n" + ChatColor.YELLOW + "/kingdom budget status"
                + "\n" + ChatColor.GRAY + " — view approved cap and spending";
    }

    private String mintHelp() {
        return info("Mint commands:")
                + "\n" + ChatColor.YELLOW + "/kingdom mint list"
                + "\n" + ChatColor.YELLOW + "/kingdom mint remove"
                + "\n" + ChatColor.GRAY + " — placement via /kingdom parliament";
    }

    private String rateLine(String label, double rate) {
        return ChatColor.GRAY + label + ": " + ChatColor.WHITE + formatPercent(rate);
    }

    private static String formatPercent(double rate) {
        return String.format(Locale.UK, "%.1f%%", rate * 100.0);
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }

    private String formatEconomy(EconomyResult result) {
        return switch (result) {
            case EconomyResult.Success success -> success(success.message());
            case EconomyResult.Failure failure -> error(failure.message());
        };
    }

    private String success(String message) {
        return ChatColor.GREEN + message;
    }

    private String error(String message) {
        return ChatColor.RED + message;
    }

    private String info(String message) {
        return ChatColor.AQUA + message;
    }
}
