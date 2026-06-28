package dev.mrlemoos.kingdom.economy.villager.merchant;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public final class CoronaMerchantRecipeFactory {

    private CoronaMerchantRecipeFactory() {}

    public static List<MerchantRecipe> build(List<CoronaMerchantOffer> offers) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (CoronaMerchantOffer offer : offers) {
            ItemStack result = new ItemStack(offer.material(), 1);
            MerchantRecipe recipe = new MerchantRecipe(result, 0);
            recipe.setMaxUses(offer.maxUses());
            recipe.addIngredient(CoronaItem.create(offer.coronaPrice()));
            recipes.add(recipe);
        }
        return recipes;
    }

    public static boolean isCoronaRecipe(MerchantRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (CoronaItem.isCorona(ingredient)) {
                return true;
            }
        }
        return false;
    }

    public static int coronaPrice(MerchantRecipe recipe) {
        if (recipe == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (CoronaItem.isCorona(ingredient)) {
                total += ingredient.getAmount();
            }
        }
        return total;
    }
}
