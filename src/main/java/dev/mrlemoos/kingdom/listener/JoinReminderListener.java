package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinReminderListener implements Listener {

    private final KingdomService service;
    private final boolean enabled;
    private final List<String> messageLines;

    public JoinReminderListener(KingdomService service, boolean enabled, List<String> messageLines) {
        this.service = service;
        this.enabled = enabled;
        this.messageLines = messageLines;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (service.getMembership(player.getUniqueId()).isPresent()) {
            return;
        }
        for (String line : messageLines) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }
}
