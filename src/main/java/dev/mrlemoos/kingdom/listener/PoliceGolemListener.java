package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;

public final class PoliceGolemListener implements Listener {

    private final PoliceService policeService;
    private final PoliceGolemService golemService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public PoliceGolemListener(
            PoliceService policeService,
            PoliceGolemService golemService,
            KingdomService kingdomService,
            YamlKingdomStore store) {
        this.policeService = policeService;
        this.golemService = golemService;
        this.kingdomService = kingdomService;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        handleRemoval(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRemove(EntityRemoveEvent event) {
        handleRemoval(event.getEntity());
    }

    private void handleRemoval(Entity entity) {
        if (!golemService.isPoliceGolem(entity)) {
            return;
        }
        UUID entityId = entity.getUniqueId();
        Optional<String> kingdomId = golemService.kingdomIdForGolem(entity);
        if (kingdomId.isEmpty()) {
            kingdomId = policeService.findKingdomForRegisteredGolem(entityId);
        }
        if (kingdomId.isEmpty()) {
            return;
        }
        if (policeService.deregisterGolem(kingdomId.get(), entityId) instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
    }
}
