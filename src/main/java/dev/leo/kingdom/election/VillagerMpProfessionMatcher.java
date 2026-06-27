package dev.leo.kingdom.election;

import java.util.Locale;
import org.bukkit.entity.Villager;

public final class VillagerMpProfessionMatcher {

    public static final String NONE_PROFESSION_KEY = "minecraft:none";

    private VillagerMpProfessionMatcher() {}

    public static boolean matches(String seatProfession, Villager villager) {
        return matchesKey(seatProfession, professionKey(villager));
    }

    public static boolean matchesKey(String seatProfession, String villagerProfessionKey) {
        if (ProfessionConstituencyResolver.CITIZEN_PROFESSION.equals(seatProfession)) {
            return NONE_PROFESSION_KEY.equalsIgnoreCase(villagerProfessionKey)
                    || "none".equalsIgnoreCase(villagerProfessionKey);
        }
        String normalisedSeat = seatProfession.toLowerCase(Locale.ROOT);
        String normalisedVillager = normaliseProfessionKey(villagerProfessionKey);
        return normalisedSeat.equals(normalisedVillager);
    }

    public static String professionKey(Villager villager) {
        return villager.getProfession().getKey().toString();
    }

    public static String professionName(Villager villager) {
        return normaliseProfessionKey(professionKey(villager));
    }

    private static String normaliseProfessionKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        int separator = lower.indexOf(':');
        return separator >= 0 ? lower.substring(separator + 1) : lower;
    }
}
