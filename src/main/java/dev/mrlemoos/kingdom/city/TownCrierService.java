package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.GazetteBoard;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Town Crier: a nitwit villager at the capital who holds the Gazette. Outside the economy,
 * elections and the constabulary. A {@link TextDisplay} above its head cycles the newest posts.
 */
public final class TownCrierService {

    public static final String NAMETAG = ColourEncoder.c("&6Town Crier");
    public static final String EMPTY_TICKER = "Hear ye! No news today.";
    public static final double TICKER_RANGE_BLOCKS = 24.0;
    public static final long TICKER_INTERVAL_TICKS = 80L; // 4 seconds
    private static final double DISPLAY_HEIGHT = 2.35;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final NamespacedKey crierTagKey;
    private final NamespacedKey kingdomTagKey;
    private final NamespacedKey displayTagKey;
    private final Map<String, UUID> displayEntityIds = new HashMap<>();
    private final Map<String, Integer> tickerIndexes = new HashMap<>();
    private BukkitTask tickerTask;

    public TownCrierService(JavaPlugin plugin, KingdomService kingdomService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.crierTagKey = new NamespacedKey(plugin, "town_crier");
        this.kingdomTagKey = new NamespacedKey(plugin, "town_crier_kingdom");
        this.displayTagKey = new NamespacedKey(plugin, "town_crier_display");
    }

    public NamespacedKey crierTagKey() {
        return crierTagKey;
    }

    public boolean isTownCrier(Entity entity) {
        if (entity == null) {
            return false;
        }
        Byte tag = entity.getPersistentDataContainer().get(crierTagKey, PersistentDataType.BYTE);
        return tag != null && tag == 1;
    }

    public Optional<String> kingdomIdOf(Entity entity) {
        if (!isTownCrier(entity)) {
            return Optional.empty();
        }
        String kingdomId = entity.getPersistentDataContainer().get(kingdomTagKey, PersistentDataType.STRING);
        return kingdomId == null || kingdomId.isBlank() ? Optional.empty() : Optional.of(kingdomId);
    }

    public Optional<Villager> findCrier(Kingdom kingdom) {
        if (kingdom == null) {
            return Optional.empty();
        }
        Optional<UUID> entityId = kingdom.getCityState().townCrierEntityId();
        if (entityId.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = Bukkit.getEntity(entityId.get());
        if (entity instanceof Villager villager && villager.isValid()) {
            return Optional.of(villager);
        }
        return Optional.empty();
    }

    public Optional<Villager> spawn(Kingdom kingdom, CapitalLocation capital) {
        if (kingdom == null || capital == null) {
            return Optional.empty();
        }
        despawn(kingdom);

        Optional<Location> site = toBukkitLocation(capital);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        Location location = site.get();
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        location.getChunk();

        String kingdomId = kingdom.getId();
        Villager crier = world.spawn(location, Villager.class, spawned -> configure(spawned, kingdomId));
        kingdom.getCityState().setTownCrierEntityId(crier.getUniqueId());
        ensureDisplay(kingdom, crier);
        ensureTickerRunning();
        return Optional.of(crier);
    }

    public void despawn(Kingdom kingdom) {
        if (kingdom == null) {
            return;
        }
        String kingdomId = kingdom.getId();
        despawnDisplay(kingdomId);
        KingdomCityState city = kingdom.getCityState();
        Optional<UUID> entityId = city.townCrierEntityId();
        if (entityId.isPresent()) {
            Entity entity = Bukkit.getEntity(entityId.get());
            if (entity != null) {
                entity.remove();
            }
        }
        city.clearTownCrierEntityId();
        tickerIndexes.remove(kingdomId);
    }

    public boolean reconcile(Kingdom kingdom) {
        if (kingdom == null) {
            return false;
        }
        KingdomCityState city = kingdom.getCityState();
        Optional<CapitalLocation> capital = city.capital();
        if (capital.isEmpty()) {
            if (city.townCrierEntityId().isEmpty() && !displayEntityIds.containsKey(kingdom.getId())) {
                return false;
            }
            despawn(kingdom);
            return true;
        }
        Optional<Villager> standing = findCrier(kingdom);
        if (standing.isPresent()) {
            configure(standing.get(), kingdom.getId());
            ensureDisplay(kingdom, standing.get());
            ensureTickerRunning();
            return false;
        }
        return spawn(kingdom, capital.get()).isPresent();
    }

    public boolean reconcileAll() {
        boolean changed = false;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (reconcile(kingdom)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Starts the 4-second ticker if it is not already running. */
    public void ensureTickerRunning() {
        if (tickerTask != null) {
            return;
        }
        tickerTask = plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, this::tickDisplays, TICKER_INTERVAL_TICKS, TICKER_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        for (String kingdomId : List.copyOf(displayEntityIds.keySet())) {
            despawnDisplay(kingdomId);
        }
    }

    private void tickDisplays() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            Optional<Villager> crier = findCrier(kingdom);
            if (crier.isEmpty()) {
                continue;
            }
            if (!playerNearby(crier.get().getLocation())) {
                continue;
            }
            TextDisplay display = ensureDisplay(kingdom, crier.get());
            if (display == null) {
                continue;
            }
            List<GazettePost> newest = kingdom.getCityState().newestGazettePosts(GazetteBoard.TICKER_SIZE);
            if (newest.isEmpty()) {
                display.text(ColourEncoder.component("&7" + EMPTY_TICKER)
                        .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                continue;
            }
            int index = tickerIndexes.getOrDefault(kingdom.getId(), 0) % newest.size();
            GazettePost post = newest.get(index);
            display.text(ColourEncoder.component(tickerLine(post))
                    .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            tickerIndexes.put(kingdom.getId(), index + 1);
        }
    }

    private static String tickerLine(GazettePost post) {
        if (post.kind() == GazettePostKind.DECREE) {
            return "&6&lDECREE &f" + post.title();
        }
        return "&7" + post.title();
    }

    private static boolean playerNearby(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location)
                    <= TICKER_RANGE_BLOCKS * TICKER_RANGE_BLOCKS) {
                return true;
            }
        }
        return false;
    }

    private TextDisplay ensureDisplay(Kingdom kingdom, Villager crier) {
        String kingdomId = kingdom.getId();
        UUID displayId = displayEntityIds.get(kingdomId);
        if (displayId != null) {
            Entity existing = Bukkit.getEntity(displayId);
            if (existing instanceof TextDisplay textDisplay && textDisplay.isValid()) {
                textDisplay.teleport(crier.getLocation().clone().add(0, DISPLAY_HEIGHT, 0));
                return textDisplay;
            }
        }
        Location above = crier.getLocation().clone().add(0, DISPLAY_HEIGHT, 0);
        World world = above.getWorld();
        if (world == null) {
            return null;
        }
        TextDisplay display = world.spawn(above, TextDisplay.class, spawned -> {
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setSeeThrough(true);
            spawned.setShadowed(false);
            spawned.setPersistent(false);
            spawned.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()));
            spawned.getPersistentDataContainer().set(displayTagKey, PersistentDataType.BYTE, (byte) 1);
            spawned.text(ColourEncoder.component("&7" + EMPTY_TICKER)
                    .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        });
        displayEntityIds.put(kingdomId, display.getUniqueId());
        return display;
    }

    private void despawnDisplay(String kingdomId) {
        UUID displayId = displayEntityIds.remove(kingdomId);
        if (displayId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(displayId);
        if (entity != null) {
            entity.remove();
        }
    }

    private void configure(Villager crier, String kingdomId) {
        crier.setAI(false);
        crier.setInvulnerable(true);
        crier.setPersistent(true);
        crier.setRemoveWhenFarAway(false);
        crier.setSilent(true);
        crier.setProfession(Villager.Profession.NONE);
        crier.setVillagerLevel(1);
        crier.setCustomName(NAMETAG);
        crier.setCustomNameVisible(true);
        crier.getPersistentDataContainer().set(crierTagKey, PersistentDataType.BYTE, (byte) 1);
        crier.getPersistentDataContainer().set(kingdomTagKey, PersistentDataType.STRING, kingdomId);
    }

    private static Optional<Location> toBukkitLocation(CapitalLocation capital) {
        World world = Bukkit.getWorld(capital.worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
                world, capital.x(), capital.y(), capital.z(), capital.yaw(), capital.pitch()));
    }
}
