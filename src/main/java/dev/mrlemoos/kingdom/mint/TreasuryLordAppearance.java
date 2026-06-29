package dev.mrlemoos.kingdom.mint;

import java.util.ArrayList;
import org.bukkit.entity.Villager;

public final class TreasuryLordAppearance {

    public static final Villager.Profession PROFESSION = Villager.Profession.CARTOGRAPHER;

    private TreasuryLordAppearance() {
    }

    public static void apply(Villager villager) {
        villager.setProfession(PROFESSION);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setVillagerExperience(1);
        villager.setVillagerLevel(1);
        villager.setRecipes(new ArrayList<>());
    }
}
