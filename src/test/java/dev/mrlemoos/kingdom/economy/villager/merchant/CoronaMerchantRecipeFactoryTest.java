package dev.mrlemoos.kingdom.economy.villager.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class CoronaMerchantRecipeFactoryTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void buildsRecipeWithCoronaNuggetIngredient() {
        List<MerchantRecipe> recipes = CoronaMerchantRecipeFactory.build(List.of(
                new CoronaMerchantOffer(Material.BREAD, 3, 12)));

        assertEquals(1, recipes.size());
        MerchantRecipe recipe = recipes.getFirst();
        assertEquals(Material.BREAD, recipe.getResult().getType());
        assertEquals(12, recipe.getMaxUses());
        assertTrue(CoronaItem.isCorona(recipe.getIngredients().getFirst()));
        assertEquals(3, recipe.getIngredients().getFirst().getAmount());
    }

    @Test
    void buildsEnchantedBookRecipeWithStoredEnchantment() {
        List<MerchantRecipe> recipes = CoronaMerchantRecipeFactory.build(List.of(
                new CoronaMerchantOffer(Material.ENCHANTED_BOOK, 1, 96, 1, Enchantment.MENDING, 1)));

        MerchantRecipe recipe = recipes.getFirst();
        assertEquals(Material.ENCHANTED_BOOK, recipe.getResult().getType());
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) recipe.getResult().getItemMeta();
        assertEquals(1, meta.getStoredEnchantLevel(Enchantment.MENDING));
        assertEquals(96, recipe.getIngredients().getFirst().getAmount());
    }

    @Test
    void buildsStackedResultForOfferAmount() {
        List<MerchantRecipe> recipes =
                CoronaMerchantRecipeFactory.build(List.of(new CoronaMerchantOffer(Material.ARROW, 16, 6, 12)));

        assertEquals(16, recipes.getFirst().getResult().getAmount());
    }

    @Test
    void enchantedBooksOfDifferentEnchantmentsAreDistinctOffers() {
        List<MerchantRecipe> recipes = CoronaMerchantRecipeFactory.build(List.of(
                new CoronaMerchantOffer(Material.ENCHANTED_BOOK, 1, 96, 1, Enchantment.MENDING, 1),
                new CoronaMerchantOffer(Material.ENCHANTED_BOOK, 1, 96, 1, Enchantment.SILK_TOUCH, 1)));

        assertFalse(CoronaMerchantRecipeFactory.sameOffer(recipes.get(0), recipes.get(1)));
        assertTrue(CoronaMerchantRecipeFactory.sameOffer(recipes.get(0), recipes.get(0)));
    }
}
