package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinReminderListener implements Listener {

    private final KingdomService service;
    private final boolean enabled;
    private final List<String> messageLines;
    private final EconomyService economy;
    private final YamlEconomyStore economyStore;

    public JoinReminderListener(KingdomService service, boolean enabled, List<String> messageLines) {
        this(service, enabled, messageLines, null, null);
    }

    public JoinReminderListener(KingdomService service, boolean enabled, List<String> messageLines,
            EconomyService economy, YamlEconomyStore economyStore) {
        this.service = service;
        this.enabled = enabled;
        this.messageLines = messageLines;
        this.economy = economy;
        this.economyStore = economyStore;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (economy != null && economyStore != null) {
            boolean delivered = false;
            for (String kingdomId : economy.kingdomEconomies().keySet()) {
                String summary = economy.takeFinanceSummary(kingdomId, event.getPlayer().getUniqueId());
                if (summary != null) {
                    event.getPlayer().sendMessage(c("&6" + summary));
                    delivered = true;
                }
            }
            if (delivered) economyStore.saveFrom(economy);
        }
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (service.getMembership(player.getUniqueId()).isPresent()) {
            return;
        }
        for (String line : messageLines) {
            player.sendMessage(c(line));
        }
    }
}
