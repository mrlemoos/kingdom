package dev.mrlemoos.kingdom.feedback;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.police.TrialJurySession;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Counts down the real-time trial window on its own one-second task while any jury is open.
 * Unlike {@link DivisionBossBarService}, it does not wait on the sixty-second election sweep.
 */
public final class TrialBossBarService {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final Supplier<java.util.List<TrialJurySession>> sessions;
    private final Map<String, BossBar> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public TrialBossBarService(
            JavaPlugin plugin,
            KingdomService kingdomService,
            Supplier<java.util.List<TrialJurySession>> sessions) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.sessions = sessions;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clearAll();
    }

    private void tick() {
        java.util.List<TrialJurySession> open = sessions.get();
        Set<String> liveKingdoms = new HashSet<>();
        long now = System.currentTimeMillis();
        if (open != null) {
            for (TrialJurySession session : open) {
                liveKingdoms.add(session.kingdomId());
                sync(session, now);
            }
        }
        for (String kingdomId : Set.copyOf(bars.keySet())) {
            if (!liveKingdoms.contains(kingdomId)) {
                clear(kingdomId);
            }
        }
    }

    private void sync(TrialJurySession session, long nowMs) {
        long remainingMs = Math.max(0L, session.closesAtMs() - nowMs);
        long windowMs = Math.max(1L, session.closesAtMs() - session.openedAtMs());
        String accused = displayName(session.accusedId());
        BossBar bar = bars.get(session.kingdomId());
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    c("&c" + TrialBarText.label(accused, remainingMs / 1000L)),
                    BarColor.RED,
                    BarStyle.SOLID);
            bars.put(session.kingdomId(), bar);
        } else {
            bar.setTitle(c("&c" + TrialBarText.label(accused, remainingMs / 1000L)));
        }
        bar.setProgress(TrialBarText.progress(remainingMs, windowMs));
        seat(bar, session.kingdomId());
        bar.setVisible(true);
    }

    public void clear(String kingdomId) {
        BossBar bar = bars.remove(kingdomId);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    public void clearAll() {
        for (String kingdomId : Map.copyOf(bars).keySet()) {
            clear(kingdomId);
        }
    }

    private void seat(BossBar bar, String kingdomId) {
        bar.removeAll();
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
            bar.addPlayer(member);
        }
    }

    private static String displayName(java.util.UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }
}
