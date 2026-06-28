package dev.mrlemoos.kingdom.command;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CoronaCommand {

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

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("Only players may use Corona commands."));
            return;
        }

        if (args.length == 0 || "balance".equalsIgnoreCase(args[0])) {
            handleBalance(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pay" -> handlePay(player, args);
            case "deposit" -> handleDeposit(player);
            default -> player.sendMessage(help());
        }
    }

    private void handleBalance(Player player) {
        double balance = economyService.getWalletBalance(player.getUniqueId());
        player.sendMessage(info("Your Corona balance: " + c("&f" + formatCorona(balance))));
    }

    private void handlePay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(error("Usage: /corona pay <player> <amount>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(error("Unknown player."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(error("You cannot pay yourself."));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(error("Amount must be positive."));
                return;
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
    }

    private void handleDeposit(Player player) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            player.sendMessage(error("You must join a kingdom to use a mint."));
            return;
        }
        if (!isAtMint(player, membership.get().getKingdomId())) {
            player.sendMessage(error("You must be at a kingdom mint to deposit Coronas."));
            return;
        }

        int nuggetCount = CoronaItem.count(player.getInventory());
        if (nuggetCount <= 0) {
            player.sendMessage(error("You have no Coronas to deposit."));
            return;
        }

        CoronaItem.removeAll(player.getInventory());
        economyService.depositFromNuggets(player.getUniqueId(), nuggetCount);
        economyStore.saveFrom(economyService);
        player.sendMessage(success("Deposited " + nuggetCount + " Corona" + pluralSuffix(nuggetCount) + "."));
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
                + "\n" + c("&e/corona balance")+ "\n" + c("&e/corona pay <player> <amount>")+ "\n" + c("&e/corona deposit")+ "\n" + c("&7Withdraw at the Lord of the Treasury at your mint.");
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
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
