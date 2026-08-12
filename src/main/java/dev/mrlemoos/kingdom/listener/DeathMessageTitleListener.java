package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.display.DeathMessageTitler;
import dev.mrlemoos.kingdom.display.PlayerPrefixComposer;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Shows the victim's noble title in the vanilla death message. */
public final class DeathMessageTitleListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PlayerPrefixComposer prefixComposer;

    public DeathMessageTitleListener(PlayerPrefixComposer prefixComposer) {
        this.prefixComposer = Objects.requireNonNull(prefixComposer, "prefixComposer");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Component message = event.deathMessage();
        if (message == null) {
            return;
        }
        Player victim = event.getEntity();
        String colouredPrefix = prefixComposer.fullColouredPrefix(victim.getUniqueId());
        if (colouredPrefix.isEmpty()) {
            return;
        }
        event.deathMessage(
                DeathMessageTitler.titled(message, victim.getName(), LEGACY.deserialize(colouredPrefix)));
    }
}
