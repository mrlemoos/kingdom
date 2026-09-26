package dev.mrlemoos.kingdom.feedback;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.church.ChurchConsentBook;
import dev.mrlemoos.kingdom.church.ChurchRites;
import dev.mrlemoos.kingdom.church.gui.RiteConfirmGui;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Counts down the minute a party has to consent to a marriage or divorce, over both parties' heads,
 * and tells them both when it lapses unanswered. Its own one-second task, as the trial bar has.
 */
public final class ConsentBossBarService {

    private final JavaPlugin plugin;
    private final ChurchConsentBook consentBook;
    /** answerer → their bar. */
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ConsentBossBarService(JavaPlugin plugin, ChurchConsentBook consentBook) {
        this.plugin = plugin;
        this.consentBook = consentBook;
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
        for (UUID answerer : Set.copyOf(bars.keySet())) {
            clear(answerer);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (ChurchConsentBook.Request lapsed : consentBook.expire(now)) {
            lapse(lapsed);
        }
        Set<UUID> live = new HashSet<>();
        for (ChurchConsentBook.Request request : consentBook.open(now)) {
            live.add(request.answerer());
            sync(request, now);
        }
        for (UUID answerer : Set.copyOf(bars.keySet())) {
            if (!live.contains(answerer)) {
                clear(answerer);
            }
        }
    }

    private void sync(ChurchConsentBook.Request request, long nowMs) {
        long remainingMs = request.remainingMs(nowMs);
        String title = c("&d" + (request.kind() == ChurchConsentBook.Kind.MARRIAGE ? "Marriage" : "Divorce")
                + " &7— " + ChurchRites.nameOf(request.answerer()) + " to answer &f"
                + ((remainingMs + 999L) / 1000L) + "s");
        BossBar bar = bars.get(request.answerer());
        if (bar == null) {
            bar = Bukkit.createBossBar(title, BarColor.PINK, BarStyle.SOLID);
            bars.put(request.answerer(), bar);
        } else {
            bar.setTitle(title);
        }
        bar.setProgress(Math.max(0.0d, Math.min(1.0d, remainingMs / (double) ChurchConsentBook.WINDOW_MS)));
        bar.removeAll();
        for (UUID party : List.of(request.proposer(), request.answerer())) {
            Player online = Bukkit.getPlayer(party);
            if (online != null) {
                bar.addPlayer(online);
            }
        }
        bar.setVisible(true);
    }

    private void lapse(ChurchConsentBook.Request request) {
        clear(request.answerer());
        Player answerer = Bukkit.getPlayer(request.answerer());
        if (answerer != null
                && answerer.getOpenInventory().getTopInventory().getHolder() instanceof RiteConfirmGui gui
                && gui.purpose() == RiteConfirmGui.Purpose.CONSENT) {
            answerer.closeInventory();
        }
        for (UUID party : List.of(request.proposer(), request.answerer())) {
            Player online = Bukkit.getPlayer(party);
            if (online != null) {
                online.sendMessage(c("&7[Church] The request lapsed unanswered."));
                RealmFeedback.refuse(online, "The request lapsed unanswered.");
            }
        }
    }

    private void clear(UUID answerer) {
        BossBar bar = bars.remove(answerer);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }
}
