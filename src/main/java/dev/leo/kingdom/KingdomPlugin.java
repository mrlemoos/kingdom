package dev.leo.kingdom;

import dev.leo.kingdom.command.KingdomCommand;
import dev.leo.kingdom.display.NoblePrefixDisplay;
import dev.leo.kingdom.listener.ChatPrefixListener;
import dev.leo.kingdom.listener.JoinReminderListener;
import dev.leo.kingdom.listener.NobleDisplayListener;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlKingdomStore;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomPlugin extends JavaPlugin {

    private KingdomService kingdomService;
    private YamlKingdomStore store;
    private NoblePrefixDisplay nobleDisplay;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        kingdomService = new KingdomService();
        store = new YamlKingdomStore(this);
        store.loadInto(kingdomService);
        nobleDisplay = new NoblePrefixDisplay(kingdomService);

        KingdomCommand kingdomCommand = new KingdomCommand(kingdomService, store, nobleDisplay);
        var command = getCommand("kingdom");
        if (command == null) {
            getLogger().severe("Command 'kingdom' missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(kingdomCommand);
        command.setTabCompleter(kingdomCommand);

        getServer().getPluginManager().registerEvents(new ChatPrefixListener(kingdomService), this);
        getServer().getPluginManager().registerEvents(new NobleDisplayListener(nobleDisplay), this);
        getServer().getPluginManager().registerEvents(
                new JoinReminderListener(
                        kingdomService,
                        getConfig().getBoolean("join-reminder", true),
                        getConfig().getStringList("join-message")),
                this);

        nobleDisplay.refreshAllOnline();

        getLogger().info("Kingdom enabled.");
    }

    @Override
    public void onDisable() {
        if (store != null && kingdomService != null) {
            store.saveFrom(kingdomService);
        }
    }

    public KingdomService getKingdomService() {
        return kingdomService;
    }
}
