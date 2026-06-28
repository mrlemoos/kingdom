package dev.mrlemoos.kingdom.economy.villager.merchant;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class CoronaMerchantPayment {

    public record Result(int fromNuggets, int fromWallet) {}

    private CoronaMerchantPayment() {}

    public static Optional<Result> collect(
            PlayerInventory inventory, EconomyService economyService, UUID playerId, int coronaPrice) {
        if (inventory == null) {
            return Optional.empty();
        }
        ItemStack[] contents = inventory.getContents();
        Optional<Result> collected = collectContents(contents, economyService, playerId, coronaPrice);
        if (collected.isPresent()) {
            inventory.setContents(contents);
        }
        return collected;
    }

    static Optional<Result> collectContents(
            ItemStack[] contents, EconomyService economyService, UUID playerId, int coronaPrice) {
        if (contents == null || economyService == null || playerId == null || coronaPrice <= 0) {
            return Optional.empty();
        }

        int availableNuggets = CoronaItem.countInContents(contents);
        int fromWallet = Math.max(0, coronaPrice - availableNuggets);
        int walletWhole = (int) Math.floor(economyService.getWalletBalance(playerId));
        if (availableNuggets + walletWhole < coronaPrice) {
            return Optional.empty();
        }

        int fromNuggets = coronaPrice - fromWallet;
        if (!removeNuggets(contents, fromNuggets)) {
            return Optional.empty();
        }
        if (fromWallet > 0 && !economyService.withdrawWholeCorona(playerId, fromWallet)) {
            return Optional.empty();
        }
        return Optional.of(new Result(fromNuggets, fromWallet));
    }

    private static boolean removeNuggets(ItemStack[] contents, int amount) {
        if (amount <= 0) {
            return true;
        }
        int remaining = amount;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!CoronaItem.isCorona(stack)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - remove);
            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            }
            remaining -= remove;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
