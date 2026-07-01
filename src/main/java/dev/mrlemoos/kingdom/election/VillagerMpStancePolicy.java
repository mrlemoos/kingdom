package dev.mrlemoos.kingdom.election;

import org.bukkit.entity.Pose;

public final class VillagerMpStancePolicy {

    private VillagerMpStancePolicy() {}

    public static boolean needsStandingReset(boolean sleeping, Pose pose) {
        return sleeping || pose == Pose.SLEEPING;
    }
}
