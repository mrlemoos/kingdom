package dev.mrlemoos.kingdom.economy.villager.merchant;

import dev.mrlemoos.kingdom.election.VillagerMpProfessionMatcher;
import dev.mrlemoos.kingdom.election.VillagerPlayerTradePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
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

    /**
     * Marks {@code count} more uses of the stocked Corona offer matching {@code selected}, mirroring
     * what vanilla does for trades the plugin settled itself. Never goes past the offer's max uses;
     * returns how many uses were actually marked.
     */
    public int consumeUses(Villager villager, MerchantRecipe selected, int count) {
        if (villager == null
                || selected == null
                || count <= 0
                || !CoronaMerchantRecipeFactory.isCoronaRecipe(selected)) {
            return 0;
        }
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        for (int index = 0; index < recipes.size(); index++) {
            MerchantRecipe offer = recipes.get(index);
            if (!CoronaMerchantRecipeFactory.isCoronaRecipe(offer)
                    || !CoronaMerchantRecipeFactory.sameOffer(offer, selected)) {
                continue;
            }
            int consumed = Math.min(count, offer.getMaxUses() - offer.getUses());
            if (consumed <= 0) {
                return 0;
            }
            MerchantRecipe updated = copyOf(offer);
            updated.setUses(offer.getUses() + consumed);
            recipes.set(index, updated);
            villager.setRecipes(recipes);
            return consumed;
        }
        return 0;
    }

    /** How many times the stocked Corona offer matching {@code selected} can still be traded. */
    public int remainingUses(Villager villager, MerchantRecipe selected) {
        Optional<MerchantRecipe> stocked = findCoronaRecipe(villager, selected);
        if (stocked.isEmpty()) {
            return 0;
        }
        MerchantRecipe offer = stocked.get();
        return Math.max(0, offer.getMaxUses() - offer.getUses());
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

    private static MerchantRecipe copyOf(MerchantRecipe recipe) {
        MerchantRecipe copy = new MerchantRecipe(recipe.getResult(), recipe.getMaxUses());
        copy.setUses(recipe.getUses());
        copy.setVillagerExperience(recipe.getVillagerExperience());
        copy.setExperienceReward(recipe.hasExperienceReward());
        for (ItemStack ingredient : recipe.getIngredients()) {
            copy.addIngredient(ingredient);
        }
        return copy;
    }
}
