package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantPayment;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantRecipeFactory;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantRecipeService;
import dev.mrlemoos.kingdom.election.TerritoryVillagerCommercePolicy;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.election.VillagerPlayerTradePolicy;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

public final class CoronaMerchantListener implements Listener {

    private final EconomyCoordinator coordinator;
    private final VillagerMpEntityService villagerMpEntityService;
    private final KingdomTerritoryResolver territoryResolver;
    private final CoronaMerchantRecipeService recipeService;

    public CoronaMerchantListener(
            EconomyCoordinator coordinator,
            VillagerMpEntityService villagerMpEntityService,
            KingdomTerritoryResolver territoryResolver,
            CoronaMerchantRecipeService recipeService) {
        this.coordinator = coordinator;
        this.villagerMpEntityService = villagerMpEntityService;
        this.territoryResolver = territoryResolver;
        this.recipeService = recipeService;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractVillager(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!isEligibleTerritoryVillager(villager)) {
            return;
        }
        boolean onStrike = isOnStrike(villager);
        if (!VillagerPlayerTradePolicy.canTradeWithPlayers(villager, onStrike)) {
            event.setCancelled(true);
            recipeService.refreshRecipes(villager, onStrike);
            return;
        }
        recipeService.refreshRecipes(villager, onStrike);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (!isEligibleTerritoryVillager(villager)) {
            return;
        }
        recipeService.refreshRecipes(villager, isOnStrike(villager));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCoronaMerchantWalletFallback(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getSlotType() != SlotType.RESULT) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() != InventoryType.MERCHANT) {
            return;
        }
        if (event.getAction() == InventoryAction.NOTHING) {
            return;
        }
        if (event.getClick() == ClickType.WINDOW_BORDER_LEFT
                || event.getClick() == ClickType.WINDOW_BORDER_RIGHT) {
            return;
        }

        MerchantInventory merchantInventory = (MerchantInventory) event.getView().getTopInventory();
        MerchantRecipe selected = merchantInventory.getSelectedRecipe();
        if (!CoronaMerchantRecipeFactory.isCoronaRecipe(selected)) {
            return;
        }
        if (!(merchantInventory.getMerchant() instanceof Villager villager)
                || !isEligibleTerritoryVillager(villager)
                || !VillagerPlayerTradePolicy.canTradeWithPlayers(villager, isOnStrike(villager))) {
            return;
        }

        int coronaPrice = CoronaMerchantRecipeFactory.coronaPrice(selected);
        if (coronaPrice <= 0) {
            return;
        }
        if (CoronaItem.count(player.getInventory()) >= coronaPrice) {
            return;
        }
        int remainingUses = recipeService.remainingUses(villager, selected);
        if (remainingUses <= 0) {
            event.setCancelled(true);
            player.sendMessage(c("&cThat trade is out of stock."));
            return;
        }

        event.setCancelled(true);
        ItemStack result = selected.getResult().clone();
        int available = CoronaItem.count(player.getInventory())
                + (int) Math.floor(coordinator.economyService().getWalletBalance(player.getUniqueId()));
        int affordable = available / coronaPrice;
        if (affordable <= 0) {
            player.sendMessage(c("&cYou need ") + coronaPrice + " Corona to buy that.");
            return;
        }
        int fits = CoronaMerchantPayment.spaceForResults(player.getInventory().getStorageContents(), result);
        if (fits <= 0) {
            player.sendMessage(c("&cMake inventory space for that trade."));
            return;
        }
        int trades = Math.min(event.isShiftClick() ? remainingUses : 1, Math.min(affordable, fits));

        Optional<CoronaMerchantPayment.Result> payment = CoronaMerchantPayment.collect(
                player.getInventory(), coordinator.economyService(), player.getUniqueId(), coronaPrice * trades);
        if (payment.isEmpty()) {
            player.sendMessage(c("&cYou need ") + coronaPrice + " Corona to buy that.");
            return;
        }

        for (int given = 0; given < trades; given++) {
            player.getInventory().addItem(result.clone());
        }
        recipeService.consumeUses(villager, selected, trades);
        coordinator.settleCoronaMerchantCommerce(
                villager,
                coronaPrice * trades,
                villagerMpEntityService.isTreasuryLordVillager(villager),
                villagerMpEntityService.isSeatedMpVillager(villager),
                villagerMpEntityService.isKingdomTaggedMpVillager(villager),
                player.getUniqueId());
    }

    private boolean isEligibleTerritoryVillager(Villager villager) {
        if (villager.getLocation().getWorld() == null) {
            return false;
        }
        Optional<String> kingdomId = territoryResolver.owningKingdomId(
                villager.getLocation().getWorld().getName(),
                villager.getLocation().getBlockX(),
                villager.getLocation().getBlockY(),
                villager.getLocation().getBlockZ());
        return TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                kingdomId,
                villagerMpEntityService.isTreasuryLordVillager(villager),
                villagerMpEntityService.isSeatedMpVillager(villager),
                villagerMpEntityService.isKingdomTaggedMpVillager(villager),
                villagerMpEntityService.isTownCrierVillager(villager));
    }

    private boolean isOnStrike(Villager villager) {
        if (villager.getLocation().getWorld() == null) {
            return false;
        }
        Optional<String> kingdomId = territoryResolver.owningKingdomId(
                villager.getLocation().getWorld().getName(),
                villager.getLocation().getBlockX(),
                villager.getLocation().getBlockY(),
                villager.getLocation().getBlockZ());
        return coordinator.isOrdinaryVillagerOnStrike(
                kingdomId,
                villager.getUniqueId(),
                villager.getLocation().getWorld(),
                villagerMpEntityService.isTreasuryLordVillager(villager),
                villagerMpEntityService.isSeatedMpVillager(villager),
                villagerMpEntityService.isKingdomTaggedMpVillager(villager));
    }

}
