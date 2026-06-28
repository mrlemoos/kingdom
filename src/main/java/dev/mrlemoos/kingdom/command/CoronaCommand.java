package dev.mrlemoos.kingdom.command;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.income.ActivityCategory;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.service.TransferResult;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CoronaCommand implements CommandExecutor, TabCompleter {

    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final EconomyCoordinator economyCoordinator;

    public CoronaCommand(
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            EconomyCoordinator economyCoordinator) {
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.economyStore = economyStore;
        this.economyCoordinator = economyCoordinator;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use Corona commands."));
            return true;
        }

        if (args.length == 0 || "balance".equalsIgnoreCase(args[0])) {
            return handleBalance(player);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pay" -> handlePay(player, args);
            case "deposit" -> handleDeposit(player);
            default -> {
                player.sendMessage(help());
                yield true;
            }
        };
    }

    private boolean handleBalance(Player player) {
        double balance = economyService.getWalletBalance(player.getUniqueId());
        player.sendMessage(info("Your Corona balance: " + ChatColor.WHITE + formatCorona(balance)));
        return true;
    }

    private boolean handlePay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(error("Usage: /corona pay <player> <amount>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(error("Unknown player."));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(error("You cannot pay yourself."));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(error("Amount must be positive."));
                return true;
            }

            String fromKingdom = kingdomService.getMembership(player.getUniqueId())
                    .map(PlayerMembership::getKingdomId)
                    .orElse(null);
            String toKingdom = kingdomService.getMembership(target.getUniqueId())
                    .map(PlayerMembership::getKingdomId)
                    .orElse(null);
            FiscalRates rates = fromKingdom != null
                    ? activeRates(fromKingdom)
                    : FiscalRates.defaults();

            TransferResult result = economyService.transferCorona(
                    player.getUniqueId(),
                    target.getUniqueId(),
                    amount,
                    fromKingdom,
                    toKingdom,
                    rates);

            grantPlayerTradeBonus(player.getUniqueId());
            if (target.isOnline()) {
                grantPlayerTradeBonus(target.getUniqueId());
            }

            economyStore.saveFrom(economyService);

            String targetName = target.getName() != null ? target.getName() : args[1];
            player.sendMessage(success("Paid " + formatCorona(amount) + " Corona to " + targetName
                    + " (fee " + formatCorona(result.fee()) + ")."));
            if (target.isOnline()) {
                target.getPlayer().sendMessage(info("You received " + formatCorona(result.amountReceived()) + " Corona."));
            }
        } catch (NumberFormatException ex) {
            player.sendMessage(error("Amount must be a number."));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(error(ex.getMessage()));
        }
        return true;
    }

    private boolean handleDeposit(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom to use a mint."));
            return true;
        }
        if (!isAtMint(player, membership.get().getKingdomId())) {
            player.sendMessage(error("You must be at a kingdom mint to deposit Coronas."));
            return true;
        }

        int nuggetCount = CoronaItem.count(player.getInventory());
        if (nuggetCount <= 0) {
            player.sendMessage(error("You have no Coronas to deposit."));
            return true;
        }

        CoronaItem.removeAll(player.getInventory());
        economyService.depositFromNuggets(player.getUniqueId(), nuggetCount);
        economyStore.saveFrom(economyService);
        player.sendMessage(success("Deposited " + nuggetCount + " Corona" + pluralSuffix(nuggetCount) + "."));
        return true;
    }

    private boolean isAtMint(Player player, String kingdomId) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        if (economy == null) {
            return false;
        }
        List<MintLocation> mints = economy.mintLocations();
        if (mints.isEmpty()) {
            return false;
        }

        for (Block block : mintCandidateBlocks(player)) {
            String world = block.getWorld().getName();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            for (MintLocation mint : mints) {
                if (mint.worldName().equals(world) && mint.x() == x && mint.y() == y && mint.z() == z) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Block> mintCandidateBlocks(Player player) {
        List<Block> blocks = new ArrayList<>();
        Block feet = player.getLocation().getBlock();
        blocks.add(feet);
        blocks.add(feet.getRelative(0, -1, 0));
        Block target = player.getTargetBlockExact(5);
        if (target != null) {
            blocks.add(target);
        }
        return blocks;
    }

    private void grantPlayerTradeBonus(UUID playerId) {
        double bonus = economyCoordinator.config().playerTradeBonus();
        if (bonus <= 0.0) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        if (!economyCoordinator.activityCooldownTracker().canEarn(ActivityCategory.PLAYER_TRADE, playerId, nowMs)) {
            return;
        }
        economyService.creditWalletDirect(playerId, bonus);
        economyCoordinator.activityCooldownTracker().record(ActivityCategory.PLAYER_TRADE, playerId, nowMs);
    }

    private FiscalRates activeRates(String kingdomId) {
        KingdomEconomy economy = economyService.kingdomEconomies().get(kingdomId);
        return economy != null ? economy.activeRates() : FiscalRates.defaults();
    }

    private static String pluralSuffix(int count) {
        return count == 1 ? "" : "s";
    }

    private String help() {
        return info("Corona commands:")
                + "\n" + ChatColor.YELLOW + "/corona balance"
                + "\n" + ChatColor.YELLOW + "/corona pay <player> <amount>"
                + "\n" + ChatColor.YELLOW + "/corona deposit"
                + "\n" + ChatColor.GRAY + "Withdraw at the Lord of the Treasury at your mint.";
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("balance", "pay", "deposit"), args[0]);
        }
        if (args.length == 2 && "pay".equalsIgnoreCase(args[0])) {
            return filter(onlineNames(), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    private List<String> filter(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
