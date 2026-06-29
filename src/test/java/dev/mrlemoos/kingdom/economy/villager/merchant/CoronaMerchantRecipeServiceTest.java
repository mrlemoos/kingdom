package dev.mrlemoos.kingdom.economy.villager.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.VillagerMock;

class CoronaMerchantRecipeServiceTest {

    private ServerMock server;
    private CoronaMerchantRecipeService recipeService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        recipeService = new CoronaMerchantRecipeService(CoronaMerchantOfferConfig.defaults());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void unemployedVillagerGetsNoCoronaOffers() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.NONE);
        villager.setRecipes(List.of(vanillaEmeraldTrade()));

        recipeService.refreshRecipes(villager);

        assertTrue(villager.getRecipes().isEmpty());
    }

    @Test
    void employedVillagerGetsProfessionCoronaOffers() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.FARMER);
        villager.setRecipes(List.of(vanillaEmeraldTrade()));

        recipeService.refreshRecipes(villager);

        assertEquals(2, villager.getRecipes().size());
        assertTrue(villager.getRecipes().stream().anyMatch(CoronaMerchantRecipeFactory::isCoronaRecipe));
    }

    private static MerchantRecipe vanillaEmeraldTrade() {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.WHEAT, 20), 0);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 1));
        return recipe;
    }
}
