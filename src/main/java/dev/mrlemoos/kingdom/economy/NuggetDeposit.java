package dev.mrlemoos.kingdom.economy;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.UUID;
import org.bukkit.inventory.PlayerInventory;

/**
 * Every whole Corona nugget a player carries, taken from their inventory and credited to their
 * wallet: the one road for {@code /corona deposit} and the Deposit button at the Lord of the Treasury.
 * The caller checks the player stands at a mint and persists the economy.
 */
public final class NuggetDeposit {

    private NuggetDeposit() {}

    /** The number of Corona deposited; zero, and nothing taken, when none are carried. */
    public static int depositAll(PlayerInventory inventory, EconomyService economyService, UUID playerId) {
        int nuggets = CoronaItem.count(inventory);
        if (nuggets <= 0) {
            return 0;
        }
        CoronaItem.removeAll(inventory);
        economyService.depositFromNuggets(playerId, nuggets);
        return nuggets;
    }
}
