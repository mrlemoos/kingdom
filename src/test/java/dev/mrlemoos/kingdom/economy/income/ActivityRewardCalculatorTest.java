package dev.mrlemoos.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityRewardCalculatorTest {

    private ActivityRewardCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ActivityRewardCalculator(EconomyConfig.defaults());
    }

    @Test
    void harvestRewardUsesMaterialValue() {
        assertEquals(0.04, calculator.calculateHarvestReward(Material.WHEAT, 1), 1e-9);
        assertEquals(0.12, calculator.calculateHarvestReward(Material.NETHER_WART, 50), 1e-9);
    }

    @Test
    void unknownHarvestMaterialReturnsZero() {
        assertEquals(0.0, calculator.calculateHarvestReward(Material.STONE, 1));
    }

    @Test
    void harvestRewardDiminishesAfterThreshold() {
        double full = calculator.calculateHarvestReward(Material.WHEAT, 100);
        double diminished = calculator.calculateHarvestReward(Material.WHEAT, 101);

        assertEquals(0.04, full, 1e-9);
        assertEquals(0.02, diminished, 1e-9);
    }

    @Test
    void craftRewardUsesMaterialValue() {
        assertEquals(0.25, calculator.calculateCraftReward(Material.IRON_INGOT, 1), 1e-9);
        assertEquals(3.0, calculator.calculateCraftReward(Material.DIAMOND_PICKAXE, 5), 1e-9);
    }

    @Test
    void trivialCraftRecipesReturnZero() {
        assertEquals(0.0, calculator.calculateCraftReward(Material.STICK, 1));
        assertEquals(0.0, calculator.calculateCraftReward(Material.BOWL, 1));
    }

    @Test
    void unknownCraftMaterialReturnsZero() {
        assertEquals(0.0, calculator.calculateCraftReward(Material.DIRT, 1));
    }

    @Test
    void villagerTradeRewardIsPercentOfEmeraldCost() {
        assertEquals(1.0, calculator.calculateVillagerTradeReward(10), 1e-9);
        assertEquals(0.5, calculator.calculateVillagerTradeReward(5), 1e-9);
    }

    @Test
    void nonPositiveEmeraldCostReturnsZero() {
        assertEquals(0.0, calculator.calculateVillagerTradeReward(0));
        assertEquals(0.0, calculator.calculateVillagerTradeReward(-3));
    }

    @Test
    void playerTradeBonusIsFixed() {
        assertEquals(0.05, calculator.calculatePlayerTradeBonus(), 1e-9);
    }
}
