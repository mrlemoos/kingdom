package dev.leo.kingdom.command;

import dev.leo.kingdom.display.NoblePrefixDisplay;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TitleStyle;
import dev.leo.kingdom.service.KingdomResult;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlKingdomStore;
import dev.leo.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
public final class KingdomCommand implements CommandExecutor, TabCompleter {

    private final KingdomService service;
    private final YamlKingdomStore store;
    private final NoblePrefixDisplay nobleDisplay;
    private final KingdomFiscalHandler fiscalHandler;
    private final EconomyService economyService;

    public KingdomCommand(KingdomService service, YamlKingdomStore store, NoblePrefixDisplay nobleDisplay) {
        this(service, store, nobleDisplay, null, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler) {
        this(service, store, nobleDisplay, fiscalHandler, null);
    }

    public KingdomCommand(
            KingdomService service,
            YamlKingdomStore store,
            NoblePrefixDisplay nobleDisplay,
            KingdomFiscalHandler fiscalHandler,
            EconomyService economyService) {
        this.service = service;
        this.store = store;
        this.nobleDisplay = nobleDisplay;
        this.fiscalHandler = fiscalHandler;
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(help(sender));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
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
            default -> {
                sender.sendMessage(help(sender));
                yield true;
            }
        };
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players can join a kingdom."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom join <kingdom>"));
            return true;
        }

        KingdomResult result = service.joinKingdom(player.getUniqueId(), args[1]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
            refreshDisplayIfOnline(player.getUniqueId());
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<Kingdom> kingdoms = service.listKingdoms().stream()
                .sorted(Comparator.comparing(Kingdom::getDisplayName))
                .toList();
        if (kingdoms.isEmpty()) {
            sender.sendMessage(info("No kingdoms exist yet."));
            return true;
        }
        sender.sendMessage(info("Kingdoms:"));
        for (Kingdom kingdom : kingdoms) {
            long members = service.getMembershipsView().values().stream()
                    .filter(m -> kingdom.getId().equals(m.getKingdomId()))
                    .count();
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + kingdom.getDisplayName()
                    + ChatColor.GRAY + " (" + kingdom.getId() + ", " + members + " members)");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Optional<Kingdom> kingdom = service.getKingdom(args[1]);
            if (kingdom.isPresent()) {
                sendKingdomInfo(sender, kingdom.get());
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target.hasPlayedBefore() || target.isOnline()) {
                sendPlayerInfo(sender, target);
                return true;
            }
            sender.sendMessage(error("Unknown kingdom or player."));
            return true;
        }

        if (sender instanceof Player player) {
            sendPlayerInfo(sender, player);
            return true;
        }

        sender.sendMessage(error("Usage: /kingdom info [kingdom|player]"));
        return true;
    }

    private void sendKingdomInfo(CommandSender sender, Kingdom kingdom) {
        sender.sendMessage(info(kingdom.getDisplayName()));
        service.territoryLabel(kingdom).ifPresent(label ->
                sender.sendMessage(ChatColor.GRAY + "Territory: " + ChatColor.WHITE + label));
        if (economyService != null) {
            String kingdomId = kingdom.getId();
            sender.sendMessage(ChatColor.GRAY + "Tax revenue: "
                    + ChatColor.WHITE + formatCorona(economyService.getTotalTaxRevenue(kingdomId)) + " Corona");
            sender.sendMessage(ChatColor.GRAY + "GDP: "
                    + ChatColor.WHITE + formatCorona(economyService.getLastDailyGdp(kingdomId)) + " Corona/day");
            double totalGdpRevenue = economyService.getTotalGdpRevenue(kingdomId);
            if (totalGdpRevenue > 0.0) {
                sender.sendMessage(ChatColor.GRAY + "Total GDP revenue: "
                        + ChatColor.WHITE + formatCorona(totalGdpRevenue) + " Corona");
            }
        }
        if (sender instanceof Player player
                && kingdom.getWorldGuardRegion() != null
                && service.resolveWorldName(kingdom).equals(player.getWorld().getName())) {
            sender.sendMessage(ChatColor.GRAY + "You are in this kingdom's linked overworld.");
        }
        List<PlayerMembership> members = service.getMembershipsView().values().stream()
                .filter(m -> kingdom.getId().equals(m.getKingdomId()))
                .sorted(Comparator.comparing((PlayerMembership m) -> m.hasNobleTitle() ? 0 : 1)
                        .thenComparing(m -> Optional.ofNullable(m.getRank())
                                .map(NobleRank::hierarchyOrder)
                                .orElse(Integer.MAX_VALUE)))
                .toList();
        for (PlayerMembership membership : members) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(membership.getPlayerId());
            String name = member.getName() != null ? member.getName() : membership.getPlayerId().toString();
            if (membership.hasNobleTitle()) {
                sender.sendMessage(membership.coloredChatPrefix().trim() + ChatColor.WHITE + " " + name);
            } else {
                sender.sendMessage(ChatColor.GRAY + "  " + name);
            }
        }
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
                sender.sendMessage(ChatColor.GRAY + "Territory: " + ChatColor.WHITE + label));
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(error("Usage: /kingdom create <id> [display name]"));
            return true;
        }
        String displayName = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : args[1];
        KingdomResult result = service.createKingdom(args[1], displayName);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return true;
    }

    private boolean handleMove(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom move <player> <kingdom>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        KingdomResult result = service.movePlayer(target.getUniqueId(), args[2]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
            refreshDisplayIfOnline(target.getUniqueId());
        }
        return true;
    }

    private boolean handleTitle(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom title <player> <rank|none> [masculine|feminine]"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if ("none".equalsIgnoreCase(args[2])) {
            KingdomResult result = service.clearTitle(target.getUniqueId());
            sender.sendMessage(format(result));
            if (result instanceof KingdomResult.Success) {
                store.saveFrom(service);
                refreshDisplayIfOnline(target.getUniqueId());
            }
            return true;
        }

        try {
            NobleRank rank = NobleRank.fromCommand(args[2]);
            TitleStyle style = TitleStyle.MASCULINE;
            if (args.length >= 4) {
                style = TitleStyle.fromCommand(args[3]);
            } else if (rank == NobleRank.QUEEN) {
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
        return true;
    }

    private boolean handleSetRegion(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom setregion <kingdom> <region>"));
            return true;
        }
        if (!WorldGuardBridge.isAvailable()) {
            sender.sendMessage(error("WorldGuard is not installed."));
            return true;
        }
        Optional<Kingdom> kingdom = service.getKingdom(args[1]);
        if (kingdom.isEmpty()) {
            sender.sendMessage(error("Unknown kingdom."));
            return true;
        }
        String worldName = service.resolveWorldName(kingdom.get());
        if (Bukkit.getWorld(worldName) == null) {
            sender.sendMessage(error("World '" + worldName + "' is not loaded."));
            return true;
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
            return true;
        }
        KingdomResult result = service.setKingdomRegion(args[1], regionId);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return true;
    }

    private boolean handleFiscal(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return true;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return fiscalHandler.handleFiscal(sender, subArgs);
    }

    private boolean handleBudget(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return true;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return fiscalHandler.handleBudget(sender, subArgs);
    }

    private boolean handleTreasury(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return true;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return fiscalHandler.handleTreasury(sender, subArgs);
    }

    private boolean handleMint(CommandSender sender, String[] args) {
        if (fiscalHandler == null) {
            sender.sendMessage(error("Economy commands are not enabled."));
            return true;
        }
        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return fiscalHandler.handleMint(sender, subArgs);
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(error("Usage: /kingdom setworld <kingdom> <world>"));
            return true;
        }
        KingdomResult result = service.setKingdomWorld(args[1], args[2]);
        sender.sendMessage(format(result));
        if (result instanceof KingdomResult.Success) {
            store.saveFrom(service);
        }
        return true;
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
        builder.append("\n").append(ChatColor.YELLOW).append("/kingdom list");
        builder.append(ChatColor.GRAY).append(" — list realms");
        builder.append("\n").append(ChatColor.YELLOW).append("/kingdom join <name>");
        builder.append(ChatColor.GRAY).append(" — choose your kingdom once");
        builder.append("\n").append(ChatColor.YELLOW).append("/kingdom info [name]");
        builder.append(ChatColor.GRAY).append(" — realm or player details");
        if (fiscalHandler != null) {
            builder.append("\n").append(ChatColor.YELLOW).append("/kingdom fiscal ...");
            builder.append(ChatColor.GRAY).append(" — propose and approve tax rates");
            builder.append("\n").append(ChatColor.YELLOW).append("/kingdom budget ...");
            builder.append(ChatColor.GRAY).append(" — treasury budget");
            builder.append("\n").append(ChatColor.YELLOW).append("/kingdom mint ...");
            builder.append(ChatColor.GRAY).append(" — kingdom mints");
        }
        if (sender.isOp()) {
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom create <id> [display]");
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom move <player> <kingdom>");
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom title <player> <rank|none> [style]");
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom setregion <kingdom> <region>");
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom setworld <kingdom> <world>");
            builder.append("\n").append(ChatColor.GOLD).append("/kingdom treasury credit <kingdom> <amount>");
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
        return ChatColor.GREEN + message;
    }

    private String error(String message) {
        return ChatColor.RED + message;
    }

    private String info(String message) {
        return ChatColor.AQUA + message;
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "list", "info"));
            if (fiscalHandler != null) {
                subs.addAll(List.of("fiscal", "budget", "mint"));
            }
            if (sender.isOp()) {
                subs.addAll(List.of("create", "move", "title", "setregion", "setworld", "treasury"));
            }
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "join", "setregion", "setworld" -> filter(kingdomIds(), args[1]);
                case "move", "title" -> filter(onlineNames(), args[1]);
                case "info" -> {
                    List<String> combined = new ArrayList<>(kingdomIds());
                    combined.addAll(onlineNames());
                    yield filter(combined, args[1]);
                }
                case "fiscal" -> filter(List.of("propose", "approve", "reject", "show"), args[1]);
                case "budget" -> filter(List.of("approve", "spend", "status"), args[1]);
                case "mint" -> filter(List.of("place", "list", "remove"), args[1]);
                case "treasury" -> filter(List.of("credit"), args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && sender.isOp() && "treasury".equals(sub) && "credit".equalsIgnoreCase(args[1])) {
            return filter(kingdomIds(), args[2]);
        }
        if (args.length == 3 && "budget".equals(sub) && "spend".equalsIgnoreCase(args[1])) {
            return filter(onlineNames(), args[2]);
        }
        if (args.length == 3 && sender.isOp()) {
            return switch (sub) {
                case "move" -> filter(kingdomIds(), args[2]);
                case "title" -> {
                    List<String> ranks = new ArrayList<>(NobleRank.commandTokens());
                    ranks.add("none");
                    yield filter(ranks, args[2]);
                }
                case "setworld" -> filter(
                        Bukkit.getWorlds().stream().map(world -> world.getName()).sorted().toList(), args[2]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 4 && sender.isOp() && "title".equals(sub)) {
            return filter(List.of("masculine", "feminine", "duke", "duchess", "lord", "lady", "count", "countess"), args[3]);
        }
        return Collections.emptyList();
    }

    private List<String> kingdomIds() {
        return service.listKingdoms().stream().map(Kingdom::getId).sorted().collect(Collectors.toList());
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
