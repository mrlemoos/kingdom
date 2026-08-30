package dev.mrlemoos.kingdom.church;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The mass as the realm sees it: the bell tolls over the church, the celebrant speaks the liturgy
 * from a bubble above his head, and every subject who comes to the altar is blessed with light and
 * sound. Nothing here decides anything — {@link ChurchService} rules on who is blessed and when.
 */
public final class MassCeremony {

    /** How high above the celebrant's head the words hang. */
    private static final double BUBBLE_HEIGHT = 2.4d;

    /** The liturgy, and the tick each line is spoken on. */
    private static final long[] LITURGY_TICKS = {20L, 100L, 180L, 260L};

    private static final String[] LITURGY = {
        "Peace be upon this realm.",
        "We are gathered in the sight of the Crown and of Heaven.",
        "Let all who come in good faith receive the blessing.",
        "The church stands open until nightfall. Come and be blessed."
    };

    /** How long a spoken line hangs before it fades. */
    private static final long BUBBLE_TICKS = 80L;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final ClericService clericService;

    /** The bubble now hanging over each kingdom's celebrant, so the next line replaces it. */
    private final Map<String, UUID> bubbles = new HashMap<>();

    public MassCeremony(
            JavaPlugin plugin,
            KingdomService kingdomService,
            ChurchService churchService,
            ClericService clericService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.clericService = Objects.requireNonNull(clericService, "clericService");
    }

    /** The bell, the liturgy and the light: mass is called in this realm. */
    public void open(Kingdom kingdom, Celebrant celebrant) {
        String kingdomId = kingdom.getId();
        Optional<Location> altar = altar(kingdomId);
        RealmFeedback.kingdomMessage(
                kingdomService,
                kingdomId,
                "&6The bell tolls over " + kingdom.getDisplayName()
                        + ": mass is held at the church. Come and be blessed.");
        RealmFeedback.kingdomSound(kingdomService, kingdomId, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
            member.sendTitle(c("&6Mass"), c("&eThe church stands open"), 10, 60, 20);
        }
        if (altar.isEmpty()) {
            return;
        }
        Location here = altar.get();
        World world = here.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(here, Sound.BLOCK_BELL_RESONATE, 1.4f, 1.0f);
        world.spawnParticle(Particle.END_ROD, here.clone().add(0, 1.2, 0), 60, 0.8, 1.2, 0.8, 0.02);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, here.clone().add(0, 1.2, 0), 40, 0.7, 1.0, 0.7, 0.1);
        for (int i = 0; i < LITURGY.length; i++) {
            String line = LITURGY[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> say(kingdomId, celebrant, line), LITURGY_TICKS[i]);
        }
    }

    /** One subject, come to the altar: the blessing itself, laid on with light. */
    public void bless(Kingdom kingdom, Celebrant celebrant, Player subject) {
        int ticks = churchService.config().blessingSeconds() * 20;
        subject.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 1));
        subject.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, 0));
        subject.sendMessage(c("&aA blessing is laid upon you."));
        subject.sendTitle(c("&6Blessed"), c("&eYou have attended mass"), 10, 50, 20);
        subject.playSound(subject.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        subject.playSound(subject.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.6f);
        World world = subject.getWorld();
        Location over = subject.getLocation().clone().add(0, 1.0, 0);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, over, 40, 0.5, 0.8, 0.5, 0.2);
        world.spawnParticle(Particle.END_ROD, over, 25, 0.4, 0.9, 0.4, 0.01);
        say(kingdom.getId(), celebrant, "Go in peace, " + subject.getName() + ".");
    }

    // --- the celebrant's voice --------------------------------------------

    /** A line from the celebrant: a bubble over his head, and the words to everyone at the altar. */
    private void say(String kingdomId, Celebrant celebrant, String line) {
        Optional<LivingEntity> speaker = speaker(kingdomId, celebrant);
        if (speaker.isEmpty()) {
            return;
        }
        String voice = celebrant == Celebrant.CLERIC ? "Cleric" : "Priest";
        for (Player nearby : atAltar(kingdomId)) {
            nearby.sendMessage(c("&6[" + voice + "] &f" + line));
        }
        Location head = speaker.get().getLocation().clone().add(0, BUBBLE_HEIGHT, 0);
        World world = head.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(head, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.4f);
        clearBubble(kingdomId);
        TextDisplay bubble = world.spawn(head, TextDisplay.class, spawned -> {
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setSeeThrough(true);
            spawned.setShadowed(false);
            spawned.setPersistent(false);
            spawned.setTransformation(new Transformation(
                    new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
            spawned.text(ColourEncoder.component("&f" + line)
                    .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE));
        });
        bubbles.put(kingdomId, bubble.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> clearBubble(kingdomId, bubble.getUniqueId()), BUBBLE_TICKS);
    }

    private void clearBubble(String kingdomId) {
        UUID existing = bubbles.remove(kingdomId);
        if (existing != null) {
            removeEntity(existing);
        }
    }

    /** Clears the bubble only if it is still the one this line hung, never a later line's. */
    private void clearBubble(String kingdomId, UUID bubbleId) {
        if (bubbleId.equals(bubbles.get(kingdomId))) {
            bubbles.remove(kingdomId);
        }
        removeEntity(bubbleId);
    }

    private static void removeEntity(UUID entityId) {
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) {
            entity.remove();
        }
    }

    /** Whoever is holding the rite in the flesh: the cleric villager, or the priest at his altar. */
    private Optional<LivingEntity> speaker(String kingdomId, Celebrant celebrant) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        if (celebrant == Celebrant.CLERIC) {
            Optional<Villager> cleric = clericService.findCleric(kingdom.get());
            return cleric.isPresent() ? Optional.of(cleric.get()) : Optional.empty();
        }
        Optional<UUID> priestId = churchService.priest(kingdomId);
        if (priestId.isEmpty()) {
            return Optional.empty();
        }
        Player priest = Bukkit.getPlayer(priestId.get());
        return priest == null ? Optional.empty() : Optional.of(priest);
    }

    private java.util.List<Player> atAltar(String kingdomId) {
        java.util.List<Player> here = new java.util.ArrayList<>();
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
            if (ChurchPresence.atChurch(churchService, kingdomId, member)) {
                here.add(member);
            }
        }
        return here;
    }

    private Optional<Location> altar(String kingdomId) {
        Optional<ChurchSite> site = churchService.church(kingdomId);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(site.get().worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, site.get().x(), site.get().y(), site.get().z()));
    }
}
