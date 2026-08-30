package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.city.GranaryGazette;
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
import dev.mrlemoos.kingdom.granary.BukkitGranaryScan;
import dev.mrlemoos.kingdom.granary.BukkitGranaryYard;
import dev.mrlemoos.kingdom.granary.BukkitTerritoryHeads;
import dev.mrlemoos.kingdom.granary.GranaryBounds;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.granary.GranaryStock;
import dev.mrlemoos.kingdom.granary.HarvestTally;
import dev.mrlemoos.kingdom.granary.RationDay;
import dev.mrlemoos.kingdom.granary.ShortfallWatch;
import dev.mrlemoos.kingdom.granary.HungerDayOutcome;
import dev.mrlemoos.kingdom.granary.HungerDayService;
import dev.mrlemoos.kingdom.granary.HungerSubject;
import dev.mrlemoos.kingdom.granary.PrivationYield;
import dev.mrlemoos.kingdom.granary.WinterLarder;
import dev.mrlemoos.kingdom.parliament.FamineGrievanceService;
import dev.mrlemoos.kingdom.granary.WinterRation;
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
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.levy.LevyDayOutcome;
import dev.mrlemoos.kingdom.war.levy.LevyUpkeepService;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.siege.FieldMoraleDecayService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.HashSet;
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
    private GranaryConfig granaryConfig;
    private HungerDayService hungerDayService;
    private FamineGrievanceService famineGrievanceService;
    private YamlKingdomStore kingdomStore;
    private ShortfallWatch shortfallWatch;
    private dev.mrlemoos.kingdom.church.ChurchService churchService;
    /**
     * Kingdoms already told their granary is full. Memory only, and deliberately so: nothing of the
     * tally is written down but the wheat under a bale, and a restart may say it again.
     */
    private final Set<String> granaryOverflowTold = new HashSet<>();
    /**
     * How the last winter day's ration went for each kingdom. Memory only: the stock is the hay
     * standing in the granary, so tomorrow's sweep reads the truth again whatever happened overnight.
     */
    private final WinterLarder winterLarder = new WinterLarder();

    /** Wires the church's escheat of villagers buried by nobody. */
    public void setChurchService(dev.mrlemoos.kingdom.church.ChurchService churchService) {
        this.churchService = churchService;
    }

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

    /**
     * Gives the task the realm's granaries, so the day's harvest off the fields is laid up in hay
     * with the rest of the day's account, and drawn back out again through the winter. Without them
     * the fields feed nobody and no bale is laid.
     */
    public void setGranary(
            GranaryConfig granaryConfig, YamlKingdomStore kingdomStore, ShortfallWatch shortfallWatch) {
        this.granaryConfig = granaryConfig != null ? granaryConfig : GranaryConfig.defaults();
        this.kingdomStore = kingdomStore;
        this.shortfallWatch = shortfallWatch;
    }

    /**
     * Gives the task the realm's hunger: the ledger behind each villager, and the grievance the
     * Crown answers for while they starve. Without them an unfed winter costs a realm nothing.
     */
    public void setHunger(HungerDayService hungerDayService, FamineGrievanceService famineGrievanceService) {
        this.hungerDayService = hungerDayService;
        this.famineGrievanceService = famineGrievanceService;
    }

    /**
     * How each kingdom's last winter day went: whether it drew its whole ration or stood short.
     * A kingdom that went short is <em>unfed</em>, which is what hunger is reckoned from.
     */
    public WinterLarder winterLarder() {
        return winterLarder;
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
        // The shortfall warning is claimed once for the whole realm, before any kingdom is visited:
        // it falls on the same two days for all of them, and must go out but once on each.
        long realmDay = currentRealmDay();
        ShortfallWatch watch = this.shortfallWatch;
        boolean warnOfShortfall = watch != null && watch.claim(realmDay);
        boolean granaryDirty = warnOfShortfall;

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
            // The granary is settled before the day's yield is reckoned: what the store could not
            // feed today is what hunger cuts that same day's work by.
            granaryDirty |= settleGranary(world, kingdom, regionId, season, epochDay, realmDay, warnOfShortfall);
            HungerDayOutcome hunger = settleHunger(world, kingdom, regionId, realmDay);
            Map<UUID, Double> yieldFactors = PrivationYield.combine(coldYieldFactors, hunger.yieldFactors());
            granaryDirty |= answerForTheFamine(kingdom, hunger, realmDay, epochDay);
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
                    yieldFactors);
            readTheDayToTheRealm(kingdom, participants, economyService, day, treasuryBefore, taxBefore);
            // Villagers whose rites were never held: the whole estate escheats to the Crown.
            if (churchService != null) {
                double lapsed = churchService.escheatLapsedVillagerFunerals(kingdom.getId());
                if (lapsed > 0.0d) {
                    economyService.creditTreasury(kingdom.getId(), lapsed);
                    granaryDirty = true;
                }
            }
            decayFieldMorale(kingdom, season);
            dirty = true;
        }

        if (dirty) {
            economyStore.saveFrom(economyService);
        }
        YamlKingdomStore store = this.kingdomStore;
        if (granaryDirty && store != null) {
            store.saveFrom(kingdomService);
        }
    }

    /**
     * The granary's whole day: the harvest laid up in hay, the winter ration drawn back out of the
     * top of the store, and — twice a year — word of how far short of the winter the realm stands.
     *
     * <p>A kingdom with no granary, or one whose region WorldGuard can no longer place, stores
     * nothing and draws nothing: it is fed exactly as one whose granary stands empty.
     *
     * @return whether anything changed that wants writing to disk
     */
    private boolean settleGranary(
            World world,
            Kingdom kingdom,
            String regionId,
            SeasonProfile season,
            long mcDay,
            long realmDay,
            boolean warnOfShortfall) {
        GranaryConfig config = this.granaryConfig;
        if (config == null) {
            return false;
        }
        String granaryRegion = kingdom.getGranaryRegion();
        Optional<GranaryBounds> bounds = granaryRegion == null || granaryRegion.isBlank()
                ? Optional.empty()
                : BukkitGranaryScan.walkableBounds(world, granaryRegion);

        boolean dirty = false;
        if (bounds.isPresent()) {
            dirty = tallyHarvest(world, kingdom, regionId, season, mcDay, bounds.get(), config);
        }
        int ration = WinterRation.balesFor(
                BukkitTerritoryHeads.countIn(world, regionId), config.headsPerHay());
        drawWinterRation(world, kingdom, bounds, ration, realmDay);
        if (warnOfShortfall) {
            dirty |= warnOfTheWinter(world, kingdom, bounds, ration, realmDay, mcDay);
        }
        return dirty;
    }

    /**
     * The winter day's ration, drawn off the top of the granary so the store sinks the way it rose.
     * A kingdom that could not draw the whole of it went <em>unfed</em>, and that is all the day
     * leaves behind. Players are never fed from the granary; the bales are simply gone.
     *
     * <p>Outside winter nothing is drawn and the record is wiped: no realm is unfed in the growing
     * year.
     */
    private void drawWinterRation(
            World world, Kingdom kingdom, Optional<GranaryBounds> bounds, int ration, long realmDay) {
        if (currentSeason() != Season.WINTER) {
            winterLarder.forget(kingdom.getId());
            return;
        }
        // One ration to the realm day, however often the sweep comes round: the day already settled
        // is not eaten twice.
        Optional<RationDay> settled = winterLarder.lastRation(kingdom.getId());
        if (settled.isPresent() && settled.get().realmDay() == realmDay) {
            return;
        }
        int drawn = ration > 0 && bounds.isPresent()
                ? BukkitGranaryYard.drawBales(world, bounds.get(), ration)
                : 0;
        winterLarder.record(kingdom.getId(), new RationDay(realmDay, ration, drawn));
    }

    /**
     * The shortfall as the realm is warned of it, on the season turn into Harvest and again on the
     * last day of autumn: how many bales it stands short of seeing the winter through at its present
     * head-count, or that it is provisioned, or that it has sited no granary at all. Hung on the
     * Gazette for the town crier and said in chat for those about to hear it.
     *
     * @return whether a post was hung, and so whether the kingdom wants writing to disk
     */
    private boolean warnOfTheWinter(
            World world, Kingdom kingdom, Optional<GranaryBounds> bounds, int ration, long realmDay, long mcDay) {
        int stock = bounds.isPresent() ? BukkitGranaryScan.stockIn(world, bounds.get()).stock() : 0;
        String granaryRegion = kingdom.getGranaryRegion();
        int shortfall = WinterRation.shortfall(stock, ration, WinterRation.winterDaysRemaining(realmDay));
        RealmFeedback.kingdomMessage(
                kingdomService, kingdom.getId(), GranaryGazette.shortfallBody(granaryRegion, shortfall));
        if (!kingdom.getCityState().hasCapital()) {
            return false;
        }
        kingdom.getCityState().addGazettePost(GranaryGazette.shortfallPost(granaryRegion, shortfall, mcDay));
        return true;
    }

    /**
     * The day's harvest tally: the realm's farmers bring in wheat at the season's rate, the loose
     * wheat left on the granary floor is taken up with it, and every bale's worth is laid in the
     * granary's lowest free course. What the granary has no room for is wasted, and the realm hears
     * of it once. Winter's fields give nothing.
     *
     * @return whether the wheat carried over changed and wants writing to disk
     */
    private boolean tallyHarvest(
            World world,
            Kingdom kingdom,
            String regionId,
            SeasonProfile season,
            long mcDay,
            GranaryBounds granary,
            GranaryConfig config) {
        GranaryStock stock = BukkitGranaryScan.stockIn(world, granary);
        int looseWheat = BukkitGranaryYard.gatherLooseWheat(world, granary);
        HarvestTally tally = HarvestTally.reckon(
                countFarmers(world, regionId),
                season.outdoorYieldFactor(),
                currentSeason() != Season.WINTER,
                looseWheat,
                kingdom.getGranaryWheat(),
                stock.free(),
                config);
        BukkitGranaryYard.layBales(world, granary, tally.balesLaid());
        boolean told = tellTheRealmOfAFullGranary(kingdom, tally, mcDay);
        boolean carriedChanged = tally.wheatCarried() != kingdom.getGranaryWheat();
        kingdom.setGranaryWheat(tally.wheatCarried());
        return told || carriedChanged;
    }

    /** Farmer-profession villagers standing in the kingdom's territory; the realm's fields in one figure. */
    private int countFarmers(World world, String regionId) {
        int farmers = 0;
        String worldName = world.getName();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!"farmer".equals(professionName(villager))) {
                continue;
            }
            Location location = villager.getLocation();
            if (isInRegion(location, worldName, regionId, location)) {
                farmers++;
            }
        }
        return farmers;
    }

    /**
     * Word that the granary is full, hung on the Gazette the day it first overflows and not again
     * until there is room and it overflows afresh. A kingdom with no capital has no Gazette.
     *
     * @return whether a post was hung, and so whether the kingdom wants writing to disk
     */
    private boolean tellTheRealmOfAFullGranary(Kingdom kingdom, HarvestTally tally, long mcDay) {
        if (!tally.overflowed()) {
            granaryOverflowTold.remove(kingdom.getId());
            return false;
        }
        if (!granaryOverflowTold.add(kingdom.getId())) {
            return false;
        }
        if (!kingdom.getCityState().hasCapital()) {
            return false;
        }
        kingdom.getCityState().addGazettePost(GranaryGazette.overflowPost(tally.wheatWasted(), mcDay));
        return true;
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

    /**
     * The winter day at the table: a realm that could not draw its whole ration leaves every
     * villager a day hungrier, a realm that could wipes the slate, and from the seventh hungry day
     * the lot takes one of those still starving. Only the hungry-day counts outlive the day.
     */
    private HungerDayOutcome settleHunger(World world, Kingdom kingdom, String regionId, long realmDay) {
        HungerDayService hunger = this.hungerDayService;
        if (hunger == null) {
            return new HungerDayOutcome(Map.of(), Set.of(), Set.of(), Optional.empty());
        }
        GranaryConfig config = this.granaryConfig != null ? this.granaryConfig : GranaryConfig.defaults();
        HungerDayOutcome outcome = hunger.settleDay(
                kingdom.getId(), collectHungerSubjects(world, regionId), winterLarder.isUnfed(kingdom.getId()),
                realmDay, config);
        if (!outcome.striking().isEmpty()) {
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdom.getId(),
                    "The granary is bare: " + outcome.striking().size()
                            + (outcome.striking().size() == 1 ? " villager has" : " villagers have")
                            + " left off work for want of bread.");
        }
        if (outcome.starved().isPresent()) {
            takeByStarvation(world, kingdom, outcome.starved().get());
        }
        if (villagerMpEntityService != null && (!outcome.striking().isEmpty() || outcome.starved().isPresent())) {
            villagerMpEntityService.reconcileAllTerritoryVillagerNametags();
        }
        return outcome;
    }

    /** Every villager standing in the kingdom's territory, and which of them never starve. */
    private List<HungerSubject> collectHungerSubjects(World world, String regionId) {
        List<HungerSubject> subjects = new ArrayList<>();
        String worldName = world.getName();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            Location location = villager.getLocation();
            if (!isInRegion(location, worldName, regionId, location)) {
                continue;
            }
            subjects.add(new HungerSubject(villager.getUniqueId(), neverStrikes(villager)));
        }
        return subjects;
    }

    /** The one the lot took. */
    private void takeByStarvation(World world, Kingdom kingdom, UUID villagerId) {
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!villager.getUniqueId().equals(villagerId)) {
                continue;
            }
            // ponytail: starvation kills outright rather than wearing the villager down over ticks —
            // the ledger already spent seven days getting here, and a death is what the realm sees.
            villager.setHealth(0.0);
            RealmFeedback.kingdomMessage(
                    kingdomService, kingdom.getId(), "A villager has starved to death for want of bread.");
            return;
        }
    }

    /**
     * What the Crown answers for while the realm starves: every subject's loyalty falls a step and
     * the grievance is entered in Hansard, once for the famine however long it runs.
     *
     * @return whether the day the famine was announced wants writing to disk
     */
    private boolean answerForTheFamine(Kingdom kingdom, HungerDayOutcome hunger, long realmDay, long mcDay) {
        FamineGrievanceService grievance = this.famineGrievanceService;
        if (grievance == null) {
            return false;
        }
        return grievance.enterGrievance(kingdom.getId(), hunger.famine(), realmDay, mcDay);
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
        return SeasonProfile.fromPluginConfig(plugin.getConfig(), currentSeason());
    }

    /** The season in force; spring before the calendar is set. */
    private Season currentSeason() {
        RealmCalendarService service = this.calendarService;
        return service == null ? Season.SPRING : service.currentSeason();
    }

    /** The realm day; before the epoch, and so never a warning day, until the calendar is set. */
    private long currentRealmDay() {
        RealmCalendarService service = this.calendarService;
        return service == null ? -1L : service.currentRealmDay();
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

        VillagerMpEntityService villagers = this.villagerMpEntityService;
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!isProductiveVillager(villager, worldName, regionId)) {
                continue;
            }
            // The cleric holds no wallet and takes no part in the villager economy.
            if (villagers != null && villagers.isClericVillager(villager)) {
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
