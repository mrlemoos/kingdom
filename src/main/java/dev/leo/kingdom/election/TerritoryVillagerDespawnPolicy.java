package dev.leo.kingdom.election;

public final class TerritoryVillagerDespawnPolicy {

    private TerritoryVillagerDespawnPolicy() {}

    public static boolean persistentInTerritory() {
        return true;
    }

    public static boolean removeWhenFarAwayInTerritory() {
        return false;
    }

    public static boolean persistentOutsideTerritory() {
        return false;
    }

    public static boolean removeWhenFarAwayOutsideTerritory() {
        return true;
    }

    public static boolean shouldManage(boolean treasuryLord, boolean seatedMp, boolean kingdomTaggedMp) {
        return !treasuryLord && !seatedMp && !kingdomTaggedMp;
    }

    public static boolean shouldApplyProtection(boolean inTerritory, boolean shouldManage) {
        return shouldManage && inTerritory;
    }

    public static boolean shouldRevertToVanilla(boolean inTerritory, boolean shouldManage) {
        return shouldManage && !inTerritory;
    }
}
