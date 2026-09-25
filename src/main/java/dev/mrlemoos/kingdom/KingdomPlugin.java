package dev.mrlemoos.kingdom;

import dev.mrlemoos.kingdom.command.CoronaCommand;
import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.city.GazetteService;
import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.city.TownCrierService;
import dev.mrlemoos.kingdom.command.KingdomCityHandler;
import dev.mrlemoos.kingdom.command.KingdomTributeHandler;
import dev.mrlemoos.kingdom.command.KingdomCommand;
import dev.mrlemoos.kingdom.command.KingdomFiscalHandler;
import dev.mrlemoos.kingdom.command.KingdomPoliceHandler;
import dev.mrlemoos.kingdom.command.KingdomWhitelistHandler;
import dev.mrlemoos.kingdom.command.ResignCommand;
import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
import dev.mrlemoos.kingdom.display.PlayerPrefixComposer;
import dev.mrlemoos.kingdom.feedback.DivisionBossBarService;
import dev.mrlemoos.kingdom.feedback.ElectionBossBarService;
import dev.mrlemoos.kingdom.feedback.TrialBossBarService;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.election.ProductiveVillagerScanner;
import dev.mrlemoos.kingdom.election.ProfessionVoteBias;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.election.VillagerPremierInauguralService;
import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyConfig;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantOfferConfig;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantRecipeService;
import dev.mrlemoos.kingdom.economy.villager.merchant.CoronaMerchantStockRotation;
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.listener.BuildConductListener;
import dev.mrlemoos.kingdom.listener.ChatPrefixListener;
import dev.mrlemoos.kingdom.listener.DeathMessageTitleListener;
import dev.mrlemoos.kingdom.listener.TrialJuryGuiListener;
import dev.mrlemoos.kingdom.listener.WantedNametagListener;
import dev.mrlemoos.kingdom.listener.CoronaMerchantListener;
import dev.mrlemoos.kingdom.listener.EconomyActivityListener;
import dev.mrlemoos.kingdom.listener.JoinReminderListener;
import dev.mrlemoos.kingdom.listener.LifeEventListener;
import dev.mrlemoos.kingdom.listener.ParliamentGuiListener;
import dev.mrlemoos.kingdom.listener.RegistrarListener;
import dev.mrlemoos.kingdom.listener.TreasuryBriefingListener;
import dev.mrlemoos.kingdom.listener.TerritoryVillagerDespawnListener;
import dev.mrlemoos.kingdom.listener.TerritoryWealthListener;
import dev.mrlemoos.kingdom.listener.NobleDisplayListener;
import dev.mrlemoos.kingdom.listener.TreasuryLordListener;
import dev.mrlemoos.kingdom.listener.VillagerProfessionNametagListener;
import dev.mrlemoos.kingdom.listener.ConscriptionListener;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyGateService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.police.BukkitJurisdictionPort;
import dev.mrlemoos.kingdom.police.BukkitPrisonSpawnPort;
import dev.mrlemoos.kingdom.police.BuildConductEnforcer;
import dev.mrlemoos.kingdom.police.JurisdictionPort;
import dev.mrlemoos.kingdom.police.PrisonElectedOfficeVacator;
import dev.mrlemoos.kingdom.police.BuildEnforcementConfig;
import dev.mrlemoos.kingdom.police.ActBreachDetector;
import dev.mrlemoos.kingdom.police.MechanicalJusticeConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.police.TrialJuryConfig;
import dev.mrlemoos.kingdom.police.TrialJuryRuntime;
import dev.mrlemoos.kingdom.police.TrialJuryService;
import dev.mrlemoos.kingdom.police.CourtSummonService;
import dev.mrlemoos.kingdom.police.CrownAssaultConfig;
import dev.mrlemoos.kingdom.police.VillagerJuryEntityService;
import dev.mrlemoos.kingdom.cloud.KingdomCloudCommands;
import dev.mrlemoos.kingdom.cloud.KingdomCloudManagerFactory;
import dev.mrlemoos.kingdom.listener.PoliceGolemListener;
import dev.mrlemoos.kingdom.listener.ResignationLetterListener;
import dev.mrlemoos.kingdom.listener.StateOpeningListener;
import dev.mrlemoos.kingdom.parliament.SpeechFromThroneItem;
import dev.mrlemoos.kingdom.parliament.CommonsReturnAnnouncer;
import dev.mrlemoos.kingdom.parliament.StateOpeningCeremony;
import dev.mrlemoos.kingdom.parliament.StateOpeningService;
import dev.mrlemoos.kingdom.whitelist.BukkitServerWhitelistGateway;
import dev.mrlemoos.kingdom.whitelist.WhitelistService;
import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationLetterItem;
import dev.mrlemoos.kingdom.resignation.ResignationService;
import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.command.ParliamentHandler;
import dev.mrlemoos.kingdom.parliament.HansardArchivist;
import dev.mrlemoos.kingdom.parliament.WinterCensureConfig;
import dev.mrlemoos.kingdom.parliament.FamineGrievanceService;
import dev.mrlemoos.kingdom.parliament.WinterCensureService;
import dev.mrlemoos.kingdom.command.LocateCommand;
import dev.mrlemoos.kingdom.command.TpCommand;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.task.ElectionTask;
import dev.mrlemoos.kingdom.task.TerritoryVillagerDespawnTask;
import dev.mrlemoos.kingdom.task.TerritoryWealthReconcileTask;
import dev.mrlemoos.kingdom.granary.FamineWatch;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.granary.GranaryTheftLog;
import dev.mrlemoos.kingdom.granary.HungerDayService;
import dev.mrlemoos.kingdom.granary.InMemoryHungerLedgerStore;
import dev.mrlemoos.kingdom.granary.StarvationLot;
import dev.mrlemoos.kingdom.granary.ShortfallWatch;
import dev.mrlemoos.kingdom.hearth.HearthConfig;
import dev.mrlemoos.kingdom.hearth.HearthDayService;
import dev.mrlemoos.kingdom.hearth.InMemoryColdLedgerStore;
import dev.mrlemoos.kingdom.task.VillagerGdpTask;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.tribute.InMemoryWarDebtStore;
import dev.mrlemoos.kingdom.war.tribute.WarTributeConfig;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.oath.InMemorySwornOutsiderStore;
import dev.mrlemoos.kingdom.war.oath.OathConfig;
import dev.mrlemoos.kingdom.war.oath.OathService;
import dev.mrlemoos.kingdom.war.levy.InMemoryLevyArrearsStore;
import dev.mrlemoos.kingdom.war.levy.LevyUpkeepConfig;
import dev.mrlemoos.kingdom.war.levy.LevyUpkeepService;
import dev.mrlemoos.kingdom.war.siege.FieldMoraleDecayService;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantRegistry;
import dev.mrlemoos.kingdom.war.levy.StandingRosterLevyRoster;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import dev.mrlemoos.kingdom.war.muster.InMemoryMusterStore;
import dev.mrlemoos.kingdom.war.muster.MusterConfig;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionConfig;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.conscription.InMemoryConscriptionStore;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadConfig;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadEntityService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import dev.mrlemoos.kingdom.war.crownsquad.InMemoryCrownSquadStore;
import dev.mrlemoos.kingdom.war.capital.CapitalService;
import dev.mrlemoos.kingdom.war.squad.OfficerEligibility;
import dev.mrlemoos.kingdom.war.squad.SquadConfig;
import dev.mrlemoos.kingdom.war.squad.SquadService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class KingdomPlugin extends JavaPlugin {

        private KingdomService kingdomService;
        private DivisionBossBarService divisionBossBarService;
        private ElectionBossBarService electionBossBarService;
        private TrialBossBarService trialBossBarService;
        private YamlKingdomStore store;
        private dev.mrlemoos.kingdom.calendar.RealmCalendarService realmCalendarService;
        private LoyaltyService loyaltyService;
        private MoraleService moraleService;
        private LoyaltyGateService loyaltyGateService;
        private OathService oathService;
        private MechanicalJusticeService mechanicalJusticeService;
        private PoliceTrialService policeTrialService;
        private NoblePrefixDisplay nobleDisplay;
        private EconomyService economyService;
        private YamlEconomyStore economyStore;
        private EconomyCoordinator economyCoordinator;
        private WarService warService;
        private StandingRosterService standingRosterService;

        @Override
        public void onEnable() {
                saveDefaultConfig();

                kingdomService = new KingdomService();
                store = new YamlKingdomStore(this);
                InMemoryLoyaltyStore loyaltyStore = new InMemoryLoyaltyStore();
                store.setLoyaltyStore(loyaltyStore);
                InMemoryMoraleStore moraleStore = new InMemoryMoraleStore();
                store.setMoraleStore(moraleStore);
                warService = new WarService(kingdomService);
                MilitaryParticipantRegistry militaryParticipantRegistry = new MilitaryParticipantRegistry();
                warService.setConfig(WarConfig.fromPluginConfig(getConfig()));
                store.setWarService(warService);
                CapitalService capitalService = new CapitalService();
                store.setCapitalService(capitalService);
                dev.mrlemoos.kingdom.war.capture.ChunkCaptureService chunkCaptureService =
                                new dev.mrlemoos.kingdom.war.capture.ChunkCaptureService(
                                                dev.mrlemoos.kingdom.war.capture.CaptureConfig.fromPluginConfig(
                                                                getConfig()));
                dev.mrlemoos.kingdom.war.occupation.OccupationBuildGate occupationBuildGate =
                                new dev.mrlemoos.kingdom.war.occupation.OccupationBuildGate(chunkCaptureService);
                InMemoryStandingRosterStore standingRosterStore = new InMemoryStandingRosterStore();
                standingRosterService = new StandingRosterService(
                                kingdomService, standingRosterStore, StandingRosterConfig.fromPluginConfig(getConfig()));
                warService.setStandingRosterService(standingRosterService);
                store.setStandingRosterStore(standingRosterStore);
                InMemoryMusterStore musterStore = new InMemoryMusterStore();
                store.setMusterStore(musterStore);
                InMemoryConscriptionStore conscriptionStore = new InMemoryConscriptionStore();
                store.setConscriptionStore(conscriptionStore);
                InMemorySwornOutsiderStore swornOutsiderStore = new InMemorySwornOutsiderStore();
                store.setSwornOutsiderStore(swornOutsiderStore);
                InMemoryLevyArrearsStore levyArrearsStore = new InMemoryLevyArrearsStore();
                store.setLevyArrearsStore(levyArrearsStore);
                InMemoryColdLedgerStore coldLedgerStore = new InMemoryColdLedgerStore();
                store.setColdLedgerStore(coldLedgerStore);
                HearthConfig hearthConfig = HearthConfig.fromPluginConfig(getConfig());
                GranaryConfig granaryConfig = GranaryConfig.fromPluginConfig(getConfig());
                ShortfallWatch shortfallWatch = new ShortfallWatch();
                store.setShortfallWatch(shortfallWatch);
                InMemoryHungerLedgerStore hungerLedgerStore = new InMemoryHungerLedgerStore();
                store.setHungerLedgerStore(hungerLedgerStore);
                FamineWatch famineWatch = new FamineWatch();
                store.setFamineWatch(famineWatch);
                GranaryTheftLog granaryTheftLog = new GranaryTheftLog();
                store.loadInto(kingdomService);
                java.util.function.LongSupplier mcDayClock = () -> {
                        org.bukkit.World mainWorld = getServer().getWorlds().isEmpty()
                                        ? null
                                        : getServer().getWorlds().get(0);
                        return mainWorld != null ? mainWorld.getFullTime() / 24000L : 0L;
                };
                RealmCalendarService realmCalendarService = new RealmCalendarService(kingdomService, mcDayClock);
                this.realmCalendarService = realmCalendarService;
                store.setCalendarService(realmCalendarService);
                store.loadCalendar();
                realmCalendarService.reconcileAllReigns();
                LoyaltyService loyaltyService = new LoyaltyService(
                                loyaltyStore, LoyaltyConfig.fromPluginConfig(getConfig()));
                this.loyaltyService = loyaltyService;
                MoraleService moraleService = new MoraleService(
                                moraleStore, MoraleConfig.fromPluginConfig(getConfig()));
                this.moraleService = moraleService;
                MusterService musterService = new MusterService(warService, kingdomService, musterStore, System::currentTimeMillis);
                musterService.setConfig(MusterConfig.fromPluginConfig(getConfig()));
                musterService.setStandingRosterService(standingRosterService);
                musterService.setLoyaltyService(loyaltyService);
                musterService.setMoraleServiceCreditHook(moraleService, realmCalendarService::currentRealmDay);
                warService.setMusterService(musterService);
                ConscriptionConfig conscriptionConfig = ConscriptionConfig.fromPluginConfig(getConfig());
                ConscriptionService conscriptionService = new ConscriptionService(
                                kingdomService,
                                conscriptionStore,
                                new ConscriptionConfig(
                                                getConfig().getBoolean("war.enabled", false) && conscriptionConfig.enabled(),
                                                conscriptionConfig.cap()));
                loyaltyGateService = new LoyaltyGateService(loyaltyService);
                oathService = new OathService(
                                kingdomService,
                                loyaltyService,
                                moraleService,
                                swornOutsiderStore,
                                OathConfig.fromPluginConfig(getConfig()));
                PoliceService policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
                MechanicalJusticeService mechanicalJusticeService = new MechanicalJusticeService(
                                kingdomService,
                                policeService,
                                MechanicalJusticeConfig.fromPluginConfig(getConfig()));
                this.mechanicalJusticeService = mechanicalJusticeService;
                mechanicalJusticeService.setRealmDaySupplier(realmCalendarService::currentRealmDay);
                store.setMechanicalJusticeService(mechanicalJusticeService);
                store.loadWarrants();

                economyService = new EconomyService(getConfig().getDouble("economy.starting-treasury", 100.0));
                // Paying income tax while out of favour is an act of service on the political track.
                economyService.setServiceCreditHook(loyaltyService, realmCalendarService::currentRealmDay);
                economyStore = new YamlEconomyStore(this);
                InMemoryWarDebtStore warDebtStore = new InMemoryWarDebtStore();
                economyStore.setWarDebtStore(warDebtStore);
                economyStore.loadInto(economyService);
                WarTributeService warTributeService = new WarTributeService(economyService, warDebtStore);
                WarTributeConfig warTributeConfig = WarTributeConfig.fromPluginConfig(getConfig());
                InMemoryCrownSquadStore crownSquadStore = new InMemoryCrownSquadStore();
                CrownSquadConfig crownSquadConfig = CrownSquadConfig.fromPluginConfig(getConfig());
                CrownSquadService crownSquadService = new CrownSquadService(
                                economyService, crownSquadConfig, crownSquadStore, java.util.UUID::randomUUID, System::currentTimeMillis);
                store.setCrownSquadStore(crownSquadStore);
                store.loadCrownSquads();
                CrownSquadEntityService crownSquadEntityService = new CrownSquadEntityService(this, crownSquadService, economyService);
                java.util.function.Predicate<java.util.UUID> squadOfficerEligibility =
                                OfficerEligibility.standingRosterOrMuster(
                                                standingRosterService,
                                                musterService,
                                                officerId -> {
                                                    java.util.Optional<dev.mrlemoos.kingdom.model.PlayerMembership> membership =
                                                            kingdomService.getMembership(officerId);
                                                    if (membership.isEmpty()) return null;
                                                    java.util.Optional<dev.mrlemoos.kingdom.model.war.ActiveWar> activeWar =
                                                            warService.activeWarFor(membership.get().getKingdomId());
                                                    return activeWar.isPresent() ? activeWar.get().id() : null;
                                                });
                SquadService squadService = new SquadService(
                                SquadConfig.fromPluginConfig(getConfig()),
                                squadOfficerEligibility,
                                officerId -> moraleService.tierOf(officerId).orElse(dev.mrlemoos.kingdom.model.war.MoraleTier.STEADFAST));
                squadService.setConscriptionService(conscriptionService);
                squadService.setCrownSquadService(crownSquadService);
                PoliceTrialService policeTrialService = new PoliceTrialService(
                                kingdomService,
                                policeService,
                                mechanicalJusticeService,
                                economyService);
                this.policeTrialService = policeTrialService;
                store.setPoliceTrialService(policeTrialService);
                store.loadPoliceCases();

                EconomyConfig economyConfig = EconomyConfig.fromPluginConfig(getConfig());
                VillagerEconomyConfig villagerEconomyConfig = VillagerEconomyConfig.fromPluginConfig(getConfig());
                CoronaMerchantOfferConfig coronaMerchantOfferConfig = CoronaMerchantOfferConfig
                                .fromPluginConfig(getConfig());
                RealmWealthRates realmWealthRates = RealmWealthRates
                                .fromPluginConfig(getConfig().getConfigurationSection("economy"));
                KingdomTerritoryResolver territoryResolver = new KingdomTerritoryResolver(kingdomService);
                JurisdictionPort jurisdictionPort = new BukkitJurisdictionPort(territoryResolver);
                PlayerPrefixComposer prefixComposer = new PlayerPrefixComposer(
                                kingdomService,
                                policeService,
                                mechanicalJusticeService,
                                jurisdictionPort);
                nobleDisplay = new NoblePrefixDisplay(prefixComposer);
                economyCoordinator = new EconomyCoordinator(
                                economyService,
                                kingdomService,
                                territoryResolver,
                                economyConfig,
                                villagerEconomyConfig);
                economyCoordinator.setPersistenceHook(() -> economyStore.saveFrom(economyService));

                WorldGuardBridge.warmUp();

                TreasuryLordService treasuryLordService = new TreasuryLordService(this, economyService, economyStore);
                ElectionConfig electionConfig = ElectionConfig.fromPluginConfig(getConfig());
                ProfessionVoteBias professionVoteBias = ProfessionVoteBias.fromPluginConfig(getConfig());
                ElectionService electionService = new ElectionService(kingdomService, electionConfig);
                policeTrialService.setElectedOfficeVacator(
                                new PrisonElectedOfficeVacator(kingdomService, electionService));
                BukkitPrisonSpawnPort prisonSpawnPort = new BukkitPrisonSpawnPort(policeService);
                policeTrialService.setPrisonSpawnPort(prisonSpawnPort);
                mechanicalJusticeService.setSpeakerVillagerResolver(kingdomId -> kingdomService
                                .getKingdom(kingdomId)
                                .flatMap(k -> k.getParliamentState().speakerVillagerEntityId()));
                ResignationService resignationService = new ResignationService(kingdomService, electionService);
                ResignationLetterItem resignationLetterItem = new ResignationLetterItem(this);
                ResignationLetterDelivery resignationLetterDelivery = new ResignationLetterDelivery(kingdomService,
                                resignationService, resignationLetterItem);
                ProductiveVillagerScanner villagerScanner = new ProductiveVillagerScanner(kingdomService);
                VillagerMpEntityService villagerMpEntityService = new VillagerMpEntityService(
                                this, kingdomService, villagerScanner, territoryResolver);
                villagerMpEntityService.setRealmDayClock(realmCalendarService::currentRealmDay);
                villagerMpEntityService.setVillagerStrikeSource(economyService, villagerEconomyConfig);
                villagerMpEntityService.setColdStrikeSource(coldLedgerStore, hearthConfig);
                economyCoordinator.setColdStrikeSource(coldLedgerStore, hearthConfig);
                villagerMpEntityService.setHungerStrikeSource(hungerLedgerStore, granaryConfig);
                economyCoordinator.setHungerStrikeSource(hungerLedgerStore, granaryConfig);
                ParliamentService parliamentService = new ParliamentService(kingdomService);
                dev.mrlemoos.kingdom.treaty.TreatyService treatyService =
                                new dev.mrlemoos.kingdom.treaty.TreatyService(kingdomService, mcDayClock);
                store.setTreatyService(treatyService);
                warService.setTreatyService(treatyService);
                store.loadTreaties();
                economyCoordinator.setTreatyService(treatyService);
                parliamentService.setProfessionVoteBias(professionVoteBias);
                parliamentService.setDivisionWindowMcDays(getConfig().getInt(
                                "parliament.villager-speaker.division-window-days",
                                ParliamentService.DEFAULT_DIVISION_WINDOW_MC_DAYS));
                parliamentService.setPremierQuestionsIntervalMcDays(getConfig().getInt(
                                "parliament.villager-speaker.premier-questions-interval-days",
                                ParliamentService.DEFAULT_PREMIER_QUESTIONS_INTERVAL_MC_DAYS));
                parliamentService.setConfidenceCooldownMcDays(getConfig().getInt(
                                "parliament.motion.confidence-cooldown-days",
                                ParliamentService.DEFAULT_CONFIDENCE_COOLDOWN_MC_DAYS));
                parliamentService.setPollingWindowMcDays(getConfig().getInt(
                                "parliament.referendum.polling-window-days",
                                ParliamentService.DEFAULT_POLLING_WINDOW_MC_DAYS));
                parliamentService.setElectionService(electionService);
                parliamentService.setVillagerSeatReleaser(villagerMpEntityService::releaseSeat);
                parliamentService.setWarService(warService);
                parliamentService.setTreatyService(treatyService);
                parliamentService.setTerritoryResolver(territoryResolver);
                parliamentService.setMcDayClock(mcDayClock);
                HansardArchivist hansardArchivist = new HansardArchivist(kingdomService);
                hansardArchivist.setCalendarService(realmCalendarService);
                electionService.setHansardArchivist(hansardArchivist::archive);
                // A Premier who wars or dissolves in winter answers for it in political standing —
                // never by a motion, which only the House may table.
                WinterCensureService winterCensureService = new WinterCensureService(
                                kingdomService,
                                loyaltyService,
                                WinterCensureConfig.fromPluginConfig(getConfig()),
                                realmCalendarService::currentSeason,
                                realmCalendarService::currentRealmDay);
                winterCensureService.setAnnouncer((kingdomId, message) -> kingdomService.getKingdom(kingdomId)
                                .ifPresent(kingdom -> org.bukkit.Bukkit.broadcastMessage(
                                                dev.mrlemoos.kingdom.helpers.ColourEncoder.c(
                                                                "&e" + kingdom.getDisplayName() + ": " + message))));
                electionService.setWinterCensureService(winterCensureService);
                warService.setWinterCensureService(winterCensureService);
                // A realm left to starve answers for it in the same coin, and by no motion either.
                FamineGrievanceService famineGrievanceService =
                                new FamineGrievanceService(kingdomService, loyaltyService, granaryConfig, famineWatch);
                famineGrievanceService.setAnnouncer((kingdomId, message) -> kingdomService.getKingdom(kingdomId)
                                .ifPresent(kingdom -> org.bukkit.Bukkit.broadcastMessage(
                                                dev.mrlemoos.kingdom.helpers.ColourEncoder.c(
                                                                "&e" + kingdom.getDisplayName() + ": " + message))));
                VillagerPremierInauguralService villagerPremierInauguralService = new VillagerPremierInauguralService(
                                kingdomService, economyService, electionService, parliamentService, professionVoteBias,
                                electionConfig);
                ElectionHandler electionHandler = new ElectionHandler(
                                electionService,
                                kingdomService,
                                store,
                                villagerScanner,
                                villagerMpEntityService,
                                nobleDisplay,
                                villagerPremierInauguralService);
                StateOpeningService stateOpeningService = new StateOpeningService(kingdomService, parliamentService);
                SpeechFromThroneItem speechFromThroneItem = new SpeechFromThroneItem(this);
                CommonsReturnAnnouncer commonsReturnAnnouncer = new CommonsReturnAnnouncer(this, kingdomService);
                commonsReturnAnnouncer.setLineDelayTicks(getConfig().getLong(
                                "parliament.state-opening.line-delay-ticks",
                                CommonsReturnAnnouncer.DEFAULT_LINE_DELAY_TICKS));
                StateOpeningCeremony stateOpeningCeremony = new StateOpeningCeremony(
                                this, kingdomService, stateOpeningService, store, speechFromThroneItem,
                                commonsReturnAnnouncer, villagerMpEntityService);
                stateOpeningCeremony.setPoliceTrialService(policeTrialService);
                stateOpeningCeremony.setRealmDayClock(realmCalendarService::currentRealmDay);
                electionHandler.setStateOpeningCeremony(stateOpeningCeremony);
                electionHandler.setCommonsReturnAnnouncer(commonsReturnAnnouncer);
                KingdomFiscalHandler fiscalHandler = new KingdomFiscalHandler(
                                economyService, kingdomService, economyStore, territoryResolver, treasuryLordService,
                                this);
                PoliceCourtService policeCourtService = new PoliceCourtService(this, kingdomService, policeService);
                PoliceGolemService policeGolemService = new PoliceGolemService(this, kingdomService, policeService);
                KingdomPoliceHandler policeHandler = new KingdomPoliceHandler(
                                policeService,
                                policeCourtService,
                                policeGolemService,
                                kingdomService,
                                store,
                                territoryResolver,
                                nobleDisplay,
                                policeTrialService.arrestRewardService(),
                                economyService,
                                economyStore);
                dev.mrlemoos.kingdom.appeal.AppealService appealService =
                                new dev.mrlemoos.kingdom.appeal.AppealService(kingdomService, policeTrialService);
                dev.mrlemoos.kingdom.appeal.AppealPetitionItem appealPetitionItem =
                                new dev.mrlemoos.kingdom.appeal.AppealPetitionItem(this);
                policeHandler.setAppealService(appealService);
                dev.mrlemoos.kingdom.listener.AppealPetitionListener appealPetitionListener =
                                new dev.mrlemoos.kingdom.listener.AppealPetitionListener(
                                                kingdomService, appealService, appealPetitionItem);
                policeHandler.setAppealDelivery(appealPetitionListener::deliverToCrown);
                TrialJuryConfig trialJuryConfig = TrialJuryConfig.fromPluginConfig(getConfig());
                TrialJuryService trialJuryService = new TrialJuryService(
                                kingdomService,
                                policeService,
                                policeTrialService,
                                mechanicalJusticeService,
                                trialJuryConfig,
                                new java.util.Random());
                CourtSummonService courtSummonService =
                                new CourtSummonService(policeService, policeCourtService);
                VillagerJuryEntityService villagerJuryEntityService = new VillagerJuryEntityService(
                                this,
                                kingdomService,
                                policeService,
                                policeCourtService,
                                villagerMpEntityService);
                trialJuryService.setVillagerJurorProvider(villagerJuryEntityService::claimJurors);
                TrialJuryRuntime trialJuryRuntime = new TrialJuryRuntime(
                                trialJuryService,
                                policeTrialService,
                                kingdomService,
                                policeService,
                                trialJuryConfig);
                trialJuryRuntime.setCourtSummonService(courtSummonService);
                trialJuryRuntime.setVillagerJuryEntityService(villagerJuryEntityService);
                policeHandler.setTrialJuryRuntime(policeTrialService, trialJuryRuntime);
                policeTrialService.setTrialJuryService(trialJuryService);
                // Give players time to reconnect so a Judge or jury can hear restored trials.
                getServer().getScheduler().runTaskLater(
                                this,
                                () -> policeTrialService.openCasesView().forEach(policeCase -> trialJuryRuntime
                                                .resolveAfterArrest(policeCase.kingdomId(), policeCase.accusedId())),
                                20L * Math.max(1, getConfig().getInt("police.case-resume-grace-seconds", 120)));
                TrialBossBarService trialBossBarService = new TrialBossBarService(
                                this, kingdomService, trialJuryService::listSessions);
                trialBossBarService.start();
                this.trialBossBarService = trialBossBarService;
                ElectionBossBarService electionBossBarService = new ElectionBossBarService(
                                this, kingdomService, electionConfig);
                electionBossBarService.start();
                this.electionBossBarService = electionBossBarService;
                policeGolemService.setPatrolDetainDeps(
                                mechanicalJusticeService,
                                jurisdictionPort,
                                trialJuryRuntime,
                                policeTrialService,
                                () -> {
                                    store.saveFrom(kingdomService);
                                    economyStore.saveFrom(economyService);
                                });
                WhitelistService whitelistService = new WhitelistService(new BukkitServerWhitelistGateway());
                KingdomWhitelistHandler whitelistHandler = new KingdomWhitelistHandler(
                                whitelistService,
                                kingdomService);
                CityService cityService = new CityService(
                                kingdomService, policeTrialService::isUnderPrisonSentence);
                policeTrialService.setBuildPermitRevoker(cityService::revokeAllPermits);
                LordMayorService lordMayorService = new LordMayorService(this, kingdomService);
                TownCrierService townCrierService = new TownCrierService(this, kingdomService);
                GazetteService gazetteService = new GazetteService(kingdomService);
                dev.mrlemoos.kingdom.church.ChurchConfig churchConfig =
                                new dev.mrlemoos.kingdom.church.ChurchConfig(
                                                getConfig().getInt("church.blessing-seconds", 120),
                                                getConfig().getInt("church.mass-interval-days", 7),
                                                getConfig().getInt("church.funeral-window-days", 3),
                                                getConfig().getDouble("church.funeral-experience-share", 0.5d),
                                                getConfig().getDouble("church.tithe-share", 0.1d));
                dev.mrlemoos.kingdom.church.ChurchService churchService =
                                new dev.mrlemoos.kingdom.church.ChurchService(
                                                kingdomService,
                                                policeService,
                                                policeTrialService::isUnderPrisonSentence,
                                                realmCalendarService::currentRealmDay,
                                                churchConfig);
                dev.mrlemoos.kingdom.church.ClericService clericService =
                                new dev.mrlemoos.kingdom.church.ClericService(this, kingdomService, churchService);
                KingdomCityHandler cityHandler = new KingdomCityHandler(
                                kingdomService,
                                cityService,
                                lordMayorService,
                                townCrierService,
                                territoryResolver,
                                store,
                                capitalService);
                dev.mrlemoos.kingdom.command.KingdomChurchHandler churchHandler =
                                new dev.mrlemoos.kingdom.command.KingdomChurchHandler(
                                                kingdomService,
                                                churchService,
                                                clericService,
                                                territoryResolver,
                                                economyService,
                                                store);
                DemobilisationService demobilisationService = new DemobilisationService(warService);
                demobilisationService.setMusterService(musterService);
                demobilisationService.setStandingRosterService(standingRosterService);
                demobilisationService.setMilitaryParticipantRegistry(militaryParticipantRegistry);
                demobilisationService.setConscriptionService(conscriptionService);
                demobilisationService.setCrownSquadService(crownSquadService);
                crownSquadService.setDemobilisationObserver(crownSquadEntityService::remove);
                ParliamentHandler parliamentHandler = new ParliamentHandler(
                                parliamentService,
                                kingdomService,
                                economyService,
                                store,
                                economyStore,
                                territoryResolver,
                                treasuryLordService,
                                this,
                                villagerPremierInauguralService,
                                warService,
                                demobilisationService);
                ResignCommand resignCommand = new ResignCommand(
                                this,
                                kingdomService,
                                resignationService,
                                villagerMpEntityService,
                                nobleDisplay,
                                store,
                                resignationLetterDelivery);
                ParliamentGuiListener parliamentGuiListener = new ParliamentGuiListener(parliamentHandler,
                                resignCommand);
                parliamentHandler.setHubGuiOpener(parliamentGuiListener::openHubGui);
                parliamentHandler.setReferendumBallotOpener(parliamentGuiListener::openReferendumBallotGui);
                fiscalHandler.setMintPrepareGuiOpener(parliamentGuiListener::openMintPrepareGui);
                dev.mrlemoos.kingdom.parliament.RoyalStandardPlacer royalStandardPlacer =
                                new dev.mrlemoos.kingdom.parliament.RoyalStandardPlacer(kingdomService);
                parliamentHandler.setRoyalStandardPlacer(royalStandardPlacer);
                parliamentHandler.setTreatyService(treatyService);

                KingdomCommand kingdomCommand = new KingdomCommand(
                                kingdomService, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler,
                                electionHandler, realmWealthRates, policeHandler, whitelistHandler,
                                warService, loyaltyService);
                dev.mrlemoos.kingdom.parliament.CoronationCeremony coronationCeremony =
                                new dev.mrlemoos.kingdom.parliament.CoronationCeremony(this, kingdomService);
                coronationCeremony.setPoliceTrialService(policeTrialService);
                coronationCeremony.setCalendarService(realmCalendarService);
                kingdomCommand.setMoraleService(moraleService);
                kingdomCommand.setCityHandler(cityHandler, cityService);
                kingdomCommand.setChurchHandler(churchHandler, churchService);
                policeHandler.setChurchService(churchService);
                parliamentGuiListener.setChurchService(churchService);
                kingdomCommand.setCoronationCeremony(coronationCeremony);
                kingdomCommand.setCalendarService(
                                realmCalendarService,
                                dev.mrlemoos.kingdom.calendar.PollingDay.fromPluginConfig(getConfig()));
                kingdomCommand.setGranaryConfig(granaryConfig);
                kingdomCommand.setTributeHandler(
                                new KingdomTributeHandler(kingdomService, economyService, warTributeService, economyStore));
                CoronaCommand coronaCommand = new CoronaCommand(economyService, kingdomService, economyStore,
                                economyCoordinator);
                TeleportService teleportService = new TeleportService(kingdomService);
                TpCommand tpCommand = new TpCommand(
                                teleportService, kingdomService, store, territoryResolver, policeTrialService);
                tpCommand.setChurchService(churchService);
                LocateCommand locateCommand = new LocateCommand(this, kingdomService, teleportService);

                LegacyPaperCommandManager<CommandSender> commandManager = KingdomCloudManagerFactory.create(this);
                KingdomCloudCommands.register(
                                commandManager,
                                kingdomCommand,
                                coronaCommand,
                                tpCommand,
                                locateCommand,
                                resignCommand,
                                kingdomService,
                                teleportService);

                getServer().getPluginManager().registerEvents(new ChatPrefixListener(prefixComposer),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.ChurchRitesListener(
                                                kingdomService, churchService, economyService, territoryResolver),
                                this);
                getServer().getPluginManager().registerEvents(
                                new DeathMessageTitleListener(prefixComposer), this);
                getServer().getPluginManager().registerEvents(new NobleDisplayListener(nobleDisplay), this);
                getServer().getPluginManager().registerEvents(
                                new WantedNametagListener(nobleDisplay, jurisdictionPort), this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.PrisonConfinementListener(
                                                policeTrialService,
                                                java.util.Objects.requireNonNullElse(
                                                                org.bukkit.Material.matchMaterial(getConfig().getString(
                                                                                "police.prison-labour.material", "STONE")),
                                                                org.bukkit.Material.STONE),
                                                Math.max(1, getConfig().getInt("police.prison-labour.seconds-per-block", 5)),
                                                Math.clamp(getConfig().getDouble(
                                                                "police.prison-labour.max-reduction-share", 0.5), 0.0, 1.0)),
                                this);
                getServer().getPluginManager().registerEvents(
                                new TrialJuryGuiListener(trialJuryRuntime), this);
                getServer().getPluginManager().registerEvents(
                                new JoinReminderListener(
                                                kingdomService,
                                                getConfig().getBoolean("join-reminder", true),
                                                getConfig().getStringList("join-message")),
                                this);
                getServer().getPluginManager().registerEvents(
                                new EconomyActivityListener(economyCoordinator, villagerMpEntityService), this);
                getServer().getPluginManager().registerEvents(
                                new CoronaMerchantListener(
                                                economyCoordinator,
                                                villagerMpEntityService,
                                                territoryResolver,
                                                new CoronaMerchantRecipeService(
                                                                coronaMerchantOfferConfig,
                                                                CoronaMerchantStockRotation.fromPluginConfig(getConfig()))),
                                this);
                getServer().getPluginManager().registerEvents(
                                new TerritoryWealthListener(this, economyService, territoryResolver, economyStore),
                                this);
                BuildConductEnforcer buildConductEnforcer = new BuildConductEnforcer(
                                new ActBreachDetector(),
                                BuildEnforcementConfig.fromPluginConfig(getConfig()),
                                System::currentTimeMillis);
                getServer().getPluginManager().registerEvents(
                                new BuildConductListener(
                                                kingdomService,
                                                territoryResolver,
                                                buildConductEnforcer,
                                                mechanicalJusticeService,
                                                loyaltyService,
                                                cityService,
                                                warService,
                                                occupationBuildGate),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.LordMayorGuiListener(
                                                lordMayorService, cityService, kingdomService, store,
                                                economyService, realmWealthRates, nobleDisplay,
                                                standingRosterService, getConfig().getBoolean("war.enabled", false)),
                                this);
                dev.mrlemoos.kingdom.listener.HonoursGuiListener honoursGuiListener =
                                new dev.mrlemoos.kingdom.listener.HonoursGuiListener(
                                                kingdomService, store, nobleDisplay);
                honoursGuiListener.setChurchService(churchService);
                getServer().getPluginManager().registerEvents(honoursGuiListener, this);
                dev.mrlemoos.kingdom.calendar.PollingDay hubPollingDay =
                                dev.mrlemoos.kingdom.calendar.PollingDay.fromPluginConfig(getConfig());
                dev.mrlemoos.kingdom.listener.TownCrierGuiListener townCrierGuiListener =
                                new dev.mrlemoos.kingdom.listener.TownCrierGuiListener(
                                                townCrierService,
                                                gazetteService,
                                                kingdomService,
                                                store,
                                                economyService,
                                                mechanicalJusticeService,
                                                realmCalendarService,
                                                hubPollingDay,
                                                treatyService);
                getServer().getPluginManager().registerEvents(townCrierGuiListener, this);

                dev.mrlemoos.kingdom.listener.MusterGuiListener musterGuiListener =
                                new dev.mrlemoos.kingdom.listener.MusterGuiListener(
                                                warService, musterService, store, kingdomService);
                getServer().getPluginManager().registerEvents(musterGuiListener, this);
                ConscriptionListener conscriptionListener = new ConscriptionListener(
                                this, kingdomService, territoryResolver, conscriptionService,
                                villagerMpEntityService,
                                () -> store.saveFrom(kingdomService));
                getServer().getPluginManager().registerEvents(conscriptionListener, this);
                getServer().getScheduler().runTaskTimer(this, conscriptionListener::reconcileLoadedVillagers, 20L, 1200L);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.SquadControlListener(
                                                kingdomService,
                                                warService,
                                                conscriptionService,
                                                crownSquadService,
                                                squadService,
                                                squadOfficerEligibility),
                                this);
                dev.mrlemoos.kingdom.war.siege.SiegeZoneResolver siegeZones =
                                new dev.mrlemoos.kingdom.war.siege.SiegeZoneResolver(
                                                dev.mrlemoos.kingdom.war.siege.SiegeConfig.fromPluginConfig(getConfig()));
                dev.mrlemoos.kingdom.war.siege.TerritoryPort siegeTerritory =
                                (kingdomId, chunk) -> territoryResolver
                                                .owningKingdomId(
                                                                chunk.worldName(),
                                                                chunk.chunkX() * 16 + 8,
                                                                64,
                                                                chunk.chunkZ() * 16 + 8)
                                                .filter(foundKingdomId -> kingdomId.equals(foundKingdomId))
                                                .isPresent();
                demobilisationService.setChunkCaptureService(chunkCaptureService);
                dev.mrlemoos.kingdom.war.capital.WarAimEvaluator warAimEvaluator =
                                new dev.mrlemoos.kingdom.war.capital.WarAimEvaluator(
                                                dev.mrlemoos.kingdom.war.capital.WarAimConfig.fromPluginConfig(
                                                                getConfig()));
                dev.mrlemoos.kingdom.war.victory.VictoryEvaluator victoryEvaluator =
                                new dev.mrlemoos.kingdom.war.victory.VictoryEvaluator(warAimEvaluator);
                victoryEvaluator.setDemobilisationService(demobilisationService);
                dev.mrlemoos.kingdom.war.victory.DefaultVictoryOutcomeDispatcher outcomeDispatcher =
                                new dev.mrlemoos.kingdom.war.victory.DefaultVictoryOutcomeDispatcher();
                java.io.File annexBackups = new java.io.File(getDataFolder(), "annexation-backups");
                java.io.File annexAudit = new java.io.File(getDataFolder(), "annexation-audit.log");
                outcomeDispatcher.setRegionMergeExecutor(
                                new dev.mrlemoos.kingdom.war.annexation.WorldGuardAnnexationExecutor(
                                                new dev.mrlemoos.kingdom.war.annexation.DomainRegionMergeExecutor(
                                                                dev.mrlemoos.kingdom.war.annexation.AnnexationConfig
                                                                                .fromPluginConfig(getConfig())),
                                                kingdomService,
                                                new dev.mrlemoos.kingdom.war.annexation.AnnexationTerritoryLink(),
                                                dev.mrlemoos.kingdom.worldguard.WorldGuardBridge::createCuboidRegion,
                                                (key, body) -> {
                                                    try {
                                                        annexBackups.mkdirs();
                                                        java.nio.file.Files.writeString(
                                                                        new java.io.File(annexBackups, key + ".txt")
                                                                                        .toPath(),
                                                                        body);
                                                    } catch (Exception ex) {
                                                        getLogger().warning("Annexation backup failed: "
                                                                        + ex.getMessage());
                                                    }
                                                },
                                                line -> {
                                                    getLogger().info(line);
                                                    try {
                                                        java.nio.file.Files.writeString(
                                                                        annexAudit.toPath(),
                                                                        line + System.lineSeparator(),
                                                                        java.nio.file.StandardOpenOption.CREATE,
                                                                        java.nio.file.StandardOpenOption.APPEND);
                                                    } catch (Exception ex) {
                                                        getLogger().warning("Annexation audit failed: "
                                                                        + ex.getMessage());
                                                    }
                                                },
                                                () -> store.saveFrom(kingdomService)));
                outcomeDispatcher.setWarTributeService(warTributeService);
                outcomeDispatcher.setWarTributeConfig(warTributeConfig);
                victoryEvaluator.setOutcomeDispatcher(outcomeDispatcher);
                dev.mrlemoos.kingdom.war.victory.VictoryTick victoryTick =
                                new dev.mrlemoos.kingdom.war.victory.VictoryTick(
                                                victoryEvaluator,
                                                chunkCaptureService,
                                                new dev.mrlemoos.kingdom.war.capital.WorldGuardLinkedTerritorySize(
                                                                kingdomService),
                                                dev.mrlemoos.kingdom.war.capital.CapitalFallMode.MAJORITY,
                                                new dev.mrlemoos.kingdom.war.capital.WorldGuardCapitalTerritory(
                                                                capitalService, kingdomService));
                dev.mrlemoos.kingdom.war.capture.ChunkCapturePresenceTick chunkCaptureTick =
                                new dev.mrlemoos.kingdom.war.capture.ChunkCapturePresenceTick(
                                                siegeZones,
                                                siegeTerritory,
                                                militaryParticipantRegistry,
                                                chunkCaptureService);
                dev.mrlemoos.kingdom.war.siege.SiegePresenceService siegePresenceService =
                                new dev.mrlemoos.kingdom.war.siege.SiegePresenceService(
                                                siegeZones,
                                                siegeTerritory,
                                                militaryParticipantRegistry,
                                                moraleService);
                dev.mrlemoos.kingdom.listener.SiegePresenceListener siegePresenceListener =
                                new dev.mrlemoos.kingdom.listener.SiegePresenceListener(
                                                kingdomService,
                                                warService,
                                                standingRosterService,
                                                musterService,
                                                militaryParticipantRegistry,
                                                siegePresenceService,
                                                chunkCaptureTick);
                siegePresenceListener.setVictoryTick(victoryTick);
                siegePresenceListener.setOnDecisiveVictory(victory -> {
                    store.saveFrom(kingdomService);
                    economyStore.saveFrom(economyService);
                });
                getServer().getPluginManager().registerEvents(siegePresenceListener, this);
                dev.mrlemoos.kingdom.hub.RealmHubSnapshotFactory realmHubSnapshots =
                                new dev.mrlemoos.kingdom.hub.RealmHubSnapshotFactory(
                                                kingdomService,
                                                new dev.mrlemoos.kingdom.city.gui.GazetteLiveStateReader(
                                                                economyService,
                                                                mechanicalJusticeService,
                                                                realmCalendarService,
                                                                hubPollingDay)
                                                .withTreatyService(treatyService)
                                                .withKingdomService(kingdomService))
                                                .withCityService(cityService)
                                                .withEconomyService(economyService)
                                                .withJusticeService(mechanicalJusticeService)
                                                .withParliamentService(parliamentService)
                                                .withStandingRosterService(
                                                        standingRosterService,
                                                        getConfig().getBoolean("war.enabled", false))
                                                .withOathService(oathService, moraleService)
                                                .withMusterService(musterService, warService)
                                                .withConscriptionService(conscriptionService)
                                                .withSiegePresenceService(siegePresenceService)
                                                .withChunkCaptureService(chunkCaptureService)
                                                .withCapitalService(capitalService);
                dev.mrlemoos.kingdom.listener.RealmHubListener realmHubListener =
                                new dev.mrlemoos.kingdom.listener.RealmHubListener(kingdomService, realmHubSnapshots)
                                                .withLoyaltyOpener(kingdomCommand::openLoyaltyLedger)
                                                .withMusterOpener(player -> musterGuiListener.open(player))
                                                .withParliamentOpener(parliamentGuiListener::openHubGui)
                                                .withReferendumOpener(parliamentGuiListener::openReferendumBallotGui)
                                                .withGazetteOpener(player -> kingdomService
                                                                .getMembership(player.getUniqueId())
                                                                .flatMap(membership -> kingdomService
                                                                                .getKingdom(membership.getKingdomId()))
                                                                .ifPresent(kingdom -> townCrierGuiListener.openGazette(
                                                                                player, kingdom, 0)));
                getServer().getPluginManager().registerEvents(realmHubListener, this);
                kingdomCommand.setRealmHubOpener(realmHubListener::openHub);
                // The morale pardon is heard at the court: right-click the judge's bench.
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.MoralePardonListener(
                                                kingdomService, policeCourtService, moraleService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.ClericGuiListener(
                                                kingdomService, churchService, clericService, store, oathService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.HorsePermitListener(
                                                this, kingdomService, cityService, store),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.CurfewEnforcementListener(
                                                this,
                                                kingdomService,
                                                territoryResolver,
                                                mechanicalJusticeService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.CrownAssaultListener(
                                                kingdomService,
                                                territoryResolver,
                                                mechanicalJusticeService,
                                                policeService,
                                                policeTrialService,
                                                policeGolemService,
                                                trialJuryRuntime,
                                                CrownAssaultConfig.fromPluginConfig(getConfig()),
                                                () -> store.saveFrom(kingdomService)),
                                this);
                getServer().getPluginManager().registerEvents(new LifeEventListener(economyCoordinator, this), this);
                getServer().getPluginManager().registerEvents(
                                new TreasuryBriefingListener(kingdomService, economyService, territoryResolver, this),
                                this);
                getServer().getPluginManager().registerEvents(
                                new TreasuryLordListener(treasuryLordService, economyService, kingdomService,
                                                economyStore, store, crownSquadService, crownSquadEntityService, warService),
                                this);
                getServer().getPluginManager().registerEvents(parliamentGuiListener, this);
                getServer().getPluginManager().registerEvents(new RegistrarListener(kingdomService), this);
                getServer().getPluginManager().registerEvents(
                                new ResignationLetterListener(
                                                kingdomService, resignationService, resignationLetterItem,
                                                resignationLetterDelivery),
                                this);
                getServer().getPluginManager().registerEvents(
                                appealPetitionListener,
                                this);
                getServer().getPluginManager().registerEvents(
                                new StateOpeningListener(speechFromThroneItem, stateOpeningCeremony), this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.CoronationListener(coronationCeremony), this);
                getServer().getPluginManager().registerEvents(
                                new VillagerProfessionNametagListener(villagerMpEntityService), this);
                getServer().getPluginManager().registerEvents(
                                new TerritoryVillagerDespawnListener(this, villagerMpEntityService), this);
                getServer().getPluginManager().registerEvents(
                                new PoliceGolemListener(policeService, policeGolemService, kingdomService, store),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.LoyaltyLedgerGuiListener(), this);
                // A bale broken by any hand but the Crown's is grain theft; a bale right-clicked
                // reads the realm's stores.
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.GranaryListener(
                                                kingdomService,
                                                territoryResolver,
                                                mechanicalJusticeService,
                                                granaryConfig,
                                                granaryTheftLog,
                                                hungerLedgerStore,
                                                realmCalendarService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.SeasonalCropGrowthListener(
                                                this, realmCalendarService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new dev.mrlemoos.kingdom.listener.SeasonalHostileSpawnListener(
                                                this, realmCalendarService),
                                this);
                dev.mrlemoos.kingdom.listener.SeasonalSnowListener seasonalSnowListener =
                                new dev.mrlemoos.kingdom.listener.SeasonalSnowListener(
                                                this, realmCalendarService, kingdomService);
                getServer().getPluginManager().registerEvents(seasonalSnowListener, this);
                getServer().getScheduler().runTaskTimer(this, seasonalSnowListener, 20L, 1200L);

                dev.mrlemoos.kingdom.task.RealmCalendarTask realmCalendarTask =
                                new dev.mrlemoos.kingdom.task.RealmCalendarTask(
                                                kingdomService, realmCalendarService, store);
                realmCalendarTask.setRecoveryServices(loyaltyService, moraleService);
                realmCalendarTask.setTreatyService(treatyService);
                realmCalendarTask.setSeasonConfig(getConfig());
                getServer().getScheduler().runTaskTimer(this, realmCalendarTask, 100L, 20L * 20);
                getServer().getScheduler().runTaskTimer(this, policeGolemService::tickFollowers, 40L, 20L);
                getServer().getScheduler().runTaskTimer(this, siegePresenceListener, 20L, 20L);
                getServer().getScheduler().runTaskTimer(
                                this,
                                new dev.mrlemoos.kingdom.task.SquadAiTask(
                                                squadService, kingdomService, warService, crownSquadEntityService),
                                20L,
                                20L);
                getServer().getScheduler().runTaskTimer(this, policeGolemService::tickPatrolDetains, 60L, 20L);
                getServer().getScheduler().runTaskTimer(
                                this,
                                () -> policeTrialService.releaseDueSentences(System.currentTimeMillis()),
                                20L,
                                20L * 30);
                int warrantLimitationDays = Math.max(1, getConfig().getInt("police.warrant-limitation-days", 7));
                getServer().getScheduler().runTaskTimer(
                                this,
                                () -> {
                                        if (!policeTrialService.arrestRewardService()
                                                        .lapseDueWarrants(
                                                                        realmCalendarService.currentRealmDay(),
                                                                        warrantLimitationDays)
                                                        .isEmpty()) {
                                                nobleDisplay.refreshAllOnline();
                                                store.saveFrom(kingdomService);
                                                economyStore.saveFrom(economyService);
                                        }
                                },
                                20L * 60,
                                20L * 60);
                getServer().getScheduler().runTaskTimer(
                                this,
                                () -> trialJuryRuntime.sweepTimeouts(System.currentTimeMillis()),
                                20L,
                                20L * 5);

                getServer().getScheduler().runTaskLater(this, fiscalHandler::respawnTreasuryLords, 20L);
                getServer().getScheduler().runTaskLater(this, policeHandler::pruneStaleEntities, 20L);
                getServer().getScheduler().runTaskLater(this, policeHandler::respawnAllJudges, 20L);

                long gdpInterval = getConfig().getLong("economy.villager-gdp.tick-interval-ticks",
                                VillagerGdpTask.DEFAULT_INTERVAL_TICKS);
                VillagerGdpTask gdpTask = new VillagerGdpTask(
                                this, economyCoordinator, kingdomService, economyStore, villagerEconomyConfig);
                gdpTask.setCalendarService(realmCalendarService);
                gdpTask.setPlayerTaxServiceCreditHook(loyaltyService, realmCalendarService::currentRealmDay);
                gdpTask.setChurchService(churchService);
                gdpTask.setConscriptionService(conscriptionService);
                LevyUpkeepService levyUpkeepService = new LevyUpkeepService(
                                levyArrearsStore,
                                LevyUpkeepConfig.fromPluginConfig(getConfig()),
                                moraleService,
                                economyService::debitTreasury,
                                new StandingRosterLevyRoster(standingRosterService));
                gdpTask.setLevyUpkeep(levyUpkeepService, musterService);
                gdpTask.setHearths(new HearthDayService(coldLedgerStore), hearthConfig, villagerMpEntityService);
                gdpTask.setGranary(granaryConfig, store, shortfallWatch);
                gdpTask.setHunger(
                                new HungerDayService(hungerLedgerStore, StarvationLot.random(new java.util.Random())),
                                famineGrievanceService);
                gdpTask.setFieldMoraleDecay(
                                new FieldMoraleDecayService(moraleService, militaryParticipantRegistry), warService);
                gdpTask.schedule(gdpInterval);

                TerritoryWealthReconcileTask wealthReconcileTask = new TerritoryWealthReconcileTask(this,
                                economyService, kingdomService, economyStore);
                wealthReconcileTask.schedule(gdpInterval);

                ElectionTask electionTask = new ElectionTask(
                                this, electionService, electionHandler, kingdomService, store, electionConfig,
                                villagerPremierInauguralService, parliamentService, villagerMpEntityService);
                electionTask.setCalendar(
                                realmCalendarService,
                                dev.mrlemoos.kingdom.calendar.PollingDay.fromPluginConfig(getConfig()));
                electionTask.setStateOpeningCeremony(stateOpeningCeremony);
                divisionBossBarService = new DivisionBossBarService(kingdomService);
                electionTask.setDivisionBossBarService(divisionBossBarService);
                electionTask.setMusterService(musterService);
                electionTask.schedule(ElectionTask.DEFAULT_INTERVAL_TICKS);

                dev.mrlemoos.kingdom.display.RealmSidebarService realmSidebarService =
                                new dev.mrlemoos.kingdom.display.RealmSidebarService(
                                                kingdomService, economyService, realmCalendarService,
                                                territoryResolver, loyaltyService, moraleService, oathService);
                getServer().getScheduler().runTaskTimer(this, realmSidebarService::refreshAllOnline, 60L, 40L);

                TerritoryVillagerDespawnTask territoryVillagerDespawnTask = new TerritoryVillagerDespawnTask(this,
                                villagerMpEntityService);
                territoryVillagerDespawnTask.setCityNpcServices(
                                lordMayorService, townCrierService, kingdomService, store);
                territoryVillagerDespawnTask.setClericService(clericService);
                territoryVillagerDespawnTask.setCrownSquadEntities(crownSquadEntityService);
                territoryVillagerDespawnTask.schedule(TerritoryVillagerDespawnTask.DEFAULT_INTERVAL_TICKS);

                dev.mrlemoos.kingdom.task.MassTask massTask = new dev.mrlemoos.kingdom.task.MassTask(
                                this,
                                kingdomService,
                                churchService,
                                new dev.mrlemoos.kingdom.church.MassCeremony(
                                                this, kingdomService, churchService, clericService),
                                store);
                massTask.setCongregationSource(villagerMpEntityService, territoryResolver);
                massTask.schedule(dev.mrlemoos.kingdom.task.MassTask.DEFAULT_INTERVAL_TICKS);

                getServer().getScheduler().runTaskLater(this, villagerMpEntityService::scheduleStartupSync, 40L);
                getServer().getScheduler().runTaskLater(this, royalStandardPlacer::raiseAll, 40L);
                getServer().getScheduler().runTaskLater(this, () -> {
                        crownSquadEntityService.reconcileAll();
                        boolean changed = lordMayorService.reconcileAll();
                        if (clericService.reconcileAll()) {
                                changed = true;
                        }
                        if (townCrierService.reconcileAll()) {
                                changed = true;
                        }
                        if (changed) {
                                store.saveFrom(kingdomService);
                        }
                }, 40L);

                nobleDisplay.refreshAllOnline();

                getLogger().info("Kingdom enabled.");
        }

        @Override
        public void onDisable() {
                if (trialBossBarService != null) {
                        trialBossBarService.stop();
                }
                if (electionBossBarService != null) {
                        electionBossBarService.stop();
                }
                if (divisionBossBarService != null) {
                        divisionBossBarService.clearAll();
                }
                if (store != null && kingdomService != null) {
                        store.saveFrom(kingdomService);
                }
                if (economyStore != null && economyService != null) {
                        economyStore.saveFrom(economyService);
                }
        }

        public PoliceTrialService getPoliceTrialService() {
                return policeTrialService;
        }

        public MechanicalJusticeService getMechanicalJusticeService() {
                return mechanicalJusticeService;
        }

        public LoyaltyService getLoyaltyService() {
                return loyaltyService;
        }

        public MoraleService getMoraleService() {
                return moraleService;
        }

        public LoyaltyGateService getLoyaltyGateService() {
                return loyaltyGateService;
        }

        public OathService getOathService() {
                return oathService;
        }

        public StandingRosterService getStandingRosterService() {
                return standingRosterService;
        }

        public KingdomService getKingdomService() {
                return kingdomService;
        }

        public EconomyService getEconomyService() {
                return economyService;
        }

        public EconomyCoordinator getEconomyCoordinator() {
                return economyCoordinator;
        }
}
