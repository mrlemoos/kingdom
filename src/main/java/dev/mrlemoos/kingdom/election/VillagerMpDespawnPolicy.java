package dev.mrlemoos.kingdom.election;

public final class VillagerMpDespawnPolicy {

    private VillagerMpDespawnPolicy() {}

    public static boolean persistentWhileSeated() {
        return true;
    }

    public static boolean removeWhenFarAwayWhileSeated() {
        return false;
    }

    /** Released and former MPs return to normal Minecraft despawn rules. */
    public static boolean persistentAfterRelease() {
        return false;
    }

    public static boolean removeWhenFarAwayAfterRelease() {
        return true;
    }

    /** Former or stranded MPs may still carry seated despawn locks after release. */
    public static boolean needsDespawnRestore(
            boolean kingdomTaggedMp,
            boolean seatedMp,
            boolean treasuryLord,
            boolean persistent,
            boolean removeWhenFarAway,
            String customName) {
        if (seatedMp || treasuryLord) {
            return false;
        }
        if (!persistent && removeWhenFarAway) {
            return false;
        }
        if (kingdomTaggedMp) {
            return true;
        }
        return VillagerMpCombatPolicy.hasMpVillagerNametag(customName);
    }
}
