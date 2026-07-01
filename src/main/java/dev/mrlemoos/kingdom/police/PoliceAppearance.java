package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

public final class PoliceAppearance {

    public static final String JUDGE_VILLAGER_LABEL = "Magistrate";

    private PoliceAppearance() {}

    public static String judgeVillagerNametag() {
        return PoliceService.JUDGE_CHAT_COLOR + "[Judge] " + c("&f" + JUDGE_VILLAGER_LABEL);
    }

    public static String patrolGolemNametag() {
        return PoliceService.CONSTABLE_CHAT_COLOR + "[Constable] " + c("&fPatrol");
    }

    public static String guardGolemNametag() {
        return PoliceService.GUARD_CHAT_COLOR + "[Guard] " + c("&fWatch");
    }
}
