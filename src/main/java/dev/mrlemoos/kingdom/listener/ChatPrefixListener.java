package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.display.PlayerPrefixComposer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatPrefixListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PlayerPrefixComposer prefixComposer;

    public ChatPrefixListener(PlayerPrefixComposer prefixComposer) {
        this.prefixComposer = Objects.requireNonNull(prefixComposer, "prefixComposer");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String colouredPrefix = prefixComposer.fullColouredPrefix(event.getPlayer().getUniqueId());
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
