package dev.mrlemoos.kingdom.election;

import org.bukkit.ChatColor;

public final class VillagerMpCombatPolicy {

    private VillagerMpCombatPolicy() {}

    /** Seated and released villager MPs remain damageable; AI lock handles stationing. */
    public static boolean shouldLockFromCombat(boolean seatedMp) {
        return false;
    }

    public static boolean needsCombatRestore(
            boolean kingdomTaggedMp, boolean seatedMp, boolean invulnerable, String customName) {
        if (seatedMp) {
            return false;
        }
        if (kingdomTaggedMp) {
            return true;
        }
        return invulnerable && hasMpVillagerNametag(customName);
    }

    public static boolean hasMpVillagerNametag(String customName) {
        if (customName == null || customName.isBlank()) {
            return false;
        }
        return ChatColor.stripColor(customName).contains("[MP]");
    }
}
