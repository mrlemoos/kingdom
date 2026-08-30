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
    private final CoronaMerchantStockRotation rotation;

    public CoronaMerchantRecipeService(CoronaMerchantOfferConfig offerConfig) {
        this(offerConfig, CoronaMerchantStockRotation.defaults());
    }

    public CoronaMerchantRecipeService(CoronaMerchantOfferConfig offerConfig, CoronaMerchantStockRotation rotation) {
        this.offerConfig = offerConfig != null ? offerConfig : CoronaMerchantOfferConfig.defaults();
        this.rotation = rotation != null ? rotation : CoronaMerchantStockRotation.defaults();
    }

    public void refreshRecipes(Villager villager) {
        refreshRecipes(villager, false);
    }

    public void refreshRecipes(Villager villager, boolean onStrike) {
        if (villager == null) {
            return;
        }
        if (!VillagerPlayerTradePolicy.canTradeWithPlayers(villager, onStrike)) {
            villager.setRecipes(List.of());
            return;
        }
        List<MerchantRecipe> existingCorona = new ArrayList<>();
        List<MerchantRecipe> merged = new ArrayList<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (CoronaMerchantRecipeFactory.isCoronaRecipe(recipe)) {
                existingCorona.add(recipe);
            } else {
                merged.add(recipe);
            }
        }
        for (MerchantRecipe offer : CoronaMerchantRecipeFactory.build(rotation.stock(
                villager.getUniqueId(), offerConfig.offersFor(VillagerMpProfessionMatcher.professionName(villager))))) {
            merged.add(existingCorona.stream()
                    .filter(existing -> CoronaMerchantRecipeFactory.sameOffer(existing, offer))
                    .findFirst()
                    .orElse(offer));
        }
        villager.setRecipes(merged);
    }

    public Optional<MerchantRecipe> findCoronaRecipe(Villager villager, MerchantRecipe selected) {
        if (villager == null || selected == null || !CoronaMerchantRecipeFactory.isCoronaRecipe(selected)) {
            return Optional.empty();
        }
        return villager.getRecipes().stream()
                .filter(CoronaMerchantRecipeFactory::isCoronaRecipe)
                .filter(recipe -> CoronaMerchantRecipeFactory.sameOffer(recipe, selected))
                .findFirst();
    }
}
