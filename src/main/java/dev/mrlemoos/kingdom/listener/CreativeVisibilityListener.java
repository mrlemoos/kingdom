package dev.mrlemoos.kingdom.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Creative players are hidden from everyone not in creative; creative players see one another. */
public final class CreativeVisibilityListener implements Listener {

    private final JavaPlugin plugin;

    public CreativeVisibilityListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean canSee(GameMode viewer, GameMode target) {
        return target != GameMode.CREATIVE || viewer == GameMode.CREATIVE;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // ponytail: event fires before the mode applies, so refresh a tick later
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                refresh(player);
            }
        });
    }

    private void refresh(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other != player) {
                apply(other, player);
                apply(player, other);
            }
        }
    }

    private void apply(Player viewer, Player target) {
        if (canSee(viewer.getGameMode(), target.getGameMode())) {
            viewer.showPlayer(plugin, target);
        } else {
            viewer.hidePlayer(plugin, target);
        }
    }
}
