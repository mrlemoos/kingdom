package dev.mrlemoos.kingdom.election;

/**
 * Which villagers wear a vanilla profession nametag. Plugin NPCs — Treasury Lords, MPs, the Town
 * Crier, the villager magistrate, the cleric — carry a name the plugin gave them, so no sweep may
 * relabel them with their profession.
 */
public final class VillagerNametagRefreshEligibility {

    private VillagerNametagRefreshEligibility() {}

    /** True when the villager holds any plugin role, and so owns its own nametag. */
    public static boolean isPluginNpc(
            boolean treasuryLord,
            boolean kingdomTaggedMp,
            boolean seatedMp,
            boolean townCrier,
            boolean villagerMagistrate,
            boolean cleric) {
        return treasuryLord || kingdomTaggedMp || seatedMp || townCrier || villagerMagistrate || cleric;
    }

    public static boolean shouldRefreshOrdinaryTerritoryNametag(
            boolean treasuryLord,
            boolean kingdomTaggedMp,
            boolean seatedMp,
            boolean townCrier,
            boolean villagerMagistrate,
            boolean cleric,
            boolean inKingdomTerritory) {
        return inKingdomTerritory
                && !isPluginNpc(
                        treasuryLord, kingdomTaggedMp, seatedMp, townCrier, villagerMagistrate, cleric);
    }
}
