package dev.mrlemoos.kingdom.election;

import java.util.Optional;

public final class TerritoryVillagerCommercePolicy {

    private TerritoryVillagerCommercePolicy() {}

    public static boolean shouldSettleEmeraldCommerce(
            Optional<String> kingdomId, boolean treasuryLord, boolean seatedMp, boolean kingdomTaggedMp) {
        return kingdomId.isPresent()
                && TerritoryVillagerDespawnPolicy.shouldManage(treasuryLord, seatedMp, kingdomTaggedMp);
    }
}
