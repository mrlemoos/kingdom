package dev.leo.kingdom;

import dev.leo.kingdom.command.CoronaCommand;
import dev.leo.kingdom.command.KingdomCommand;
import dev.leo.kingdom.command.KingdomFiscalHandler;
import dev.leo.kingdom.command.TpCommand;
import dev.leo.kingdom.display.NoblePrefixDisplay;
import dev.leo.kingdom.economy.EconomyCoordinator;
import dev.leo.kingdom.economy.income.EconomyConfig;
import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.leo.kingdom.listener.ChatPrefixListener;
import dev.leo.kingdom.listener.EconomyActivityListener;
import dev.leo.kingdom.listener.JoinReminderListener;
import dev.leo.kingdom.listener.LifeEventListener;
import dev.leo.kingdom.listener.MintInteractListener;
import dev.leo.kingdom.listener.MintPrepareListener;
import dev.leo.kingdom.listener.ParliamentGuiListener;
import dev.leo.kingdom.listener.TreasuryBriefingListener;
import dev.leo.kingdom.listener.NobleDisplayListener;
import dev.leo.kingdom.listener.TreasuryLordListener;
import dev.leo.kingdom.mint.TreasuryLordService;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.command.ParliamentHandler;
import dev.leo.kingdom.service.ParliamentService;
import dev.leo.kingdom.service.TeleportService;
import dev.leo.kingdom.storage.YamlEconomyStore;
import dev.leo.kingdom.storage.YamlKingdomStore;
import dev.leo.kingdom.task.VillagerGdpTask;
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
        KingdomTerritoryResolver territoryResolver = new KingdomTerritoryResolver(kingdomService);
        economyCoordinator = new EconomyCoordinator(
                economyService, kingdomService, territoryResolver, economyConfig);
        economyCoordinator.setPersistenceHook(() -> economyStore.saveFrom(economyService));

        TreasuryLordService treasuryLordService = new TreasuryLordService(this, economyService, economyStore);
        ParliamentService parliamentService = new ParliamentService(kingdomService);
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
                this);
        ParliamentGuiListener parliamentGuiListener = new ParliamentGuiListener(parliamentHandler);
        parliamentHandler.setHubGuiOpener(parliamentGuiListener::openHubGui);

        KingdomCommand kingdomCommand = new KingdomCommand(
                kingdomService, store, nobleDisplay, fiscalHandler, economyService, parliamentHandler);
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

        getServer().getPluginManager().registerEvents(new ChatPrefixListener(kingdomService), this);
        getServer().getPluginManager().registerEvents(new NobleDisplayListener(nobleDisplay), this);
        getServer().getPluginManager().registerEvents(
                new JoinReminderListener(
                        kingdomService,
                        getConfig().getBoolean("join-reminder", true),
                        getConfig().getStringList("join-message")),
                this);
        getServer().getPluginManager().registerEvents(new EconomyActivityListener(economyCoordinator), this);
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
                new MintPrepareListener(parliamentHandler, parliamentGuiListener),
                this);

        getServer().getScheduler().runTaskLater(this, fiscalHandler::respawnTreasuryLords, 20L);

        long gdpInterval = getConfig().getLong("economy.villager-gdp.tick-interval-ticks", VillagerGdpTask.DEFAULT_INTERVAL_TICKS);
        VillagerGdpTask gdpTask = new VillagerGdpTask(this, economyCoordinator, kingdomService, economyStore);
        gdpTask.schedule(gdpInterval);

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
