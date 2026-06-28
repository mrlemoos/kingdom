package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.income.ActivityCategory;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantRecipeFactory;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

public final class EconomyActivityListener implements Listener {

    private final EconomyCoordinator coordinator;
    private final VillagerMpEntityService villagerMpEntityService;

    public EconomyActivityListener(EconomyCoordinator coordinator, VillagerMpEntityService villagerMpEntityService) {
        this.coordinator = coordinator;
        this.villagerMpEntityService = villagerMpEntityService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        if (!coordinator.config().harvestMaterialValues().containsKey(material)) {
            return;
        }
        if (!isFullyGrown(block)) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        int harvestCount = coordinator.harvestCountThisHour(player.getUniqueId(), nowMs);
        double gross = coordinator.activityRewardCalculator().calculateHarvestReward(material, harvestCount);
        if (gross <= 0.0) {
            return;
        }

        coordinator.creditPlayerFromActivity(player, gross, block.getLocation(), ActivityCategory.HARVEST)
                .ifPresent(ignored -> {
                    coordinator.recordHarvest(player.getUniqueId(), nowMs);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            result = event.getRecipe().getResult();
        }
        if (result == null || result.getType().isAir()) {
            return;
        }

        double gross = coordinator.activityRewardCalculator().calculateCraftReward(result.getType(), 0);
        if (gross <= 0.0) {
            return;
        }

        coordinator.creditPlayerFromActivity(
                player, gross, player.getLocation(), ActivityCategory.CRAFT);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerchantTrade(InventoryClickEvent event) {
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

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) {
            return;
        }

        MerchantInventory merchantInventory = (MerchantInventory) event.getView().getTopInventory();
        MerchantRecipe recipe = merchantInventory.getSelectedRecipe();
        if (recipe == null) {
            return;
        }

        int emeraldCost = emeraldCost(recipe);
        int coronaCost = CoronaMerchantRecipeFactory.coronaPrice(recipe);
        if (emeraldCost > 0) {
            double gross = coordinator.activityRewardCalculator().calculateVillagerTradeReward(emeraldCost);
            if (gross > 0.0) {
                coordinator.creditPlayerFromActivity(
                        player, gross, player.getLocation(), ActivityCategory.VILLAGER_TRADE);
            }
        }

        if (merchantInventory.getMerchant() instanceof Villager villager) {
            boolean treasuryLord = villagerMpEntityService.isTreasuryLordVillager(villager);
            boolean seatedMp = villagerMpEntityService.isSeatedMpVillager(villager);
            boolean kingdomTaggedMp = villagerMpEntityService.isKingdomTaggedMpVillager(villager);
            if (coronaCost > 0) {
                coordinator.settleCoronaMerchantCommerce(
                        villager, coronaCost, treasuryLord, seatedMp, kingdomTaggedMp);
            }
            if (emeraldCost > 0) {
                coordinator.settleEmeraldVillagerCommerce(
                        villager, emeraldCost, treasuryLord, seatedMp, kingdomTaggedMp);
            }
        }
    }

    private static boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    private static int emeraldCost(MerchantRecipe recipe) {
        int emeralds = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient != null && ingredient.getType() == Material.EMERALD) {
                emeralds += ingredient.getAmount();
            }
        }
        return emeralds;
    }
}
