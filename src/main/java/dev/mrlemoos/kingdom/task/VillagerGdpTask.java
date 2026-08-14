package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.feedback.DailyRealmReport;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomicParticipant;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomicParticipants;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyConfig;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyDayResult;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyProcessor;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.hearth.BukkitHearthScan;
import dev.mrlemoos.kingdom.hearth.ColdDayOutcome;
import dev.mrlemoos.kingdom.hearth.ColdSubject;
import dev.mrlemoos.kingdom.hearth.HearthConfig;
import dev.mrlemoos.kingdom.hearth.HearthDayService;
import dev.mrlemoos.kingdom.hearth.HearthSite;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.levy.LevyDayOutcome;
import dev.mrlemoos.kingdom.war.levy.LevyUpkeepService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.siege.FieldMoraleDecayService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerGdpTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 24000L;

    private final JavaPlugin plugin;
    private final EconomyCoordinator coordinator;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final VillagerEconomyConfig villagerEconomyConfig;
    private final VillagerEconomyProcessor processor;
    private final Random random;
    private RealmCalendarService calendarService;
    private LevyUpkeepService levyUpkeepService;
    private MusterService musterService;
    private FieldMoraleDecayService fieldMoraleDecayService;
    private WarService warService;
    private HearthDayService hearthDayService;
    private HearthConfig hearthConfig;
    private VillagerMpEntityService villagerMpEntityService;

    public VillagerGdpTask(
            JavaPlugin plugin,
            EconomyCoordinator coordinator,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            VillagerEconomyConfig villagerEconomyConfig) {
        this(plugin, coordinator, kingdomService, economyStore, villagerEconomyConfig, new VillagerEconomyProcessor(), new Random());
    }

    VillagerGdpTask(
            JavaPlugin plugin,
            EconomyCoordinator coordinator,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            VillagerEconomyConfig villagerEconomyConfig,
            VillagerEconomyProcessor processor,
            Random random) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.villagerEconomyConfig = villagerEconomyConfig != null ? villagerEconomyConfig : VillagerEconomyConfig.defaults();
        this.processor = Objects.requireNonNull(processor, "processor");
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Gives the task the realm calendar, so the day's yield is reckoned by the season in force. */
    public void setCalendarService(RealmCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Gives the task the levy's reckoning, so each realm day's wage bill is charged on the treasury
     * with the rest of the day's account. The muster is optional: without it only the standing
     * roster is paid for.
     */
    public void setLevyUpkeep(LevyUpkeepService levyUpkeepService, MusterService musterService) {
        this.levyUpkeepService = levyUpkeepService;
        this.musterService = musterService;
    }

    /**
     * Gives the task the field's morale reckoning, so a hard season wears down the men kept under
     * arms in a war with the rest of the day's business. Both are optional; without either, the
     * field is left alone.
     */
    public void setFieldMoraleDecay(FieldMoraleDecayService fieldMoraleDecayService, WarService warService) {
        this.fieldMoraleDecayService = fieldMoraleDecayService;
        this.warService = warService;
    }

    /**
     * Gives the task the realm's hearths, so a winter day's warmth — and the cold that follows want
     * of it — is reckoned with the rest of the day's account. Without them, winter costs nobody
     * anything but the season's own yield.
     */
    public void setHearths(
            HearthDayService hearthDayService,
            HearthConfig hearthConfig,
            VillagerMpEntityService villagerMpEntityService) {
        this.hearthDayService = hearthDayService;
        this.hearthConfig = hearthConfig != null ? hearthConfig : HearthConfig.defaults();
        this.villagerMpEntityService = villagerMpEntityService;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        EconomyConfig config = coordinator.config();
        EconomyService economyService = coordinator.economyService();
        SeasonProfile season = currentSeasonProfile();
        boolean dirty = false;

        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String regionId = kingdom.getWorldGuardRegion();
            if (regionId == null || regionId.isBlank()) {
                continue;
            }

            String worldName = kingdomService.resolveWorldName(kingdom);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            List<VillagerEconomicParticipant> productive = collectProductiveParticipants(world, kingdom, regionId, config);
            List<MpSeat> seatedVillagerMps = kingdom.getElectionState().seatsView().values().stream().toList();
            List<VillagerEconomicParticipant> participants =
                    VillagerEconomicParticipants.merge(productive, seatedVillagerMps);

            Map<UUID, Double> coldYieldFactors = settleHearths(world, kingdom, regionId, season);
            long epochDay = world.getFullTime() / 24000L;
            double treasuryBefore = economyService.getTreasuryBalance(kingdom.getId());
            double taxBefore = economyService.getTotalTaxRevenue(kingdom.getId());
            VillagerEconomyDayResult day = processor.processKingdomDay(
                    kingdom.getId(),
                    participants,
                    economyService,
                    config,
                    villagerEconomyConfig,
                    epochDay,
                    random,
                    season,
                    chargedKingdomId -> chargeLevyUpkeep(kingdom, chargedKingdomId, season),
                    coldYieldFactors);
            readTheDayToTheRealm(kingdom, participants, economyService, day, treasuryBefore, taxBefore);
            decayFieldMorale(kingdom, season);
            dirty = true;
        }

        if (dirty) {
            economyStore.saveFrom(economyService);
        }
    }

    /**
     * The winter day at the hearths: each burns the day's fuel out of the container beside it, the
     * villagers within reach are warm, and the rest go a day colder. The world is asked afresh — no
     * hearth is ever recorded — and only the cold-day counts outlive the day.
     */
    private Map<UUID, Double> settleHearths(World world, Kingdom kingdom, String regionId, SeasonProfile season) {
        HearthDayService hearths = this.hearthDayService;
        if (hearths == null) {
            return Map.of();
        }
        HearthConfig config = this.hearthConfig != null ? this.hearthConfig : HearthConfig.defaults();
        List<HearthSite> sites = season.hearthsRequired()
                ? BukkitHearthScan.hearthsIn(world, regionId, config)
                : List.of();
        List<ColdSubject> subjects = collectColdSubjects(world, regionId);
        ColdDayOutcome outcome = hearths.settleDay(kingdom.getId(), sites, subjects, config, season.hearthsRequired());
        if (!outcome.striking().isEmpty()) {
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdom.getId(),
                    "The cold bites: " + outcome.striking().size()
                            + (outcome.striking().size() == 1 ? " villager has" : " villagers have")
                            + " left off work for want of a burning hearth.");
        }
        if (villagerMpEntityService != null) {
            villagerMpEntityService.reconcileAllTerritoryVillagerNametags();
        }
        return outcome.yieldFactors();
    }

    /** Every villager standing in the kingdom's territory, and which of them never strike. */
    private List<ColdSubject> collectColdSubjects(World world, String regionId) {
        List<ColdSubject> subjects = new ArrayList<>();
        String worldName = world.getName();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            Location location = villager.getLocation();
            if (!isInRegion(location, worldName, regionId, location)) {
                continue;
            }
            subjects.add(new ColdSubject(
                    villager.getUniqueId(), location.getX(), location.getY(), location.getZ(), neverStrikes(villager)));
        }
        return subjects;
    }

    /** Seated villager MPs and Lords of the Treasury keep working, cold or not. */
    private boolean neverStrikes(Villager villager) {
        VillagerMpEntityService villagers = this.villagerMpEntityService;
        if (villagers == null) {
            return false;
        }
        return villagers.isTreasuryLordVillager(villager)
                || villagers.isSeatedMpVillager(villager)
                || villagers.isKingdomTaggedMpVillager(villager)
                || villagers.isTownCrierVillager(villager);
    }

    /**
     * The day's levy upkeep, charged on the treasury in the day's account after the yield and its
     * taxes and before the escheat. The realm hears its warning, and any desertion, at once.
     */
    private void chargeLevyUpkeep(Kingdom kingdom, String kingdomId, SeasonProfile season) {
        LevyUpkeepService levy = this.levyUpkeepService;
        if (levy == null) {
            return;
        }
        RealmCalendarService calendar = this.calendarService;
        long realmDay = calendar == null ? 0L : calendar.currentRealmDay();
        MusterService muster = this.musterService;
        Set<UUID> mustered = muster == null ? Set.of() : muster.answeredMembers(kingdomId);
        LevyDayOutcome outcome = levy.settleDay(kingdomId, mustered, season, realmDay);
        for (String announcement : outcome.announcements()) {
            RealmFeedback.kingdomMessage(kingdomService, kingdom.getId(), announcement);
        }
        for (UUID deserter : outcome.deserters()) {
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdom.getId(),
                    subjectName(deserter) + " has deserted the standing roster for want of pay.");
        }
    }

    /**
     * A hard season's toll on the men this kingdom keeps in the field, taken once a realm day. Only
     * a kingdom at war has men in the field, and only a season that bites takes anything from them.
     */
    private void decayFieldMorale(Kingdom kingdom, SeasonProfile season) {
        FieldMoraleDecayService decay = this.fieldMoraleDecayService;
        WarService wars = this.warService;
        if (decay == null || wars == null || season.siegeMoraleDecayDays() <= 0) {
            return;
        }
        Optional<ActiveWar> war = wars.activeWarFor(kingdom.getId());
        if (war.isEmpty()) {
            return;
        }
        RealmCalendarService calendar = this.calendarService;
        long realmDay = calendar == null ? 0L : calendar.currentRealmDay();
        List<UUID> worn = decay.decayDay(war.get().id(), kingdom.getId(), season, realmDay);
        if (!worn.isEmpty()) {
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdom.getId(),
                    "The season bites hard in the field: " + worn.size()
                            + (worn.size() == 1 ? " soldier loses" : " soldiers lose") + " heart.");
        }
    }

    /** A deserter as the realm knows him; the bare id when the server has never seen the name. */
    private static String subjectName(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null && !name.isBlank() ? name : playerId.toString();
    }

    /** The profile of the season in force, as tuned in config; spring's neutral figures before the calendar is set. */
    private SeasonProfile currentSeasonProfile() {
        RealmCalendarService service = this.calendarService;
        Season season = service == null ? Season.SPRING : service.currentSeason();
        return SeasonProfile.fromPluginConfig(plugin.getConfig(), season);
    }

    /**
     * Reads the day's account to the realm. Every figure is one the processor has just settled — the
     * treasury's movement, the tax it took, the villagers' output, and the richest wallet among them.
     */
    private void readTheDayToTheRealm(
            Kingdom kingdom,
            List<VillagerEconomicParticipant> participants,
            EconomyService economyService,
            VillagerEconomyDayResult day,
            double treasuryBefore,
            double taxBefore) {
        VillagerEconomicParticipant richest = null;
        double richestBalance = 0.0;
        for (VillagerEconomicParticipant participant : participants) {
            double balance = economyService.getVillagerWalletBalance(kingdom.getId(), participant.villagerId());
            if (richest == null || balance > richestBalance) {
                richest = participant;
                richestBalance = balance;
            }
        }
        DailyRealmReport report = new DailyRealmReport(
                kingdom.getDisplayName(),
                DailyRealmReport.delta(economyService.getTreasuryBalance(kingdom.getId()), treasuryBefore),
                DailyRealmReport.delta(economyService.getTotalTaxRevenue(kingdom.getId()), taxBefore),
                day.totalGdpCredited(),
                richest == null ? null : professionLabel(richest.profession()),
                richestBalance);
        RealmFeedback.kingdomMessage(kingdomService, kingdom.getId(), report.line());
    }

    /** A profession as the realm hears it: {@code weaponsmith} becomes {@code Weaponsmith}. */
    private static String professionLabel(String profession) {
        if (profession == null || profession.isBlank()) {
            return "Commoner";
        }
        return Character.toUpperCase(profession.charAt(0)) + profession.substring(1);
    }

    private List<VillagerEconomicParticipant> collectProductiveParticipants(
            World world, Kingdom kingdom, String regionId, EconomyConfig config) {
        List<VillagerEconomicParticipant> participants = new ArrayList<>();
        String worldName = world.getName();
        int position = 0;

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!isProductiveVillager(villager, worldName, regionId)) {
                continue;
            }
            String profession = professionName(villager);
            int tierIndex = EconomyConfig.tierIndexForVillagerPosition(position++, config.villagerSoftCapTiers());
            participants.add(new VillagerEconomicParticipant(villager.getUniqueId(), profession, tierIndex));
        }

        return participants;
    }

    private boolean isProductiveVillager(Villager villager, String worldName, String regionId) {
        Location bedLocation = memoryLocation(villager, MemoryKey.HOME);
        Location workLocation = memoryLocation(villager, MemoryKey.JOB_SITE);

        boolean bedInRegion = isInRegion(bedLocation, worldName, regionId, villager.getLocation());
        boolean workInRegion = isInRegion(workLocation, worldName, regionId, villager.getLocation());
        return bedInRegion && workInRegion;
    }

    private static boolean isInRegion(Location location, String worldName, String regionId, Location fallback) {
        Location check = location != null ? location : fallback;
        if (check.getWorld() == null || !worldName.equals(check.getWorld().getName())) {
            return false;
        }

        List<String> foundRegions = WorldGuardBridge.regionsAt(
                worldName, check.getBlockX(), check.getBlockY(), check.getBlockZ());
        String normalised = Kingdom.normaliseId(regionId);
        return foundRegions.stream().anyMatch(found -> Kingdom.normaliseId(found).equals(normalised));
    }

    private static Location memoryLocation(Villager villager, MemoryKey<Location> key) {
        return villager.getMemory(key);
    }

    private static String professionName(Villager villager) {
        String key = villager.getProfession().getKey().getKey();
        int separator = key.indexOf(':');
        return separator >= 0 ? key.substring(separator + 1).toLowerCase() : key.toLowerCase();
    }
}
