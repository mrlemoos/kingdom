package dev.mrlemoos.kingdom;

import dev.mrlemoos.kingdom.command.CoronaCommand;
import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.command.KingdomCommand;
import dev.mrlemoos.kingdom.command.KingdomFiscalHandler;
import dev.mrlemoos.kingdom.command.KingdomPoliceHandler;
import dev.mrlemoos.kingdom.command.KingdomWhitelistHandler;
import dev.mrlemoos.kingdom.command.ResignCommand;
import dev.mrlemoos.kingdom.display.NoblePrefixDisplay;
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
import dev.mrlemoos.kingdom.economy.wealth.RealmWealthRates;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.listener.BuildConductListener;
import dev.mrlemoos.kingdom.listener.ChatPrefixListener;
import dev.mrlemoos.kingdom.listener.CoronaMerchantListener;
import dev.mrlemoos.kingdom.listener.EconomyActivityListener;
import dev.mrlemoos.kingdom.listener.JoinReminderListener;
import dev.mrlemoos.kingdom.listener.LifeEventListener;
import dev.mrlemoos.kingdom.listener.MintInteractListener;
import dev.mrlemoos.kingdom.listener.MintLecternGuardListener;
import dev.mrlemoos.kingdom.listener.MintPrepareListener;
import dev.mrlemoos.kingdom.listener.ParliamentGuiListener;
import dev.mrlemoos.kingdom.listener.TreasuryBriefingListener;
import dev.mrlemoos.kingdom.listener.TerritoryVillagerDespawnListener;
import dev.mrlemoos.kingdom.listener.TerritoryWealthListener;
import dev.mrlemoos.kingdom.listener.NobleDisplayListener;
import dev.mrlemoos.kingdom.listener.TreasuryLordListener;
import dev.mrlemoos.kingdom.listener.VillagerProfessionNametagListener;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyGateService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.police.BuildConductEnforcer;
import dev.mrlemoos.kingdom.police.BuildEnforcementConfig;
import dev.mrlemoos.kingdom.police.ActBreachDetector;
import dev.mrlemoos.kingdom.police.MechanicalJusticeConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceCourtService;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.cloud.KingdomCloudCommands;
import dev.mrlemoos.kingdom.cloud.KingdomCloudManagerFactory;
import dev.mrlemoos.kingdom.listener.PoliceGolemListener;
import dev.mrlemoos.kingdom.listener.ResignationLetterListener;
import dev.mrlemoos.kingdom.whitelist.BukkitServerWhitelistGateway;
import dev.mrlemoos.kingdom.whitelist.WhitelistService;
import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationLetterItem;
import dev.mrlemoos.kingdom.resignation.ResignationService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.command.ParliamentHandler;
import dev.mrlemoos.kingdom.command.LocateCommand;
import dev.mrlemoos.kingdom.command.TpCommand;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.task.ElectionTask;
import dev.mrlemoos.kingdom.task.TerritoryVillagerDespawnTask;
import dev.mrlemoos.kingdom.task.TerritoryWealthReconcileTask;
import dev.mrlemoos.kingdom.task.VillagerGdpTask;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.oath.InMemorySwornOutsiderStore;
import dev.mrlemoos.kingdom.war.oath.OathConfig;
import dev.mrlemoos.kingdom.war.oath.OathService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class KingdomPlugin extends JavaPlugin {

        private KingdomService kingdomService;
        private YamlKingdomStore store;
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
                warService.setConfig(WarConfig.fromPluginConfig(getConfig()));
                store.setWarService(warService);
                InMemoryStandingRosterStore standingRosterStore = new InMemoryStandingRosterStore();
                standingRosterService = new StandingRosterService(
                                kingdomService, standingRosterStore, StandingRosterConfig.fromPluginConfig(getConfig()));
                warService.setStandingRosterService(standingRosterService);
                store.setStandingRosterStore(standingRosterStore);
                store.loadInto(kingdomService);
                LoyaltyService loyaltyService = new LoyaltyService(
                                loyaltyStore, LoyaltyConfig.fromPluginConfig(getConfig()));
                this.loyaltyService = loyaltyService;
                MoraleService moraleService = new MoraleService(
                                moraleStore, MoraleConfig.fromPluginConfig(getConfig()));
                this.moraleService = moraleService;
                loyaltyGateService = new LoyaltyGateService(loyaltyService);
                oathService = new OathService(
                                kingdomService,
                                loyaltyService,
                                moraleService,
                                new InMemorySwornOutsiderStore(),
                                OathConfig.fromPluginConfig(getConfig()));
                PoliceService policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
                MechanicalJusticeService mechanicalJusticeService = new MechanicalJusticeService(
                                kingdomService,
                                policeService,
                                MechanicalJusticeConfig.fromPluginConfig(getConfig()));
                this.mechanicalJusticeService = mechanicalJusticeService;
                nobleDisplay = new NoblePrefixDisplay(kingdomService, policeService);

                economyService = new EconomyService(getConfig().getDouble("economy.starting-treasury", 100.0));
                economyStore = new YamlEconomyStore(this);
                economyStore.loadInto(economyService);
                PoliceTrialService policeTrialService = new PoliceTrialService(
                                kingdomService,
                                policeService,
                                mechanicalJusticeService,
                                economyService);
                this.policeTrialService = policeTrialService;

                EconomyConfig economyConfig = EconomyConfig.fromPluginConfig(getConfig());
                VillagerEconomyConfig villagerEconomyConfig = VillagerEconomyConfig.fromPluginConfig(getConfig());
                CoronaMerchantOfferConfig coronaMerchantOfferConfig = CoronaMerchantOfferConfig
                                .fromPluginConfig(getConfig());
                RealmWealthRates realmWealthRates = RealmWealthRates
                                .fromPluginConfig(getConfig().getConfigurationSection("economy"));
                KingdomTerritoryResolver territoryResolver = new KingdomTerritoryResolver(kingdomService);
                economyCoordinator = new EconomyCoordinator(
                                economyService,
                                kingdomService,
                                territoryResolver,
                                economyConfig,
                                villagerEconomyConfig.villagerCommerceTaxRate());
                economyCoordinator.setPersistenceHook(() -> economyStore.saveFrom(economyService));

                WorldGuardBridge.warmUp();

                TreasuryLordService treasuryLordService = new TreasuryLordService(this, economyService, economyStore);
                ElectionConfig electionConfig = ElectionConfig.fromPluginConfig(getConfig());
                ProfessionVoteBias professionVoteBias = ProfessionVoteBias.fromPluginConfig(getConfig());
                ElectionService electionService = new ElectionService(kingdomService, electionConfig);
                ResignationService resignationService = new ResignationService(kingdomService, electionService);
                ResignationLetterItem resignationLetterItem = new ResignationLetterItem(this);
                ResignationLetterDelivery resignationLetterDelivery = new ResignationLetterDelivery(kingdomService,
                                resignationService, resignationLetterItem);
                ProductiveVillagerScanner villagerScanner = new ProductiveVillagerScanner(kingdomService);
                VillagerMpEntityService villagerMpEntityService = new VillagerMpEntityService(
                                this, kingdomService, villagerScanner, territoryResolver);
                ParliamentService parliamentService = new ParliamentService(kingdomService);
                parliamentService.setProfessionVoteBias(professionVoteBias);
                parliamentService.setWarService(warService);
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
                                nobleDisplay);
                WhitelistService whitelistService = new WhitelistService(new BukkitServerWhitelistGateway());
                KingdomWhitelistHandler whitelistHandler = new KingdomWhitelistHandler(
                                whitelistService,
                                kingdomService);
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
                                new DemobilisationService(warService));
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

                KingdomCommand kingdomCommand = new KingdomCommand(
                                kingdomService, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler,
                                electionHandler, realmWealthRates, policeHandler, whitelistHandler,
                                warService, loyaltyService);
                CoronaCommand coronaCommand = new CoronaCommand(economyService, kingdomService, economyStore,
                                economyCoordinator);
                TeleportService teleportService = new TeleportService(kingdomService);
                TpCommand tpCommand = new TpCommand(teleportService, kingdomService, store, territoryResolver);
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

                getServer().getPluginManager().registerEvents(new ChatPrefixListener(kingdomService, policeService),
                                this);
                getServer().getPluginManager().registerEvents(new NobleDisplayListener(nobleDisplay), this);
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
                                                new CoronaMerchantRecipeService(coronaMerchantOfferConfig)),
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
                                                loyaltyService),
                                this);
                getServer().getPluginManager().registerEvents(new LifeEventListener(economyCoordinator, this), this);
                getServer().getPluginManager().registerEvents(new MintInteractListener(economyCoordinator), this);
                getServer().getPluginManager().registerEvents(
                                new MintLecternGuardListener(this, economyCoordinator, treasuryLordService),
                                this);
                getServer().getPluginManager().registerEvents(
                                new TreasuryBriefingListener(kingdomService, economyService, territoryResolver, this),
                                this);
                getServer().getPluginManager().registerEvents(
                                new TreasuryLordListener(treasuryLordService, economyService, kingdomService,
                                                economyStore),
                                this);
                getServer().getPluginManager().registerEvents(parliamentGuiListener, this);
                getServer().getPluginManager().registerEvents(
                                new ResignationLetterListener(
                                                kingdomService, resignationService, resignationLetterItem,
                                                resignationLetterDelivery),
                                this);
                getServer().getPluginManager().registerEvents(
                                new MintPrepareListener(parliamentHandler, parliamentGuiListener),
                                this);
                getServer().getPluginManager().registerEvents(
                                new VillagerProfessionNametagListener(villagerMpEntityService), this);
                getServer().getPluginManager().registerEvents(
                                new TerritoryVillagerDespawnListener(this, villagerMpEntityService), this);
                getServer().getPluginManager().registerEvents(
                                new PoliceGolemListener(policeService, policeGolemService, kingdomService, store),
                                this);

                getServer().getScheduler().runTaskLater(this, fiscalHandler::respawnTreasuryLords, 20L);
                getServer().getScheduler().runTaskLater(this, policeHandler::pruneStaleEntities, 20L);
                getServer().getScheduler().runTaskLater(this, policeHandler::respawnAllJudges, 20L);

                long gdpInterval = getConfig().getLong("economy.villager-gdp.tick-interval-ticks",
                                VillagerGdpTask.DEFAULT_INTERVAL_TICKS);
                VillagerGdpTask gdpTask = new VillagerGdpTask(
                                this, economyCoordinator, kingdomService, economyStore, villagerEconomyConfig);
                gdpTask.schedule(gdpInterval);

                TerritoryWealthReconcileTask wealthReconcileTask = new TerritoryWealthReconcileTask(this,
                                economyService, kingdomService, economyStore);
                wealthReconcileTask.schedule(gdpInterval);

                ElectionTask electionTask = new ElectionTask(
                                this, electionService, electionHandler, kingdomService, store, electionConfig,
                                villagerPremierInauguralService);
                electionTask.schedule(ElectionTask.DEFAULT_INTERVAL_TICKS);

                TerritoryVillagerDespawnTask territoryVillagerDespawnTask = new TerritoryVillagerDespawnTask(this,
                                villagerMpEntityService);
                territoryVillagerDespawnTask.schedule(TerritoryVillagerDespawnTask.DEFAULT_INTERVAL_TICKS);

                getServer().getScheduler().runTaskLater(this, villagerMpEntityService::scheduleStartupSync, 40L);

                nobleDisplay.refreshAllOnline();

                getLogger().info("Kingdom enabled.");
        }

        @Override
        public void onDisable() {
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
