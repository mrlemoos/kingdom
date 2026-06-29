package dev.mrlemoos.kingdom.election;

public final class VillagerTerritoryNametagReconciliation {

    private VillagerTerritoryNametagReconciliation() {}

    public static boolean shouldReconcileNametag(String currentNametag, String professionName, boolean eligible) {
        if (!eligible) {
            return false;
        }
        return !labelForProfession(professionName).equals(currentNametag);
    }

    public static String labelForProfession(String professionName) {
        return ProfessionConstituencyResolver.villagerProfessionNametag(professionName);
    }
}
