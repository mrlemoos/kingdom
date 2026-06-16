package dev.leo.kingdom.listener;

import dev.leo.kingdom.service.KingdomService;
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
        String prefix = service.nobleChatPrefix(event.getPlayer().getUniqueId());
        if (prefix.isEmpty()) {
            return;
        }
        event.setFormat(ChatColor.GOLD + prefix + ChatColor.RESET + "%s" + ChatColor.WHITE + ": %s");
    }
}
