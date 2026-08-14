package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.RealmDate;
import dev.mrlemoos.kingdom.calendar.RegnalDating;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.city.SeasonGazette;
import dev.mrlemoos.kingdom.helpers.ColourEncoder;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Turns the page of the realm calendar: advances the clock, proclaims the New Year and each kingdom's accession
 * anniversary, ticks the loyalty and morale recovery clocks for the day, and persists the day so the record
 * survives a restart.
 */
public final class RealmCalendarTask implements Runnable {

    /** The colour each season wears on the title screen. */
    private static final Map<Season, NamedTextColor> SEASON_COLOURS = new EnumMap<>(Map.of(
            Season.SPRING, NamedTextColor.GREEN,
            Season.SUMMER, NamedTextColor.YELLOW,
            Season.AUTUMN, NamedTextColor.GOLD,
            Season.WINTER, NamedTextColor.AQUA));

    private final KingdomService kingdomService;
    private final RealmCalendarService calendarService;
    private final YamlKingdomStore store;
    private long lastProclaimedDay = -1L;
    private LoyaltyService loyaltyService;
    private MoraleService moraleService;
    private FileConfiguration seasonConfig;

    public RealmCalendarTask(
            KingdomService kingdomService, RealmCalendarService calendarService, YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.calendarService = calendarService;
        this.store = store;
    }

    /**
     * Optional hooks (nullable setters, mirroring the war domain) so the day roll also advances
     * loyalty and morale recovery. Without them the tracks still record offences; nothing recovers.
     */
    public void setRecoveryServices(LoyaltyService loyaltyService, MoraleService moraleService) {
        this.loyaltyService = loyaltyService;
        this.moraleService = moraleService;
    }

    /** Gives the task the plugin config, so the tuned season profile governs how slowly morale mends. */
    public void setSeasonConfig(FileConfiguration seasonConfig) {
        this.seasonConfig = seasonConfig;
    }

    @Override
    public void run() {
        long realmDay = calendarService.currentRealmDay();
        // The season turn is claimed before the day-roll guard: it is proclaimed once per season, whatever the
        // hour and across a restart, because the day it was last proclaimed is persisted.
        proclaimSeasonTurn(realmDay);
        if (realmDay == lastProclaimedDay) {
            return;
        }
        boolean firstSweep = lastProclaimedDay < 0L;
        lastProclaimedDay = realmDay;
        if (firstSweep) {
            return;
        }

        RealmDate date = dev.mrlemoos.kingdom.calendar.RealmCalendar.dateOf(realmDay);
        if (date.isNewYear()) {
            Bukkit.broadcastMessage(c(
                    "&6The realm turns the page: " + date.format() + ", Realm Year " + date.realmYear() + "."));
        }
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            calendarService.currentReign(kingdom.getId())
                    .filter(reign -> RegnalDating.isAccessionAnniversary(reign, realmDay))
                    .ifPresent(reign -> {
                        int year = RegnalDating.regnalYear(reign, realmDay);
                        Bukkit.broadcastMessage(c("&6" + kingdom.getDisplayName() + " enters the "
                                + year + dev.mrlemoos.kingdom.calendar.RealmCalendar.ordinalSuffix(year)
                                + " year of the reign of " + reign.styledName() + "."));
                    });
        }
        tickRecoveryClocks(realmDay);
        store.saveFrom(kingdomService);
    }

    /** Tells the realm what has come upon it on the first day of a season, and only then. */
    private void proclaimSeasonTurn(long realmDay) {
        Optional<Season> turning = calendarService.seasonTurn().claim(realmDay);
        if (turning.isEmpty()) {
            return;
        }
        Season season = turning.get();
        Bukkit.broadcastMessage(c("&6" + season.proclamation()));
        showSeasonTitle(season);
        criersCry(season);
        // Persist at once: the day roll below may not run for hours, and the word must not go out twice.
        store.saveFrom(kingdomService);
    }

    /** Throws the season across every screen, in the colour the season wears. */
    private void showSeasonTitle(Season season) {
        Title title = Title.title(
                ColourEncoder.component("&l" + season.displayName()).colorIfAbsent(SEASON_COLOURS.get(season)),
                ColourEncoder.component(season.banner()).colorIfAbsent(NamedTextColor.GRAY),
                Times.times(Duration.ofMillis(500L), Duration.ofSeconds(4L), Duration.ofSeconds(1L)));
        Bukkit.getServer().showTitle(title);
    }

    /**
     * Hangs the season on every Gazette, so each kingdom's town crier has the news on his ticker for
     * those who were not about when the word first went out. A kingdom with no capital has no crier
     * and no Gazette to hang it on.
     */
    private void criersCry(Season season) {
        long mcDay = calendarService.epochWorldDay() + calendarService.currentRealmDay();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getCityState().hasCapital()) {
                kingdom.getCityState().addGazettePost(SeasonGazette.post(season, mcDay));
            }
        }
    }

    /**
     * One recovery tick per realm day for every subject with a tier on record. Faithful subjects
     * hold no entry on the political track, so only those actually recovering are visited.
     */
    private void tickRecoveryClocks(long realmDay) {
        if (loyaltyService != null) {
            for (UUID playerId : new LinkedHashSet<>(loyaltyService.store().allTiersView().keySet())) {
                loyaltyService.tickRecovery(playerId, realmDay);
            }
        }
        if (moraleService != null) {
            double moraleRecoveryFactor = currentSeasonProfile().moraleRecoveryFactor();
            for (UUID playerId : new LinkedHashSet<>(moraleService.store().allTiersView().keySet())) {
                moraleService.tickRecovery(playerId, realmDay, moraleRecoveryFactor);
            }
        }
    }

    /**
     * The profile of the season in force, as tuned in config; spring's neutral figures until the plugin
     * config is given, so a soldier mends at the configured pace and nothing changes.
     */
    private SeasonProfile currentSeasonProfile() {
        FileConfiguration config = this.seasonConfig;
        Season season = calendarService.currentSeason();
        return config == null ? SeasonProfile.defaults(season) : SeasonProfile.fromPluginConfig(config, season);
    }
}
