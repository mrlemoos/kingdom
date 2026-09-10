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

        assertEquals(
                1 + CoronaMerchantOfferConfig.defaults().offersFor("farmer").size(),
                villager.getRecipes().size());
        assertTrue(villager.getRecipes().stream().anyMatch(CoronaMerchantRecipeFactory::isCoronaRecipe));
    }

    @Test
    void refreshKeepsConsumedUsesOfExistingCoronaOffers() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.LIBRARIAN);
        recipeService.refreshRecipes(villager);

        List<MerchantRecipe> recipes = new java.util.ArrayList<>(villager.getRecipes());
        MerchantRecipe used = recipes.getFirst();
        used.setUses(1);
        villager.setRecipes(recipes);

        recipeService.refreshRecipes(villager);

        assertEquals(
                1,
                villager.getRecipes().stream()
                        .filter(recipe -> CoronaMerchantRecipeFactory.sameOffer(recipe, used))
                        .findFirst()
                        .orElseThrow()
                        .getUses());
    }

    @Test
    void consumeUsesMarksTheRequestedNumberOfTrades() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.CLERIC);
        recipeService.refreshRecipes(villager);
        MerchantRecipe offer = villager.getRecipes().stream()
                .filter(CoronaMerchantRecipeFactory::isCoronaRecipe)
                .findFirst()
                .orElseThrow();

        assertEquals(3, recipeService.consumeUses(villager, offer, 3));

        assertEquals(3, stockedUses(villager, offer));
    }

    @Test
    void consumeUsesRefusesWhenTheOfferIsOutOfStock() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.CLERIC);
        recipeService.refreshRecipes(villager);
        List<MerchantRecipe> recipes = new java.util.ArrayList<>(villager.getRecipes());
        MerchantRecipe offer = recipes.stream()
                .filter(CoronaMerchantRecipeFactory::isCoronaRecipe)
                .findFirst()
                .orElseThrow();
        offer.setUses(offer.getMaxUses());
        villager.setRecipes(recipes);

        assertEquals(0, recipeService.consumeUses(villager, offer, 1));
        assertEquals(0, recipeService.remainingUses(villager, offer));

        assertEquals(offer.getMaxUses(), stockedUses(villager, offer));
    }

    @Test
    void consumeUsesClampsToRemainingStock() {
        Villager villager = new VillagerMock(server, UUID.randomUUID());
        villager.setProfession(Villager.Profession.CLERIC);
        recipeService.refreshRecipes(villager);
        MerchantRecipe offer = villager.getRecipes().stream()
                .filter(CoronaMerchantRecipeFactory::isCoronaRecipe)
                .findFirst()
                .orElseThrow();
        int maxUses = offer.getMaxUses();

        assertEquals(maxUses, recipeService.consumeUses(villager, offer, maxUses + 5));

        assertEquals(maxUses, stockedUses(villager, offer));
        assertEquals(0, recipeService.remainingUses(villager, offer));
    }

    private static int stockedUses(Villager villager, MerchantRecipe offer) {
        return villager.getRecipes().stream()
                .filter(recipe -> CoronaMerchantRecipeFactory.sameOffer(recipe, offer))
                .findFirst()
                .orElseThrow()
                .getUses();
    }

    private static MerchantRecipe vanillaEmeraldTrade() {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.WHEAT, 20), 0);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 1));
        return recipe;
    }
}
