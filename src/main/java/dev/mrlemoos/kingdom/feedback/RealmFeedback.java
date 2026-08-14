package dev.mrlemoos.kingdom.feedback;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import dev.mrlemoos.kingdom.honours.HonourProclamation;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * The realm made audible. Every ceremony, verdict and grant the plugin already announces in chat is
 * answered here with a sound the subjects can hear and, where the event has a place, a burst of
 * particles over it.
 *
 * <p>Nothing here decides anything: it is presentation laid alongside the existing messages, and it
 * is silent whenever no server is running, so the domain services it is called from stay testable.
 */
public final class RealmFeedback {

    private RealmFeedback() {}

    // --- ceremonies of Parliament ----------------------------------------

    /** Royal assent: the dragon's growl over the Lords, and a totem's blessing on the Act. */
    public static void royalAssent(KingdomService kingdomService, String kingdomId) {
        kingdomSound(kingdomService, kingdomId, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.0f);
        atLords(kingdomService, kingdomId, Particle.TOTEM_OF_UNDYING, 40);
    }

    /** Assent withheld, or a bill lost in the division: the House says no. */
    public static void billFailed(KingdomService kingdomService, String kingdomId) {
        kingdomSound(kingdomService, kingdomId, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        atLords(kingdomService, kingdomId, Particle.ANGRY_VILLAGER, 12);
    }

    /** A bill carried in the Commons. */
    public static void billPassed(KingdomService kingdomService, String kingdomId) {
        kingdomSound(kingdomService, kingdomId, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
        atLords(kingdomService, kingdomId, Particle.HAPPY_VILLAGER, 20);
    }

    /** The Speaker calls a division. */
    public static void divisionOpened(KingdomService kingdomService, String kingdomId) {
        kingdomSound(kingdomService, kingdomId, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
    }

    /** An election, general or otherwise, returns its result. */
    public static void electionResult(KingdomService kingdomService, String kingdomId) {
        kingdomSound(kingdomService, kingdomId, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /** A monarch is crowned. */
    public static void coronation(Player monarch) {
        if (monarch == null || noServer()) {
            return;
        }
        burst(monarch.getLocation(), Particle.FIREWORK, 60);
        burst(monarch.getLocation(), Particle.TOTEM_OF_UNDYING, 30);
    }

    // --- the constabulary -------------------------------------------------

    /** An arrest: the cell door shuts behind the suspect. */
    public static void arrested(UUID suspectId) {
        Player suspect = player(suspectId);
        if (suspect == null) {
            return;
        }
        play(suspect, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f);
        title(suspect, "&cArrested", "&7You are taken into custody");
    }

    /** A sentence handed down by a judge, a jury, or the realm. */
    public static void sentenced(UUID convictId, String sentenceLabel) {
        Player convict = player(convictId);
        if (convict == null) {
            return;
        }
        play(convict, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.7f);
        title(convict, "&cSentenced", "&7" + sentenceLabel);
    }

    /** A guilty verdict read aloud to the realm — harsh and final. */
    public static void verdictGuilty(KingdomService kingdomService, String kingdomId, String accusedName) {
        kingdomSound(kingdomService, kingdomId, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);
        kingdomMessage(
                kingdomService,
                kingdomId,
                "&cThe court finds " + accusedName + " guilty.");
        burstAtCourt(kingdomService, kingdomId, Particle.SMOKE, 24);
    }

    /** An acquittal — relief for the accused and the gallery. */
    public static void verdictAcquittal(KingdomService kingdomService, String kingdomId, String accusedName) {
        kingdomSound(kingdomService, kingdomId, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        kingdomMessage(
                kingdomService,
                kingdomId,
                "&aThe court acquits " + accusedName + ".");
        burstAtCourt(kingdomService, kingdomId, Particle.HAPPY_VILLAGER, 24);
    }

    private static void burstAtCourt(KingdomService kingdomService, String kingdomId, Particle particle, int count) {
        if (noServer() || kingdomService == null || kingdomId == null) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        Optional<dev.mrlemoos.kingdom.model.police.CourtLocation> court =
                kingdom.get().getPoliceState().court();
        if (court.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(court.get().worldName());
        if (world == null) {
            return;
        }
        burst(
                new Location(world, court.get().x() + 0.5, court.get().y() + 1.0, court.get().z() + 0.5),
                particle,
                count);
    }

    /** A convict walks free at the end of their sentence. */
    public static void released(UUID convictId) {
        Player convict = player(convictId);
        if (convict == null) {
            return;
        }
        play(convict, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 1.4f);
        title(convict, "&aReleased", "&7Your sentence has been served");
    }

    // --- the city ---------------------------------------------------------

    /** The oath of allegiance at city hall: a blessing on the swearer, heard through the realm. */
    public static void oathOfAllegiance(Player swearer) {
        if (swearer == null || noServer()) {
            return;
        }
        play(swearer, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        burst(swearer.getLocation(), Particle.TOTEM_OF_UNDYING, 40);
        burst(swearer.getLocation(), Particle.FIREWORK, 30);
        burst(swearer.getLocation(), Particle.HAPPY_VILLAGER, 20);
        title(swearer, "&6Allegiance sworn", "&eYou are a member of this realm");
    }

    /** A build permit issued. */
    public static void permitGranted(UUID holderId) {
        Player holder = player(holderId);
        if (holder == null) {
            return;
        }
        play(holder, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        burst(holder.getLocation(), Particle.HAPPY_VILLAGER, 16);
        actionBar(holder, "&aBuild permit granted");
    }

    /** A build permit withdrawn. */
    public static void permitRevoked(UUID holderId) {
        Player holder = player(holderId);
        if (holder == null) {
            return;
        }
        play(holder, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        actionBar(holder, "&cBuild permit revoked");
    }

    // --- personal honours -------------------------------------------------

    /** A noble title granted, or taken away when {@code rankLabel} is absent. */
    public static void titleChanged(UUID playerId, String rankLabel) {
        titleChanged(null, null, playerId, rankLabel);
    }

    public static void titleChanged(
            KingdomService kingdomService, String kingdomId, UUID playerId, String rankLabel) {
        Player player = player(playerId);
        if (player == null) {
            return;
        }
        if (rankLabel == null || rankLabel.isBlank()) {
            play(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            title(player, "&7Untitled", "&7Your title has been surrendered");
            return;
        }
        Location here = player.getLocation();
        World world = here.getWorld();
        if (world != null) {
            world.playSound(here, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
            if (HonourProclamation.isKnighthood(rankLabel)) {
                world.playSound(here, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.4f);
            } else {
                world.playSound(here, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.4f);
            }
        }
        burst(here, Particle.FIREWORK, 50);
        burst(here, Particle.TOTEM_OF_UNDYING, 40);
        burst(here, Particle.END_ROD, 24);
        burst(here, Particle.HAPPY_VILLAGER, 16);
        title(player, "&6" + rankLabel, "&e" + HonourProclamation.screenSubheading(rankLabel), 10, 70, 20);
        if (kingdomService != null && kingdomId != null) {
            kingdomMessage(kingdomService, kingdomId, "&6" + HonourProclamation.line(player.getName(), rankLabel));
        }
    }

    /** A seat won at the polls. */
    public static void tookOffice(UUID playerId, String officeLabel) {
        Player player = player(playerId);
        if (player == null) {
            return;
        }
        play(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        title(player, "&6Elected", "&eYou are returned as " + officeLabel);
    }

    // --- the plumbing -----------------------------------------------------

    /** Every online subject of that kingdom. */
    public static List<Player> onlineMembers(KingdomService kingdomService, String kingdomId) {
        List<Player> members = new ArrayList<>();
        if (noServer() || kingdomService == null || kingdomId == null) {
            return members;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            kingdomService.getMembership(online.getUniqueId()).ifPresent(membership -> {
                if (kingdomId.equals(membership.getKingdomId())) {
                    members.add(online);
                }
            });
        }
        return members;
    }

    /** One line to every online subject of that kingdom. */
    public static void kingdomMessage(KingdomService kingdomService, String kingdomId, String message) {
        for (Player member : onlineMembers(kingdomService, kingdomId)) {
            member.sendMessage(c(message));
        }
    }

    /** One sound in the ears of every online subject of that kingdom. */
    public static void kingdomSound(
            KingdomService kingdomService, String kingdomId, Sound sound, float volume, float pitch) {
        for (Player member : onlineMembers(kingdomService, kingdomId)) {
            play(member, sound, volume, pitch);
        }
    }

    /** A burst of particles over the kingdom's House of Lords, where one has been set. */
    public static void atLords(
            KingdomService kingdomService, String kingdomId, Particle particle, int count) {
        if (noServer() || kingdomService == null) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        Optional<ChamberSite> lords = kingdom.get().getParliamentSites().lords();
        if (lords.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(lords.get().worldName());
        if (world == null) {
            return;
        }
        burst(new Location(world, lords.get().x(), lords.get().y() + 1.0, lords.get().z()), particle, count);
    }

    /** A burst of particles at a place, spread a block or so about it. */
    public static void burst(Location location, Particle particle, int count) {
        if (noServer() || location == null || location.getWorld() == null) {
            return;
        }
        location.getWorld().spawnParticle(particle, location, count, 0.6, 0.8, 0.6, 0.02);
    }

    /** A title over one player's eyes. */
    public static void title(Player player, String heading, String subheading) {
        title(player, heading, subheading, 5, 45, 15);
    }

    public static void title(
            Player player, String heading, String subheading, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        player.sendTitle(c(heading), c(subheading), fadeIn, stay, fadeOut);
    }

    /** A line across one player's action bar. */
    public static void actionBar(Player player, String message) {
        if (player == null) {
            return;
        }
        player.sendActionBar(ColourEncoder.component(message));
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Player player(UUID playerId) {
        if (noServer() || playerId == null) {
            return null;
        }
        return Bukkit.getPlayer(playerId);
    }

    /** True while no server is running — under test, or during shutdown. */
    private static boolean noServer() {
        try {
            return Bukkit.getServer() == null;
        } catch (Throwable unavailable) {
            return true;
        }
    }
}
