package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.economy.villager.VillagerStrike;
import dev.mrlemoos.kingdom.granary.HungerRamp;

public final class VillagerTerritoryNametagReconciliation {

    private VillagerTerritoryNametagReconciliation() {}

    public static boolean shouldReconcileNametag(
            String currentNametag, String professionName, boolean eligible, boolean onStrike) {
        return shouldReconcileNametag(currentNametag, professionName, eligible, onStrike, false);
    }

    public static boolean shouldReconcileNametag(
            String currentNametag, String professionName, boolean eligible, boolean onStrike, boolean starving) {
        if (!eligible) {
            return false;
        }
        return !labelFor(professionName, onStrike, starving).equals(currentNametag);
    }

    public static String labelFor(String professionName, boolean onStrike) {
        return labelFor(professionName, onStrike, false);
    }

    /** Starving is the worse of the two privations, and is what the villager is called for it. */
    public static String labelFor(String professionName, boolean onStrike, boolean starving) {
        if (starving) {
            return HungerRamp.NAMETAG;
        }
        if (onStrike) {
            return VillagerStrike.NAMETAG;
        }
        return labelForProfession(professionName);
    }

    public static String labelForProfession(String professionName) {
        return ProfessionConstituencyResolver.villagerProfessionNametag(professionName);
    }
}
