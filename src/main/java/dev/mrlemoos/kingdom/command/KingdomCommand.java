package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.KingdomResult;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KingdomCommand {

    private final KingdomService service;
    private final YamlKingdomStore store;
    private final NoblePrefixDisplay nobleDisplay;
    private final KingdomFiscalHandler fiscalHandler;
    private final EconomyService economyService;
    private final RealmWealthRates realmWealthRates;
    private final ParliamentHandler parliamentHandler;
    private final ElectionHandler electionHandler;
    private final KingdomPoliceHandler policeHandler;
    private final KingdomWhitelistHandler whitelistHandler;
    private final WarService warService;
    private final LoyaltyService loyaltyService;

    public KingdomCommand(KingdomService service, YamlKingdomStore store, NoblePrefixDisplay nobleDisplay) {
        this(service, store, nobleDisplay, null, null, null, null, null, null, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler) {
        this(service, store, nobleDisplay, fiscalHandler, null, null, null, null, null, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, null, null, null, null, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, null, null, null, null,
                null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler,
            ElectionHandler electionHandler) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, electionHandler, null,
                null, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler,
            ElectionHandler electionHandler,
            RealmWealthRates realmWealthRates) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, electionHandler,
                realmWealthRates, null, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler,
            ElectionHandler electionHandler,
            RealmWealthRates realmWealthRates,
            KingdomPoliceHandler policeHandler) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, electionHandler,
                realmWealthRates, policeHandler, null, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler,
            ElectionHandler electionHandler,
            RealmWealthRates realmWealthRates,
            KingdomPoliceHandler policeHandler,
            KingdomWhitelistHandler whitelistHandler) {
        this(service, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, electionHandler,
                realmWealthRates, policeHandler, whitelistHandler, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService,
            ParliamentHandler parliamentHandler,
            ElectionHandler electionHandler,
            RealmWealthRates realmWealthRates,
            KingdomPoliceHandler policeHandler,
            KingdomWhitelistHandler whitelistHandler,
            WarService warService,
            LoyaltyService loyaltyService) {
        this.service = service;
        this.store = store;
        this.nobleDisplay = nobleDisplay;
        this.fiscalHandler = fiscalHandler;
        this.economyService = economyService;
        this.realmWealthRates = realmWealthRates != null ? realmWealthRates : RealmWealthRates.defaults();
        this.parliamentHandler = parliamentHandler;
        this.electionHandler = electionHandler;
        this.policeHandler = policeHandler;
        this.whitelistHandler = whitelistHandler;
        this.warService = warService;
        this.loyaltyService = loyaltyService;
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(help(sender));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> handleJoin(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "move" -> handleMove(sender, args);
            case "title" -> handleTitle(sender, args);
            case "setregion" -> handleSetRegion(sender, args);
            case "setworld" -> handleSetWorld(sender, args);
            case "fiscal" -> handleFiscal(sender, args);
            case "budget" -> handleBudget(sender, args);
            case "mint" -> handleMint(sender, args);
            case "treasury" -> handleTreasury(sender, args);
            case "parliament" -> handleParliament(sender, args);
            case "election" -> handleElection(sender, args);
            case "police" -> handlePolice(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            default -> sender.sendMessage(help(sender));
        }
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can join a kingdom."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom join <kingdom>"));
            return;
        }

        KingdomResult result = service.joinKingdom(player.getUniqueId(), args[1]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
            refreshDisplayIfOnline(player.getUniqueId());
        }
    }

    private void handleList(CommandSender sender) {
        List<Kingdom> kingdoms = service.listKingdoms().stream()
                .sorted(Comparator.comparingDouble((Kingdom kingdom) -> realmWealthFor(kingdom)).reversed()
                        .thenComparing(kingdom -> kingdom.getDisplayName()))
                .toList();
        if (kingdoms.isEmpty()) {
            sender.sendMessage(info("No kingdoms exist yet."));
            return;
        }
        sender.sendMessage(info("Kingdoms (by realm wealth):"));
        for (Kingdom kingdom : kingdoms) {
            long members = service.getMembershipsView().values().stream()
                    .filter(m -> kingdom.getId().equals(m.getKingdomId()))
                    .count();
            String wealthLabel = economyService != null
                    ? c("&7, ")+ c("&f" + formatCorona(realmWealthFor(kingdom)))
                            + c("&7 Corona realm wealth"): "";
            sender.sendMessage(c("&7 - ")+ c("&e" + kingdom.getDisplayName())
                    + c("&7 (")+ kingdom.getId() + ", " + members + " members)" + wealthLabel);
        }
        return;
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Optional<Kingdom> kingdom = service.getKingdom(args[1]);
            if (kingdom.isPresent()) {
                sendKingdomInfo(sender, kingdom.get());
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target.hasPlayedBefore() || target.isOnline()) {
                sendPlayerInfo(sender, target);
                return;
            }
            sender.sendMessage(error("Unknown kingdom or player."));
            return;
        }

        if (sender instanceof Player player) {
            sendPlayerInfo(sender, player);
            return;
        }

        sender.sendMessage(error("Usage: /kingdom info [kingdom|player]"));
        return;
    }

    private void sendKingdomInfo(CommandSender sender, Kingdom kingdom) {
        sender.sendMessage(info(kingdom.getDisplayName()));
        service.territoryLabel(kingdom).ifPresent(label ->
                sender.sendMessage(c("&7Territory: ")+ c("&f" + label)));
        if (economyService != null) {
            String kingdomId = kingdom.getId();
            boolean hasTerritory = kingdom.getWorldGuardRegion() != null
                    && !kingdom.getWorldGuardRegion().isBlank();
            double treasury = economyService.getTreasuryBalance(kingdomId);
            double materialReserves = hasTerritory
                    ? economyService.getMaterialReserveValue(kingdomId, realmWealthRates)
                    : 0.0;
            double estateValue = hasTerritory
                    ? economyService.getEstateValue(kingdomId, realmWealthRates)
                    : 0.0;
            double realmWealth = economyService.getRealmWealth(kingdomId, realmWealthRates);

            sender.sendMessage(c("&7Treasury: ")+ c("&f" + formatCorona(treasury)) + " Corona");
            sender.sendMessage(c("&7Material reserves: ")+ c("&f" + formatCorona(materialReserves)) + " Corona");
            sender.sendMessage(c("&7Estates: ")+ c("&f" + formatCorona(estateValue)) + " Corona");
            sender.sendMessage(c("&7Realm wealth: ")+ c("&f" + formatCorona(realmWealth)) + " Corona");
            if (!hasTerritory) {
                sender.sendMessage(c("&7No territory linked — physical reserves and estates are not counted."));
            }
            sender.sendMessage(c("&7Tax revenue: ")+ c("&f" + formatCorona(economyService.getTotalTaxRevenue(kingdomId))) + " Corona");
            sender.sendMessage(c("&7GDP: ")+ c("&f" + formatCorona(economyService.getLastDailyGdp(kingdomId))) + " Corona/day");
            sender.sendMessage(c("&7Active villager wallets: ")+ c("&f" + formatCorona(economyService.getTotalActiveVillagerWalletBalance(kingdomId)))
                    + " Corona");
            sender.sendMessage(c("&7Villager trades settled (last day): ")+ c("&f" + economyService.getLastDayTradesSettled(kingdomId)));
            double totalGdpRevenue = economyService.getTotalGdpRevenue(kingdomId);
            if (totalGdpRevenue > 0.0) {
                sender.sendMessage(c("&7Total GDP revenue: ")+ c("&f" + formatCorona(totalGdpRevenue)) + " Corona");
            }
        }
        if (sender instanceof Player player
                && kingdom.getWorldGuardRegion() != null
                && service.resolveWorldName(kingdom).equals(player.getWorld().getName())) {
            sender.sendMessage(c("&7You are in this kingdom's linked overworld."));
        }
        sendWarAndPoliceSummary(sender, kingdom);
        List<PlayerMembership> members = service.getMembershipsView().values().stream()
                .filter(m -> kingdom.getId().equals(m.getKingdomId()))
                .sorted(Comparator.comparing((PlayerMembership m) -> m.hasNobleTitle() ? 0 : 1)
                        .thenComparing(m -> {
                            NobleRank rank = m.getRank();
                            return rank != null ? rank.hierarchyOrder() : Integer.MAX_VALUE;
                        }))
                .toList();
        for (PlayerMembership membership : members) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(membership.getPlayerId());
            String name = member.getName() != null ? member.getName() : membership.getPlayerId().toString();
            if (membership.hasNobleTitle()) {
                sender.sendMessage(membership.colouredChatPrefix().trim() + c("&f ")+ name);
            } else {
                sender.sendMessage(c("&7  ")+ name);
            }
        }
    }

    private void sendWarAndPoliceSummary(CommandSender sender, Kingdom kingdom) {
        Optional<ActiveWar> war = warService != null
                ? warService.activeWarFor(kingdom.getId())
                : Optional.empty();
        String warLine = KingdomInfoSummary.warLine(kingdom.getId(), war, this::kingdomDisplayName);
        sender.sendMessage(c("&7" + warLine));

        String policeLine = KingdomInfoSummary.policeLine(kingdom.getPoliceState(), this::offlinePlayerName);
        sender.sendMessage(c("&7" + policeLine));
    }

    private String kingdomDisplayName(String kingdomId) {
        Optional<Kingdom> kingdom = service.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }

    private String offlinePlayerName(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return player.getName();
    }

    private void sendPlayerInfo(CommandSender sender, OfflinePlayer target) {
        Optional<PlayerMembership> membership = service.getMembership(target.getUniqueId());
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        if (membership.isEmpty()) {
            sender.sendMessage(info(name + " has not joined a kingdom."));
            return;
        }
        Kingdom kingdom = service.getKingdom(membership.get().getKingdomId()).orElseThrow();
        String rank = membership.get().hasNobleTitle()
                ? membership.get().chatPrefix().trim()
                : "Citizen";
        sender.sendMessage(info(name + ": " + kingdom.getDisplayName() + " — " + rank));
        service.territoryLabel(kingdom).ifPresent(label ->
                sender.sendMessage(c("&7Territory: ")+ c("&f" + label)));
        if (loyaltyService != null) {
            sender.sendMessage(c("&7" + KingdomInfoSummary.loyaltyLine(loyaltyService.tierOf(target.getUniqueId()))));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom create <id> [display name]"));
            return;
        }
        String displayName = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : args[1];
        KingdomResult result = service.createKingdom(args[1], displayName);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return;
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom move <player> <kingdom>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        KingdomResult result = service.movePlayer(target.getUniqueId(), args[2]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
            refreshDisplayIfOnline(target.getUniqueId());
        }
        return;
    }

    private void handleTitle(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom title <player> <rank|none> [masculine|feminine]"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if ("none".equalsIgnoreCase(args[2])) {
            KingdomResult result = service.clearTitle(target.getUniqueId());
            sender.sendMessage(format(result));
            if (result instanceof KingdomResult.Success) {
                store.saveFrom(service);
                refreshDisplayIfOnline(target.getUniqueId());
            }
            return;
        }

        try {
            String rankArg = args[2];
            NobleRank rank = NobleRank.fromCommand(rankArg);
            TitleStyle style = TitleStyle.MASCULINE;
            if (args.length >= 4) {
                style = TitleStyle.fromCommand(args[3]);
            } else if (rank == NobleRank.QUEEN || "princess".equalsIgnoreCase(rankArg)) {
                style = TitleStyle.FEMININE;
            }
            KingdomResult result = service.assignTitle(target.getUniqueId(), rank, style);
            sender.sendMessage(format(result));
            if (result instanceof KingdomResult.Success) {
                store.saveFrom(service);
                refreshDisplayIfOnline(target.getUniqueId());
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(error(ex.getMessage()));
        }
        return;
    }

    private void handleSetRegion(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom setregion <kingdom> <region>"));
            return;
        }
        if (!WorldGuardBridge.isAvailable()) {
            sender.sendMessage(error("WorldGuard is not installed."));
            return;
        }
        Optional<Kingdom> kingdom = service.getKingdom(args[1]);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return;
        }
        String worldName = service.resolveWorldName(kingdom.get());
        if (Bukkit.getWorld(worldName) == null) {
            sender.sendMessage(error("World '" + worldName + "' is not loaded."));
            return;
        }
        String regionId = Kingdom.normaliseId(args[2]);
        Optional<String> regionWorld = WorldGuardBridge.findWorldContainingRegion(regionId);
        if (regionWorld.isPresent()) {
            if (!regionWorld.get().equals(worldName)) {
                sender.sendMessage(info("Linked kingdom world to '" + regionWorld.get() + "' (region found there)."));
            }
            kingdom.get().setWorldName(regionWorld.get());
            worldName = regionWorld.get();
        }
        if (!WorldGuardBridge.regionExists(worldName, regionId)) {
            if (regionWorld.isEmpty()) {
                sender.sendMessage(error("Region '" + regionId + "' not found in any loaded world. "
                        + "Run /rg list in the overworld and use the exact id."));
            } else {
                sender.sendMessage(error("Region '" + regionId + "' could not be verified in " + worldName + ". "
                        + "Check server console for WorldGuard errors."));
            }
            return;
        }
        KingdomResult result = service.setKingdomRegion(args[1], regionId);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return;
    }

    private void handleFiscal(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        fiscalHandler.handleFiscal(sender, subArgs);
    }

    private void handleBudget(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        fiscalHandler.handleBudget(sender, subArgs);
    }

    private void handleElection(CommandSender sender, String[] args) {
        if (electionHandler == null) {
            sender.sendMessage(error("Elections are not available."));
            return;
        }
        String[] shifted = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        electionHandler.handle(sender, shifted);
    }

    private void handleParliament(CommandSender sender, String[] args) {
        if (parliamentHandler == null) {
            sender.sendMessage(error("Parliament is not available."));
            return;
        }
        String[] shifted = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        parliamentHandler.handle(sender, shifted);
    }

    private void handleTreasury(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        fiscalHandler.handleTreasury(sender, subArgs);
    }

    private void handleMint(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        fiscalHandler.handleMint(sender, subArgs);
    }

    private void handlePolice(CommandSender sender, String[] args) {
        if (policeHandler == null) {
            sender.sendMessage(error("Police commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        policeHandler.handlePolice(sender, subArgs);
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (whitelistHandler == null) {
            sender.sendMessage(error("Whitelist commands are not enabled."));
            return;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        whitelistHandler.handleWhitelist(sender, subArgs);
    }

    private void handleSetWorld(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom setworld <kingdom> <world>"));
            return;
        }
        KingdomResult result = service.setKingdomWorld(args[1], args[2]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return;
    }

    private void refreshDisplayIfOnline(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            nobleDisplay.refresh(online);
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        sender.sendMessage(error("Operators only."));
        return false;
    }

    private String help(CommandSender sender) {
        StringBuilder builder = new StringBuilder(info("Kingdom commands:"));
        builder.append("\n").append(c("&e")).append("/kingdom list");
        builder.append(c("&7")).append(" — list realms");
        builder.append("\n").append(c("&e")).append("/kingdom join <name>");
        builder.append(c("&7")).append(" — choose your kingdom once");
        builder.append("\n").append(c("&e")).append("/kingdom info [name]");
        builder.append(c("&7")).append(" — realm or player details");
        if (fiscalHandler != null) {
            builder.append("\n").append(c("&e")).append("/kingdom election ...");
            builder.append(c("&7")).append(" — MP elections and nominations");
            builder.append("\n").append(c("&e")).append("/kingdom parliament ...");
            builder.append(c("&7")).append(" — table bills, divisions, royal assent");
            builder.append("\n").append(c("&e")).append("/kingdom fiscal show");
            builder.append(c("&7")).append(" — view active fiscal rates");
            builder.append("\n").append(c("&e")).append("/kingdom budget status");
            builder.append(c("&7")).append(" — treasury budget");
            builder.append("\n").append(c("&e")).append("/kingdom mint list");
            builder.append(c("&7")).append(" — kingdom mints");
            builder.append("\n").append(c("&e")).append("/kingdom police status");
            builder.append(c("&7")).append(" — police readiness");
            builder.append("\n").append(c("&e")).append("/kingdom whitelist status");
            builder.append(c("&7")).append(" — server whitelist");
        }
        if (sender.isOp()) {
            builder.append("\n").append(c("&6")).append("/kingdom create <id> [display]");
            builder.append("\n").append(c("&6")).append("/kingdom move <player> <kingdom>");
            builder.append("\n").append(c("&6")).append("/kingdom title <player> <rank|none> [style]");
            builder.append("\n").append(c("&6")).append("/kingdom setregion <kingdom> <region>");
            builder.append("\n").append(c("&6")).append("/kingdom setworld <kingdom> <world>");
            builder.append("\n").append(c("&6")).append("/kingdom treasury credit <kingdom> <amount>");
        }
        return builder.toString();
    }

    private String format(KingdomResult result) {
        return switch (result) {
            case KingdomResult.Success success -> success(success.message());
            case KingdomResult.Failure failure -> error(failure.message());
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

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }

    private double realmWealthFor(Kingdom kingdom) {
        if (economyService == null) {
            return 0.0;
        }
        return economyService.getRealmWealth(kingdom.getId(), realmWealthRates);
    }
}
