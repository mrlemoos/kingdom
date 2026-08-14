package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lets the season set how thickly the hostile dark spawns: winter sends more of it abroad, riding the vanilla
 * spawn event so light, biome and the server's own mob cap all still hold.
 *
 * <p>Overworld only — the Nether and the End have no seasons. Only natural spawns are weighed, and only those
 * at night or out of the sky's reach, since a mob boosted into the daylight merely burns at dawn.
 */
public final class SeasonalHostileSpawnListener implements Listener {

    /** The hour past which the overworld surface is dark enough for a spawn to survive. */
    private static final long NIGHTFALL_TICK = 13000L;

    /** The hour at which the overworld surface grows light again. */
    private static final long DAYBREAK_TICK = 23000L;

    private final JavaPlugin plugin;
    private final RealmCalendarService calendarService;
    private final Random random = new Random();

    public SeasonalHostileSpawnListener(JavaPlugin plugin, RealmCalendarService calendarService) {
        this.plugin = plugin;
        this.calendarService = calendarService;
    }

    /**
     * Weighs a season's hostile spawn factor against a roll in {@code [0, 1)}. A factor beyond one sends that
     * share of spawns a companion; a factor of one or less never interferes, no season thinning the dark.
     */
    public static boolean shouldReinforce(double hostileSpawnFactor, double roll) {
        return hostileSpawnFactor > 1.0 && roll < hostileSpawnFactor - 1.0;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != SpawnReason.NATURAL) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        Location location = event.getLocation();
        World world = entity.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (!survivesTheHour(world, location)) {
            return;
        }
        SeasonProfile profile = currentSeasonProfile();
        if (!shouldReinforce(profile.hostileSpawnFactor(), random.nextDouble())) {
            return;
        }
        world.spawnEntity(location, entity.getType(), SpawnReason.NATURAL);
    }

    /** True where a fresh mob would not simply burn away: underground, under a roof, or after nightfall. */
    private static boolean survivesTheHour(World world, Location location) {
        if (location.getBlock().getLightFromSky() == 0) {
            return true;
        }
        long time = world.getTime();
        return time >= NIGHTFALL_TICK && time < DAYBREAK_TICK;
    }

    /** The profile of the season in force, as tuned in config; spring's neutral figures before the calendar is set. */
    private SeasonProfile currentSeasonProfile() {
        RealmCalendarService service = this.calendarService;
        Season season = service == null ? Season.SPRING : service.currentSeason();
        return SeasonProfile.fromPluginConfig(plugin.getConfig(), season);
    }
}
