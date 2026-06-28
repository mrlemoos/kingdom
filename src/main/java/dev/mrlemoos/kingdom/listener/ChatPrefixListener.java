package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.service.KingdomService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatPrefixListener implements Listener {

    private final KingdomService service;

    public ChatPrefixListener(KingdomService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String coloredPrefix = service.coloredNobleChatPrefix(event.getPlayer().getUniqueId());
        if (coloredPrefix.isEmpty()) {
            return;
        }
        event.setFormat(coloredPrefix + ChatColor.RESET + "%s" + ChatColor.WHITE + ": %s");
    }
}
