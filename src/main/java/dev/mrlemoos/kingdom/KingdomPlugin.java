package dev.mrlemoos.kingdom;

import dev.mrlemoos.kingdom.command.CoronaCommand;
import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.command.KingdomCommand;
import dev.mrlemoos.kingdom.command.KingdomFiscalHandler;
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
import dev.mrlemoos.kingdom.listener.ChatPrefixListener;
import dev.mrlemoos.kingdom.listener.CoronaMerchantListener;
import dev.mrlemoos.kingdom.listener.EconomyActivityListener;
import dev.mrlemoos.kingdom.listener.JoinReminderListener;
import dev.mrlemoos.kingdom.listener.LifeEventListener;
import dev.mrlemoos.kingdom.listener.MintInteractListener;
import dev.mrlemoos.kingdom.listener.MintPrepareListener;
import dev.mrlemoos.kingdom.listener.ParliamentGuiListener;
import dev.mrlemoos.kingdom.listener.TreasuryBriefingListener;
import dev.mrlemoos.kingdom.listener.TerritoryVillagerDespawnListener;
import dev.mrlemoos.kingdom.listener.TerritoryWealthListener;
import dev.mrlemoos.kingdom.listener.NobleDisplayListener;
import dev.mrlemoos.kingdom.listener.TreasuryLordListener;
import dev.mrlemoos.kingdom.listener.VillagerProfessionNametagListener;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import dev.mrlemoos.kingdom.command.TpCommand;
import dev.mrlemoos.kingdom.listener.ResignationLetterListener;
import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationLetterItem;
import dev.mrlemoos.kingdom.resignation.ResignationService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.command.ParliamentHandler;
import dev.mrlemoos.kingdom.service.TeleportService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.task.ElectionTask;
import dev.mrlemoos.kingdom.task.TerritoryVillagerDespawnTask;
import dev.mrlemoos.kingdom.task.TerritoryWealthReconcileTask;
import dev.mrlemoos.kingdom.task.VillagerGdpTask;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomPlugin extends JavaPlugin {

    private KingdomService kingdomService;
    private YamlKingdomStore store;
    private NoblePrefixDisplay nobleDisplay;
    private EconomyService economyService;
    private YamlEconomyStore economyStore;
    private EconomyCoordinator economyCoordinator;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        kingdomService = new KingdomService();
        store = new YamlKingdomStore(this);
        store.loadInto(kingdomService);
        nobleDisplay = new NoblePrefixDisplay(kingdomService);

        economyService = new EconomyService(getConfig().getDouble("economy.starting-treasury", 100.0));
        economyStore = new YamlEconomyStore(this);
        economyStore.loadInto(economyService);

        EconomyConfig economyConfig = EconomyConfig.fromPluginConfig(getConfig());
        VillagerEconomyConfig villagerEconomyConfig = VillagerEconomyConfig.fromPluginConfig(getConfig());
        CoronaMerchantOfferConfig coronaMerchantOfferConfig = CoronaMerchantOfferConfig.fromPluginConfig(getConfig());
        RealmWealthRates realmWealthRates = RealmWealthRates.fromPluginConfig(getConfig().getConfigurationSection("economy"));
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
        ResignationLetterDelivery resignationLetterDelivery =
                new ResignationLetterDelivery(kingdomService, resignationService, resignationLetterItem);
        ProductiveVillagerScanner villagerScanner = new ProductiveVillagerScanner(kingdomService);
        VillagerMpEntityService villagerMpEntityService = new VillagerMpEntityService(
                this, kingdomService, villagerScanner, territoryResolver);
        ParliamentService parliamentService = new ParliamentService(kingdomService);
        parliamentService.setProfessionVoteBias(professionVoteBias);
        VillagerPremierInauguralService villagerPremierInauguralService = new VillagerPremierInauguralService(
                kingdomService, economyService, electionService, parliamentService, professionVoteBias, electionConfig);
        ElectionHandler electionHandler = new ElectionHandler(
                electionService,
                kingdomService,
                store,
                villagerScanner,
                villagerMpEntityService,
                nobleDisplay,
                villagerPremierInauguralService);
        KingdomFiscalHandler fiscalHandler = new KingdomFiscalHandler(
                economyService, kingdomService, economyStore, territoryResolver, treasuryLordService, this);
        ParliamentHandler parliamentHandler = new ParliamentHandler(
                parliamentService,
                kingdomService,
                economyService,
                store,
                economyStore,
                territoryResolver,
                treasuryLordService,
                this,
                villagerPremierInauguralService);
        ResignCommand resignCommand = new ResignCommand(
                this,
                kingdomService,
                resignationService,
                villagerMpEntityService,
                nobleDisplay,
                store,
                resignationLetterDelivery);
        ParliamentGuiListener parliamentGuiListener = new ParliamentGuiListener(parliamentHandler, resignCommand);
        parliamentHandler.setHubGuiOpener(parliamentGuiListener::openHubGui);

        KingdomCommand kingdomCommand = new KingdomCommand(
                kingdomService, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler, electionHandler, realmWealthRates);
        var kingdom = getCommand("kingdom");
        if (kingdom == null) {
            getLogger().severe("Command 'kingdom' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        kingdom.setExecutor(kingdomCommand);
        kingdom.setTabCompleter(kingdomCommand);

        CoronaCommand coronaCommand = new CoronaCommand(economyService, kingdomService, economyStore, economyCoordinator);
        var corona = getCommand("corona");
        if (corona == null) {
            getLogger().severe("Command 'corona' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        corona.setExecutor(coronaCommand);
        corona.setTabCompleter(coronaCommand);

        TeleportService teleportService = new TeleportService(kingdomService);
        TpCommand tpCommand = new TpCommand(teleportService, kingdomService, store, territoryResolver);
        var tp = getCommand("tp");
        if (tp == null) {
            getLogger().severe("Command 'tp' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        tp.setExecutor(tpCommand);
        tp.setTabCompleter(tpCommand);

        var resign = getCommand("resign");
        if (resign == null) {
            getLogger().severe("Command 'resign' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        resign.setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Only players may use this command.");
                return true;
            }
            return resignCommand.handle(player);
        });

        getServer().getPluginManager().registerEvents(new ChatPrefixListener(kingdomService), this);
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
        getServer().getPluginManager().registerEvents(new LifeEventListener(economyCoordinator, this), this);
        getServer().getPluginManager().registerEvents(new MintInteractListener(economyCoordinator), this);
        getServer().getPluginManager().registerEvents(
                new TreasuryBriefingListener(kingdomService, economyService, territoryResolver, this),
                this);
        getServer().getPluginManager().registerEvents(
                new TreasuryLordListener(treasuryLordService, economyService, kingdomService, economyStore),
                this);
        getServer().getPluginManager().registerEvents(parliamentGuiListener, this);
        getServer().getPluginManager().registerEvents(
                new ResignationLetterListener(
                        kingdomService, resignationService, resignationLetterItem, resignationLetterDelivery),
                this);
        getServer().getPluginManager().registerEvents(
                new MintPrepareListener(parliamentHandler, parliamentGuiListener),
                this);
        getServer().getPluginManager().registerEvents(
                new VillagerProfessionNametagListener(villagerMpEntityService), this);
        getServer().getPluginManager().registerEvents(
                new TerritoryVillagerDespawnListener(this, villagerMpEntityService), this);

        getServer().getScheduler().runTaskLater(this, fiscalHandler::respawnTreasuryLords, 20L);

        long gdpInterval = getConfig().getLong("economy.villager-gdp.tick-interval-ticks", VillagerGdpTask.DEFAULT_INTERVAL_TICKS);
        VillagerGdpTask gdpTask = new VillagerGdpTask(
                this, economyCoordinator, kingdomService, economyStore, villagerEconomyConfig);
        gdpTask.schedule(gdpInterval);

        TerritoryWealthReconcileTask wealthReconcileTask =
                new TerritoryWealthReconcileTask(this, economyService, kingdomService, economyStore);
        wealthReconcileTask.schedule(gdpInterval);

        ElectionTask electionTask = new ElectionTask(
                this, electionService, electionHandler, kingdomService, store, electionConfig, villagerPremierInauguralService);
        electionTask.schedule(ElectionTask.DEFAULT_INTERVAL_TICKS);

        TerritoryVillagerDespawnTask territoryVillagerDespawnTask =
                new TerritoryVillagerDespawnTask(this, villagerMpEntityService);
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
