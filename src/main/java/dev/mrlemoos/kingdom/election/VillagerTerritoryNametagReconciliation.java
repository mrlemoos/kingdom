package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.economy.villager.VillagerStrike;

public final class VillagerTerritoryNametagReconciliation {

    private VillagerTerritoryNametagReconciliation() {}

    public static boolean shouldReconcileNametag(
            String currentNametag, String professionName, boolean eligible, boolean onStrike) {
        if (!eligible) {
            return false;
        }
        return !labelFor(professionName, onStrike).equals(currentNametag);
    }

    public static String labelFor(String professionName, boolean onStrike) {
        if (onStrike) {
            return VillagerStrike.NAMETAG;
        }
        return labelForProfession(professionName);
    }

    public static String labelForProfession(String professionName) {
        return ProfessionConstituencyResolver.villagerProfessionNametag(professionName);
    }
}
