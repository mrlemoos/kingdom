package dev.leo.kingdom.worldguard;

import org.bukkit.Bukkit;
import org.bukkit.World;

public final class WorldGuardBridge {

    private WorldGuardBridge() {}

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static boolean regionExists(String worldName, String regionId) {
        if (!isAvailable() || worldName == null || regionId == null || regionId.isBlank()) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        try {
            Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Object wgPlugin = wgPluginClass.getMethod("inst").invoke(null);

            Object platform = wgPluginClass.getMethod("getPlatform").invoke(wgPlugin);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);

            Class<?> weWorldClass = Class.forName("com.sk89q.worldedit.world.World");
            Object manager = container.getClass().getMethod("get", weWorldClass).invoke(container, adaptedWorld);
            if (manager == null) {
                return false;
            }

            Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, regionId);
            return region != null;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
