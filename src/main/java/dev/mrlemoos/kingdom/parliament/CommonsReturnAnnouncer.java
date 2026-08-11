package dev.mrlemoos.kingdom.parliament;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

/**
 * Gives the Speaker a voice and a stage: the return of the Commons is read out one member at a
 * time from the Bar of the House, each sentence hanging over the Speaker's head as it is spoken.
 */
public final class CommonsReturnAnnouncer {

    /** Ticks between one member's return and the next. */
    public static final long DEFAULT_LINE_DELAY_TICKS = 30L;

    private static final int TOTAL_SEATS = 8;
    private static final double HOLOGRAM_HEIGHT = 2.4;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final Map<String, BukkitTask> readings = new ConcurrentHashMap<>();
    private long lineDelayTicks = DEFAULT_LINE_DELAY_TICKS;

    public CommonsReturnAnnouncer(JavaPlugin plugin, KingdomService kingdomService) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
    }

    public void setLineDelayTicks(long lineDelayTicks) {
        this.lineDelayTicks = Math.max(1L, lineDelayTicks);
    }

    public long lineDelayTicks() {
        return lineDelayTicks;
    }

    /**
     * The Speaker takes the Bar and reads the roll-call of the House, a member at a time. The
     * callback runs once the last line has been read and the Speaker has resumed their place.
     */
    public void announceRollCall(String kingdomId, Location addressPoint, Runnable whenFinished) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            whenFinished.run();
            return;
        }
        List<String> lines = CommonsReturn.rollCall(
                kingdom.get().getElectionState(), TOTAL_SEATS, CommonsReturnAnnouncer::playerName);
        Entity speaker = speakerEntity(kingdom.get());
        Location resumeAt = speaker == null ? null : speaker.getLocation().clone();
        takeTheBar(kingdom.get(), speaker, addressPoint);
        read(kingdomId, lines, () -> {
            if (speaker != null && resumeAt != null && speaker.isValid()) {
                speaker.teleport(resumeAt);
            }
            whenFinished.run();
        });
    }

    /** The Speaker reads a single seat's return after a by-election. */
    public void announceSeatReturn(String kingdomId, int seatIndex) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> {
            Optional<MpSeat> seat = kingdom.getElectionState().seat(seatIndex);
            if (seat.isEmpty()) {
                return;
            }
            read(
                    kingdomId,
                    List.of(CommonsReturn.seatReturn(seat.get(), CommonsReturnAnnouncer::playerName)),
                    () -> {});
        });
    }

    private void read(String kingdomId, List<String> lines, Runnable whenFinished) {
        BukkitTask previous = readings.remove(kingdomId);
        if (previous != null) {
            previous.cancel();
        }
        Reading reading = new Reading(kingdomId, lines, whenFinished);
        readings.put(kingdomId, reading.runTaskTimer(plugin, 0L, lineDelayTicks));
    }

    /** One line per tick-interval, so the House hears its members named rather than dumped. */
    private final class Reading extends org.bukkit.scheduler.BukkitRunnable {

        private final String kingdomId;
        private final List<String> lines;
        private final Runnable whenFinished;
        private int next;
        private TextDisplay hologram;

        private Reading(String kingdomId, List<String> lines, Runnable whenFinished) {
            this.kingdomId = kingdomId;
            this.lines = lines;
            this.whenFinished = whenFinished;
        }

        @Override
        public void run() {
            Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
            if (kingdom.isEmpty() || next >= lines.size()) {
                finish();
                return;
            }
            String line = lines.get(next++);
            Entity speaker = speakerEntity(kingdom.get());
            for (Player member : onlineMembers(kingdomId)) {
                member.sendMessage(NoblePrefixDisplay.speakerVillagerNametag() + c(" &f" + line));
            }
            if (speaker != null) {
                speaker.getWorld().playSound(speaker.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
                showHologram(speaker, line);
            }
        }

        private void showHologram(Entity speaker, String line) {
            Location above = speaker.getLocation().clone().add(0, HOLOGRAM_HEIGHT, 0);
            if (hologram == null || !hologram.isValid()) {
                hologram = speaker.getWorld().spawn(above, TextDisplay.class, spawned -> {
                    spawned.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                    spawned.setSeeThrough(true);
                    spawned.setShadowed(true);
                    spawned.setPersistent(false);
                    spawned.setTransformation(new Transformation(
                            new org.joml.Vector3f(),
                            new org.joml.Quaternionf(),
                            new org.joml.Vector3f(1.2f, 1.2f, 1.2f),
                            new org.joml.Quaternionf()));
                });
            } else {
                hologram.teleport(above);
            }
            // A text display inherits no chat default, so an uncoloured root renders black.
            hologram.text(ColourEncoder.component("&f" + line)
                    .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE));
        }

        private void finish() {
            if (hologram != null && hologram.isValid()) {
                hologram.remove();
            }
            hologram = null;
            readings.remove(kingdomId);
            cancel();
            whenFinished.run();
        }
    }

    /** The Speaker crosses to the Bar of the House to address the Crown. */
    private void takeTheBar(Kingdom kingdom, Entity speaker, Location addressPoint) {
        if (speaker == null) {
            return;
        }
        Optional<ChamberSite> bar = kingdom.getParliamentSites().bar();
        if (bar.isPresent()) {
            World world = Bukkit.getWorld(bar.get().worldName());
            if (world != null) {
                speaker.teleport(new Location(world, bar.get().x() + 0.5, bar.get().y(), bar.get().z() + 0.5));
                return;
            }
        }
        if (addressPoint != null) {
            speaker.teleport(addressPoint);
        }
    }

    /** The villager Speaker gives voice to the roll; where a player holds the Chair, they do. */
    private Entity speakerEntity(Kingdom kingdom) {
        Optional<UUID> villagerSpeaker = kingdom.getParliamentState().speakerVillagerEntityId();
        if (villagerSpeaker.isPresent()) {
            Entity found = Bukkit.getEntity(villagerSpeaker.get());
            if (found != null) {
                return found;
            }
        }
        for (Player member : onlineMembers(kingdom.getId())) {
            if (kingdomService
                    .getMembership(member.getUniqueId())
                    .filter(membership -> membership.getRank() == NobleRank.SPEAKER)
                    .isPresent()) {
                return member;
            }
        }
        return null;
    }

    private List<Player> onlineMembers(String kingdomId) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(online -> kingdomService
                        .getMembership(online.getUniqueId())
                        .filter(membership -> kingdomId.equals(membership.getKingdomId()))
                        .isPresent())
                .map(Player.class::cast)
                .toList();
    }

    private static String playerName(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? "Unknown" : name;
    }
}
