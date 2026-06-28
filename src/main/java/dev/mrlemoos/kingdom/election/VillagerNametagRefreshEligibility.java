package dev.mrlemoos.kingdom.election;

public final class VillagerNametagRefreshEligibility {

    private VillagerNametagRefreshEligibility() {}

    public static boolean shouldRefreshOrdinaryTerritoryNametag(
            boolean treasuryLord, boolean kingdomTaggedMp, boolean seatedMp, boolean inKingdomTerritory) {
        return inKingdomTerritory && !treasuryLord && !kingdomTaggedMp && !seatedMp;
    }
}
