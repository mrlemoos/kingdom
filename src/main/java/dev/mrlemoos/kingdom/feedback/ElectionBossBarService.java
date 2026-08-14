package dev.mrlemoos.kingdom.feedback;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.ElectionState;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Counts down an open election on its own one-second task. Unlike {@link DivisionBossBarService},
 * it does not wait on the sixty-second election sweep — polling is a real-time window.
 */
public final class ElectionBossBarService {

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ElectionConfig config;
    private final Map<String, BossBar> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ElectionBossBarService(
            JavaPlugin plugin, KingdomService kingdomService, ElectionConfig config) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
        this.config = config;
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
        long now = System.currentTimeMillis();
        Set<String> liveKingdoms = new HashSet<>();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            ElectionState election = kingdom.getElectionState().election();
            if (!election.isActive()) {
                continue;
            }
            liveKingdoms.add(kingdom.getId());
            sync(kingdom.getId(), election, now);
        }
        for (String kingdomId : Set.copyOf(bars.keySet())) {
            if (!liveKingdoms.contains(kingdomId)) {
                clear(kingdomId);
            }
        }
    }

    private void sync(String kingdomId, ElectionState election, long nowMs) {
        long remainingMs = Math.max(0L, election.endsAtMs() - nowMs);
        if (election.phase() == ElectionPhase.AWAITING_SPEAKER_TIE) {
            remainingMs = 0L;
        }
        String title = ElectionBarText.label(
                election.type().orElse(null), election.phase(), remainingMs);
        BossBar bar = bars.get(kingdomId);
        if (bar == null) {
            bar = Bukkit.createBossBar(c("&b" + title), BarColor.BLUE, BarStyle.SOLID);
            bars.put(kingdomId, bar);
        } else {
            bar.setTitle(c("&b" + title));
        }
        bar.setProgress(ElectionBarText.progress(remainingMs, config.durationMs()));
        Optional<ElectionType> type = election.type();
        boolean houseOnly = type.isPresent() && type.get() == ElectionType.PREMIER;
        seat(bar, kingdomId, houseOnly);
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

    /**
     * Re-seats the bar. A Premier election is House business; a general or by-election hangs over
     * the whole realm.
     */
    private void seat(BossBar bar, String kingdomId, boolean houseOnly) {
        bar.removeAll();
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
            if (!houseOnly || houseMember(member)) {
                bar.addPlayer(member);
            }
        }
    }

    private boolean houseMember(Player member) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(member.getUniqueId());
        return membership.isPresent() && HouseBarAudience.sees(membership.get().getRank());
    }
}
