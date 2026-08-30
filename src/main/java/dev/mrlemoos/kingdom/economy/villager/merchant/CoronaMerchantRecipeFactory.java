package dev.mrlemoos.kingdom.economy.villager.merchant;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class CoronaMerchantRecipeFactory {

    private CoronaMerchantRecipeFactory() {}

    public static List<MerchantRecipe> build(List<CoronaMerchantOffer> offers) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (CoronaMerchantOffer offer : offers) {
            MerchantRecipe recipe = new MerchantRecipe(result(offer), 0);
            recipe.setMaxUses(offer.maxUses());
            recipe.addIngredient(CoronaItem.create(offer.coronaPrice()));
            recipes.add(recipe);
        }
        return recipes;
    }

    public static ItemStack result(CoronaMerchantOffer offer) {
        ItemStack result = new ItemStack(offer.material(), offer.amount());
        Enchantment enchantment = offer.enchantment();
        if (enchantment == null) {
            return result;
        }
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }
        if (meta instanceof EnchantmentStorageMeta storage) {
            storage.addStoredEnchant(enchantment, offer.enchantmentLevel(), true);
        } else {
            meta.addEnchant(enchantment, offer.enchantmentLevel(), true);
        }
        result.setItemMeta(meta);
        return result;
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

    /**
     * Two Corona recipes are the same offer when the result stack and Corona price match. Results are
     * compared with {@link ItemStack#isSimilar(ItemStack)} so that enchanted books of different
     * enchantments stay distinct.
     */
    public static boolean sameOffer(MerchantRecipe left, MerchantRecipe right) {
        if (left == null || right == null) {
            return false;
        }
        if (!left.getResult().isSimilar(right.getResult())) {
            return false;
        }
        return coronaPrice(left) == coronaPrice(right);
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
