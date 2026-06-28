package dev.mrlemoos.kingdom.economy.villager.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.MerchantRecipe;
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
}
