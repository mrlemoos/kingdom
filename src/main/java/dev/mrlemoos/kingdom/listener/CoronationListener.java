package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.parliament.CoronationCeremony;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Holds a Coronation for a monarch crowned while away until they return to the realm. */
public final class CoronationListener implements Listener {

    private final CoronationCeremony ceremony;

    public CoronationListener(CoronationCeremony ceremony) {
        this.ceremony = ceremony;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        ceremony.crownIfPendingOnJoin(event.getPlayer());
    }
}
