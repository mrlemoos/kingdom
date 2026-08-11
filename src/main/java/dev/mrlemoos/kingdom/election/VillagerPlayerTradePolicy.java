package dev.mrlemoos.kingdom.election;

import org.bukkit.entity.Villager;

public final class VillagerPlayerTradePolicy {

    private VillagerPlayerTradePolicy() {
    }

    public static boolean canTradeWithPlayers(Villager villager) {
        return canTradeWithPlayers(villager, false);
    }

    public static boolean canTradeWithPlayers(Villager villager, boolean onStrike) {
        return villager != null
                && villager.getProfession() != Villager.Profession.NONE
                && !onStrike;
    }
}
