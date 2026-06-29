package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.service.KingdomService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatPrefixListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final KingdomService service;

    public ChatPrefixListener(KingdomService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String colouredPrefix = service.colouredNobleChatPrefix(event.getPlayer().getUniqueId());
        if (colouredPrefix.isEmpty()) {
            return;
        }
        Component prefix = LEGACY.deserialize(colouredPrefix);
        event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                .append(prefix)
                .append(sourceDisplayName.color(NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(message)
                .build());
    }
}
