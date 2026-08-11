package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.police.JurisdictionPort;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Refreshes player prefixes when crossing into or out of a kingdom's linked territory so the
 * wanted nametag appears and clears with jurisdiction.
 */
public final class WantedNametagListener implements Listener {

    private final NoblePrefixDisplay display;
    private final JurisdictionPort jurisdictionPort;
    private final Map<UUID, Optional<String>> lastTerritory = new ConcurrentHashMap<>();

    public WantedNametagListener(NoblePrefixDisplay display, JurisdictionPort jurisdictionPort) {
        this.display = Objects.requireNonNull(display, "display");
        this.jurisdictionPort = Objects.requireNonNull(jurisdictionPort, "jurisdictionPort");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        Optional<String> now = jurisdictionPort.kingdomAt(playerId);
        Optional<String> previous = lastTerritory.getOrDefault(playerId, Optional.empty());
        if (Objects.equals(previous.orElse(null), now.orElse(null))) {
            return;
        }
        lastTerritory.put(playerId, now);
        display.refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTerritory.remove(event.getPlayer().getUniqueId());
    }
}
