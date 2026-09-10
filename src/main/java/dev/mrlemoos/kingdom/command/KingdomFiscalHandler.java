package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.model.FiscalProposal;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyResult;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.mint.RoyalMintPlacementPolicy;
import dev.mrlemoos.kingdom.mint.TreasuryLordManagementPolicy;
import dev.mrlemoos.kingdom.mint.TreasuryLordMintSelector;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.mint.TreasuryLordTargetScan;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomFiscalHandler {

    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final TerritoryResolver territoryResolver;
    private final TreasuryLordService treasuryLordService;
    private final JavaPlugin plugin;
    private MintPrepareGuiOpener mintPrepareGuiOpener;

    /** Opens the mint prepare board; supplied by the parliament GUI listener after construction. */
    @FunctionalInterface
    public interface MintPrepareGuiOpener {
        void open(Player player, String kingdomId, MintLocation location);
    }

    public void setMintPrepareGuiOpener(MintPrepareGuiOpener mintPrepareGuiOpener) {
        this.mintPrepareGuiOpener = mintPrepareGuiOpener;
    }

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
            case "prepare" -> handleMintPrepare(sender);
            case "list" -> handleMintList(sender);
            case "remove" -> handleMintRemove(sender);
            case "despawn" -> handleMintDespawn(sender);
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
        sender.sendMessage(rateLine("Villager wallet interest", active.villagerWalletInterest()));
        sender.sendMessage(rateLine("Tariff", active.tariff()));

        Optional<FiscalProposal> pending = kingdomEconomy(kingdomId).pendingProposal();
        if (pending.isEmpty()) {
            sender.sendMessage(c("&7No fiscal proposal is pending."));
            return true;
        }

        FiscalRates proposed = pending.get().proposedRates();
        sender.sendMessage(info("Pending proposal:"));
        sender.sendMessage(rateLine("Base tax", proposed.baseRate()));
        sender.sendMessage(rateLine("Foreign surcharge", proposed.foreignSurcharge()));
        sender.sendMessage(rateLine("Transfer fee", proposed.transferFee()));
        sender.sendMessage(rateLine("Cross-kingdom transfer fee", proposed.crossKingdomTransferFee()));
        sender.sendMessage(rateLine("Villager wallet interest", proposed.villagerWalletInterest()));
        sender.sendMessage(rateLine("Tariff", proposed.tariff()));
        OfflinePlayer proposer = Bukkit.getOfflinePlayer(pending.get().proposerId());
        String proposerName = proposer.getName() != null ? proposer.getName() : pending.get().proposerId().toString();
        sender.sendMessage(c("&7Proposed by: ")+ c("&f" + proposerName));
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
        sender.sendMessage(c("&7Treasury balance: ")+ c("&f" + formatCorona(treasury)));
        sender.sendMessage(c("&7Approved: ")+ c("&f" + formatCorona(approved)));
        sender.sendMessage(c("&7Spent: ")+ c("&f" + formatCorona(spent)));
        sender.sendMessage(c("&7Remaining: ")+ c("&f" + formatCorona(remaining)));
        return true;
    }

    /** The mint stands where the sender stands, facing the way they face. Empty when refused. */
    private Optional<MintLocation> siteMintWhereStanding(Player player, String kingdomId) {
        Location standing = player.getLocation();
        if (standing.getWorld() == null) {
            player.sendMessage(error("You must be in a loaded world to site a mint."));
            return Optional.empty();
        }
        if (!isInOwnTerritory(standing, kingdomId)) {
            TerritoryLocation territory = territoryResolver.resolve(
                    standing.getWorld().getName(),
                    standing.getBlockX(),
                    standing.getBlockY(),
                    standing.getBlockZ(),
                    kingdomId);
            player.sendMessage(error(mintTerritoryError(kingdomId, standing, territory)));
            return Optional.empty();
        }
        return Optional.of(new MintLocation(
                standing.getWorld().getName(),
                standing.getBlockX(),
                standing.getBlockY(),
                standing.getBlockZ(),
                standing.getYaw(),
                null));
    }

    /** Premier or Crown sites a mint for a SPEND_MINT bill, then the prepare GUI tables it. */
    private boolean handleMintPrepare(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        NobleRank rank = membership.get().getRank();
        if (rank != NobleRank.PREMIER && !RankAuthority.isCrown(rank)) {
            sender.sendMessage(error("Only the Premier, King, or Queen may prepare a mint."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        Optional<MintLocation> sited = siteMintWhereStanding(player.get(), kingdomId);
        if (sited.isEmpty()) {
            return true;
        }
        if (mintPrepareGuiOpener == null) {
            sender.sendMessage(error("The mint prepare board is unavailable."));
            return true;
        }
        mintPrepareGuiOpener.open(player.get(), kingdomId, sited.get());
        return true;
    }

    private boolean handleMintPlace(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!RoyalMintPlacementPolicy.canPlace(membership.get().getRank())) {
            sender.sendMessage(error("Only the King, Queen or a Lord may place a mint."));
            return true;
        }

        Optional<MintLocation> sited = siteMintWhereStanding(player.get(), membership.get().getKingdomId());
        if (sited.isEmpty()) {
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        MintLocation location = sited.get();
        int maxMints = plugin.getConfig().getInt("economy.max-mints-per-kingdom", 3);
        EconomyResult result = economyService.placeRoyalMint(kingdomId, location, maxMints);
        sender.sendMessage(formatEconomy(result));
        if (!(result instanceof EconomyResult.Success)) {
            return true;
        }

        MintLocation withLord = treasuryLordService.ensureLord(kingdomId, location);
        economyStore.saveFrom(economyService);
        sender.sendMessage(success("Lord of the Treasury stationed at "
                + withLord.x() + ", " + withLord.y() + ", " + withLord.z() + "."));
        return true;
    }

    private boolean isInOwnTerritory(Location location, String kingdomId) {
        if (location.getWorld() == null) {
            return false;
        }
        TerritoryLocation territory = territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
        return territory.type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM;
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
            sender.sendMessage(c("&7 - ")+ c("&f" + mint.worldName())
                    + c("&7 @ ")+ mint.x() + ", " + mint.y() + ", " + mint.z());
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

        treasuryLordService.despawnLord(kingdomId, nearest);

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
        economyService.replaceState(economyService.wallets(), economyService.villagerWallets(), kingdomEconomies);
        economyStore.saveFrom(economyService);

        sender.sendMessage(success("Removed mint at " + nearest.x() + ", " + nearest.y() + ", " + nearest.z() + "."));
        return true;
    }

    private boolean handleMintDespawn(CommandSender sender) {
        Optional<Player> player = requirePlayer(sender);
        if (player.isEmpty()) {
            return true;
        }
        Optional<PlayerMembership> membership = requireMembership(player.get());
        if (membership.isEmpty()) {
            return true;
        }
        if (!TreasuryLordManagementPolicy.canDespawn(
                membership.get().getRank(), sender.hasPermission("kingdom.admin"))) {
            sender.sendMessage(error(
                    "Only the King, Queen, a Lord, or an admin may despawn the Lord of the Treasury."));
            return true;
        }

        String kingdomId = membership.get().getKingdomId();
        KingdomEconomy economy = kingdomEconomy(kingdomId);
        List<MintLocation> mints = economy.mintLocations();
        if (mints.isEmpty()) {
            sender.sendMessage(error("Your kingdom has no mints."));
            return true;
        }

        Location playerLoc = player.get().getLocation();
        Optional<MintLocation> mint = resolveMintForDespawn(
                player.get(), kingdomId, mints, playerLoc);
        if (mint.isEmpty()) {
            sender.sendMessage(error("No mints found in this world."));
            return true;
        }

        if (!treasuryLordService.releaseLord(kingdomId, mint.get())) {
            sender.sendMessage(error("Could not despawn the Lord of the Treasury for that mint."));
            return true;
        }

        sender.sendMessage(success("Despawned the Lord of the Treasury at "
                + mint.get().x() + ", " + mint.get().y() + ", " + mint.get().z() + "."));
        return true;
    }

    private Optional<MintLocation> resolveMintForDespawn(
            Player player, String kingdomId, List<MintLocation> mints, Location playerLoc) {
        Optional<Entity> targeted = TreasuryLordTargetScan.targetedEntity(player, 5.0);
        if (targeted.isPresent() && targeted.get() instanceof Villager villager && treasuryLordService.isLordEntity(villager)) {
            Optional<String> lordKingdom = treasuryLordService.kingdomIdForLord(villager);
            if (lordKingdom.filter(kingdomId::equals).isPresent()) {
                Location lordLoc = villager.getLocation();
                Optional<MintLocation> aimed = TreasuryLordMintSelector.forLordAt(
                        mints,
                        lordLoc.getWorld().getName(),
                        lordLoc.getX(),
                        lordLoc.getY(),
                        lordLoc.getZ(),
                        Optional.of(villager.getUniqueId()));
                if (aimed.isPresent()) {
                    return aimed;
                }
            }
        }

        return TreasuryLordMintSelector.nearestInWorld(
                mints, playerLoc.getWorld().getName(), playerLoc.getX(), playerLoc.getY(), playerLoc.getZ());
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
        return c("&6Treasury commands:")+ "\n" + c("&e/kingdom treasury credit <kingdom> <amount>")+ c("&7 — add Corona to a kingdom treasury (admin)");
    }

    private String mintTerritoryError(String kingdomId, Location location, TerritoryLocation territory) {
        List<String> regions = WorldGuardBridge.regionsAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        if (regions.isEmpty()) {
            return "No WorldGuard region where you stand. Stand inside your kingdom's /rg region.";
        }
        if (territory.type() == TerritoryLocation.IncomeLocation.FOREIGN_KINGDOM) {
            String other = territory.kingdomId().orElse("another kingdom");
            return "Where you stand is in " + other + "'s territory, not yours.";
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        String linked = kingdom.flatMap(kingdomService::territoryLabel).orElse("not set");
        return "WorldGuard region '" + regions.get(0) + "' is not linked to your kingdom ("
                + linked + "). Ask an admin to run /kingdom setregion.";
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
                + "\n" + c("&e/kingdom fiscal show")+ "\n" + c("&7 — view active and pending rates (pending via Parliament)");
    }

    private String budgetHelp() {
        return info("Budget commands:")
                + "\n" + c("&e/kingdom budget status")+ "\n" + c("&7 — view approved cap and spending");
    }

    private String mintHelp() {
        return info("Mint commands:")
                + "\n" + c("&e/kingdom mint place")+ c("&7 — place a mint where you stand in your territory (King, Queen or Lord)")+ "\n" + c("&e/kingdom mint list")+ "\n" + c("&e/kingdom mint remove")+ c("&7 — remove the nearest mint (King or Queen)")+ "\n" + c("&e/kingdom mint despawn")+ c("&7 — remove the Lord of the Treasury you are looking at, or at the nearest mint (King, Queen or Lord)")+ "\n" + c("&7 — /kingdom mint prepare — site a mint for a bill (Premier, King, or Queen)");
    }

    private String rateLine(String label, double rate) {
        return c("&7" + label) + ": " + c("&f" + formatPercent(rate));
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
        return c("&a" + message);
    }

    private String error(String message) {
        return c("&c" + message);
    }

    private String info(String message) {
        return c("&b" + message);
    }
}
