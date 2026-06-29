package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;

public final class VillagerProfessionNametagListener implements Listener {

    private final VillagerMpEntityService villagerMpEntityService;

    public VillagerProfessionNametagListener(VillagerMpEntityService villagerMpEntityService) {
        this.villagerMpEntityService = villagerMpEntityService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        villagerMpEntityService.refreshNametagAfterProfessionChange(villager, event.getProfession());
    }
}
