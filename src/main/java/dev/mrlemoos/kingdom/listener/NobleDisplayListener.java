package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class NobleDisplayListener implements Listener {

    private final NoblePrefixDisplay display;

    public NobleDisplayListener(NoblePrefixDisplay display) {
        this.display = display;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        display.refresh(event.getPlayer());
    }
}
