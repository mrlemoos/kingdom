package dev.mrlemoos.kingdom.cloud;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;

public final class KingdomCloudManagerFactory {

    private KingdomCloudManagerFactory() {}

    public static LegacyPaperCommandManager<CommandSender> create(JavaPlugin plugin) {
        LegacyPaperCommandManager<CommandSender> manager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.simpleCoordinator());

        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }

        return manager;
    }
}
