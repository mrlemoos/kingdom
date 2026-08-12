package dev.mrlemoos.kingdom.parliament;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runs the Coronation: when a throne that stood vacant is filled, the realm is summoned to the House
 * of Lords, the new monarch is crowned before them, and everyone is returned whence they came. Where
 * no House of Lords has been set the accession is merely proclaimed — a coronation never stands in
 * the way of the Crown being granted.
 */
public final class CoronationCeremony {

    private static final long CROWNING_DELAY_TICKS = 60L;
    private static final long RETURN_DELAY_TICKS = 140L;

    /** Blocks between the throne and the front rank of the summoned realm. */
    private static final int AUDIENCE_STAND_OFF = 5;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private PoliceTrialService policeTrialService;

    /** Monarchs crowned in absentia: the ceremony waits for them to log in. */
    private final Map<UUID, String> pendingCoronations = new ConcurrentHashMap<>();

    public CoronationCeremony(JavaPlugin plugin, KingdomService kingdomService) {
        this.plugin = plugin;
        this.kingdomService = kingdomService;
    }

    public void setPoliceTrialService(PoliceTrialService policeTrialService) {
        this.policeTrialService = policeTrialService;
    }

    /** True when nobody in that player's kingdom presently holds the Crown. */
    public boolean throneVacantFor(UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty()) {
            return false;
        }
        String kingdomId = membership.get().getKingdomId();
        return !kingdomService.hasPlayerWithRank(kingdomId, NobleRank.KING)
                && !kingdomService.hasPlayerWithRank(kingdomId, NobleRank.QUEEN);
    }

    /** Called once a title assignment has succeeded, with the throne's state from before it. */
    public void crownIfDue(UUID playerId, NobleRank assignedRank, boolean throneWasVacant) {
        if (!CoronationDecision.shouldCrown(assignedRank, throneWasVacant)) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        Player monarch = Bukkit.getPlayer(playerId);
        if (monarch == null) {
            pendingCoronations.put(playerId, kingdomId);
            return;
        }
        crown(monarch, kingdomId);
    }

    /** The coronation of a monarch crowned while offline waits at the door for them. */
    public void crownIfPendingOnJoin(Player player) {
        String kingdomId = pendingCoronations.remove(player.getUniqueId());
        if (kingdomId == null) {
            return;
        }
        crown(player, kingdomId);
    }

    private void crown(Player monarch, String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        Optional<ChamberSite> lords = kingdom.get().getParliamentSites().lords();
        World world = lords.isPresent() ? Bukkit.getWorld(lords.get().worldName()) : null;
        if (lords.isEmpty() || world == null) {
            proclaim(monarch, kingdom.get());
            return;
        }

        Map<UUID, Location> origins = new HashMap<>();
        origins.put(monarch.getUniqueId(), monarch.getLocation().clone());
        Location throne = ChamberSummons.landingFor(world, lords.get(), new int[] {0, 0}, monarch.getLocation());
        monarch.teleport(throne);

        List<Player> summoned = new ArrayList<>(ChamberSummons.onlineMembers(kingdomService, kingdomId));
        summoned.removeIf(member -> member.getUniqueId().equals(monarch.getUniqueId()));
        summoned.removeIf(member -> {
            if (policeTrialService != null && policeTrialService.isKingdomTeleportBlocked(member.getUniqueId())) {
                member.sendMessage(
                        c("&cYou remain confined under a prison sentence and cannot attend the Coronation."));
                return true;
            }
            return false;
        });
        List<int[]> offsets =
                SafeChamberLanding.frontOffsets(summoned.size(), throne.getYaw(), AUDIENCE_STAND_OFF);
        for (int i = 0; i < summoned.size(); i++) {
            Player member = summoned.get(i);
            origins.put(member.getUniqueId(), member.getLocation().clone());
            member.teleport(ChamberSummons.facing(
                    ChamberSummons.landingFor(world, throne, offsets.get(i)), throne));
            member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        }

        Bukkit.broadcastMessage(c("&6The realm of " + kingdom.get().getDisplayName()
                + " is summoned to the House of Lords for the Coronation."));
        for (Player member : ChamberSummons.onlineMembers(kingdomService, kingdomId)) {
            member.sendTitle(c("&6Coronation"), c("&eThe realm gathers at the throne"), 10, 60, 20);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            proclaim(monarch, kingdom.get());
            for (Player member : ChamberSummons.onlineMembers(kingdomService, kingdomId)) {
                member.playSound(member.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }, CROWNING_DELAY_TICKS);

        Bukkit.getScheduler().runTaskLater(plugin, () -> returnSummoned(origins), RETURN_DELAY_TICKS);
    }

    private void proclaim(Player monarch, Kingdom kingdom) {
        Bukkit.broadcastMessage(c("&6" + crownTitle(monarch.getUniqueId()) + " " + monarch.getName()
                + " is crowned sovereign of " + kingdom.getDisplayName() + "."));
        Bukkit.broadcastMessage(c("&eLong live the " + crownTitle(monarch.getUniqueId()) + "!"));
    }

    private String crownTitle(UUID playerId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(playerId);
        if (membership.isEmpty()) {
            return NobleRank.KING.displayTitle(TitleStyle.MASCULINE);
        }
        NobleRank rank = membership.get().getRank();
        TitleStyle style = membership.get().getTitleStyle();
        if (rank == null) {
            rank = NobleRank.KING;
        }
        return rank.displayTitle(style != null ? style : TitleStyle.MASCULINE);
    }

    private static void returnSummoned(Map<UUID, Location> origins) {
        origins.forEach((playerId, origin) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && origin.getWorld() != null) {
                player.teleport(origin);
            }
        });
    }
}
