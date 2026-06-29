package dev.mrlemoos.kingdom.economy.villager.merchant;

import dev.mrlemoos.kingdom.election.VillagerMpProfessionMatcher;
import dev.mrlemoos.kingdom.election.VillagerPlayerTradePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

public final class CoronaMerchantRecipeService {

    private final CoronaMerchantOfferConfig offerConfig;

    public CoronaMerchantRecipeService(CoronaMerchantOfferConfig offerConfig) {
        this.offerConfig = offerConfig != null ? offerConfig : CoronaMerchantOfferConfig.defaults();
    }

    public void refreshRecipes(Villager villager) {
        if (villager == null) {
            return;
        }
        if (!VillagerPlayerTradePolicy.canTradeWithPlayers(villager)) {
            villager.setRecipes(List.of());
            return;
        }
        List<MerchantRecipe> merged = new ArrayList<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (!CoronaMerchantRecipeFactory.isCoronaRecipe(recipe)) {
                merged.add(recipe);
            }
        }
        merged.addAll(CoronaMerchantRecipeFactory.build(
                offerConfig.offersFor(VillagerMpProfessionMatcher.professionName(villager))));
        villager.setRecipes(merged);
    }

    public Optional<MerchantRecipe> findCoronaRecipe(Villager villager, MerchantRecipe selected) {
        if (villager == null || selected == null || !CoronaMerchantRecipeFactory.isCoronaRecipe(selected)) {
            return Optional.empty();
        }
        return villager.getRecipes().stream()
                .filter(CoronaMerchantRecipeFactory::isCoronaRecipe)
                .filter(recipe -> recipesMatch(recipe, selected))
                .findFirst();
    }

    private static boolean recipesMatch(MerchantRecipe left, MerchantRecipe right) {
        if (left.getResult().getType() != right.getResult().getType()) {
            return false;
        }
        return CoronaMerchantRecipeFactory.coronaPrice(left) == CoronaMerchantRecipeFactory.coronaPrice(right);
    }
}
