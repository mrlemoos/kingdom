package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import java.util.Random;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lets the season set the pace of the harvest: crops come on slowly through winter and apace through summer
 * and autumn, riding the vanilla grow event so light, water and farmland rules all still hold.
 *
 * <p>Overworld only — the Nether and the End have no seasons.
 */
public final class SeasonalCropGrowthListener implements Listener {

    /** What the season makes of a growth the world has offered. */
    public enum GrowthVerdict {
        /** The growth is refused outright; the crop stands where it is. */
        CANCEL,
        /** The growth proceeds exactly as vanilla intended. */
        NORMAL,
        /** The growth proceeds and carries one further stage with it. */
        BOOST
    }

    private final JavaPlugin plugin;
    private final RealmCalendarService calendarService;
    private final Random random = new Random();

    public SeasonalCropGrowthListener(JavaPlugin plugin, RealmCalendarService calendarService) {
        this.plugin = plugin;
        this.calendarService = calendarService;
    }

    /**
     * Weighs a season's crop growth factor against a roll in {@code [0, 1)}. A factor short of one refuses that
     * share of growths; a factor beyond one carries that share an extra stage; a factor of one never interferes.
     */
    public static GrowthVerdict decide(double cropGrowthFactor, double roll) {
        if (cropGrowthFactor < 1.0) {
            return roll < 1.0 - cropGrowthFactor ? GrowthVerdict.CANCEL : GrowthVerdict.NORMAL;
        }
        if (cropGrowthFactor > 1.0) {
            return roll < cropGrowthFactor - 1.0 ? GrowthVerdict.BOOST : GrowthVerdict.NORMAL;
        }
        return GrowthVerdict.NORMAL;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.getBlock().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        SeasonProfile profile = currentSeasonProfile();
        GrowthVerdict verdict = decide(profile.cropGrowthFactor(), random.nextDouble());
        if (verdict == GrowthVerdict.CANCEL) {
            event.setCancelled(true);
            return;
        }
        if (verdict == GrowthVerdict.BOOST) {
            advanceOneStage(event.getNewState());
        }
    }

    /** Carries an ageable crop one stage beyond what the event already grants it, never past its last. */
    private void advanceOneStage(BlockState newState) {
        BlockData data = newState.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }
        ageable.setAge(ageable.getAge() + 1);
        newState.setBlockData(ageable);
    }

    /** The profile of the season in force, as tuned in config; spring's neutral figures before the calendar is set. */
    private SeasonProfile currentSeasonProfile() {
        RealmCalendarService service = this.calendarService;
        Season season = service == null ? Season.SPRING : service.currentSeason();
        return SeasonProfile.fromPluginConfig(plugin.getConfig(), season);
    }
}
