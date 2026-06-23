package dev.leo.kingdom.storage;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TeleportPlace;
import dev.leo.kingdom.model.TitleStyle;
import dev.leo.kingdom.service.KingdomService;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlKingdomStore {

    private final JavaPlugin plugin;
    private final File dataFile;

    public YamlKingdomStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void loadInto(KingdomService service) {
        if (!dataFile.exists()) {
            seedFromConfig(service);
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Map<String, Kingdom> kingdoms = new HashMap<>();
        ConfigurationSection kingdomSection = data.getConfigurationSection("kingdoms");
        if (kingdomSection != null) {
            for (String id : kingdomSection.getKeys(false)) {
                ConfigurationSection entry = kingdomSection.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                Kingdom kingdom = new Kingdom(id, entry.getString("display-name", id));
                kingdom.setWorldName(entry.getString("world"));
                kingdom.setWorldGuardRegion(entry.getString("worldguard-region"));
                kingdom.replaceTeleports(readTeleports(entry.getConfigurationSection("teleports")));
                kingdoms.put(kingdom.getId(), kingdom);
            }
        }

        Map<UUID, PlayerMembership> memberships = new HashMap<>();
        ConfigurationSection playerSection = data.getConfigurationSection("players");
        if (playerSection != null) {
            for (String uuidString : playerSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    ConfigurationSection entry = playerSection.getConfigurationSection(uuidString);
                    if (entry == null) {
                        continue;
                    }
                    String kingdomId = entry.getString("kingdom");
                    if (kingdomId == null || !kingdoms.containsKey(Kingdom.normaliseId(kingdomId))) {
                        plugin.getLogger().warning("Skipping player " + uuidString + " with unknown kingdom.");
                        continue;
                    }
                    PlayerMembership membership = new PlayerMembership(playerId, Kingdom.normaliseId(kingdomId));
                    String rankName = entry.getString("rank");
                    if (rankName != null) {
                        NobleRank rank = NobleRank.fromCommand(rankName);
                        TitleStyle style = TitleStyle.MASCULINE;
                        String styleName = entry.getString("title-style");
                        if (styleName != null) {
                            style = TitleStyle.valueOf(styleName.toUpperCase());
                        }
                        membership.assignTitle(rank, style);
                    }
                    memberships.put(playerId, membership);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Skipping invalid player entry: " + uuidString, ex);
                }
            }
        }

        service.replaceState(kingdoms, memberships);
    }

    public void saveFrom(KingdomService service) {
        FileConfiguration data = new YamlConfiguration();

        for (Kingdom kingdom : service.listKingdoms()) {
            String path = "kingdoms." + kingdom.getId();
            data.set(path + ".display-name", kingdom.getDisplayName());
            data.set(path + ".world", kingdom.getWorldName());
            data.set(path + ".worldguard-region", kingdom.getWorldGuardRegion());
            writeTeleports(data, path + ".teleports", kingdom.getTeleportsView());
        }

        for (PlayerMembership membership : service.getMembershipsView().values()) {
            String path = "players." + membership.getPlayerId();
            data.set(path + ".kingdom", membership.getKingdomId());
            if (membership.hasNobleTitle()) {
                data.set(path + ".rank", membership.getRank().name().toLowerCase());
                data.set(path + ".title-style", membership.getTitleStyle().name().toLowerCase());
            }
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdom data.", ex);
        }
    }

    private void seedFromConfig(KingdomService service) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection kingdomSection = config.getConfigurationSection("kingdoms");
        if (kingdomSection == null) {
            return;
        }
        for (String id : kingdomSection.getKeys(false)) {
            ConfigurationSection entry = kingdomSection.getConfigurationSection(id);
            String displayName = entry != null ? entry.getString("display-name", id) : id;
            service.createKingdom(id, displayName);
        }
    }

    static void writeTeleports(FileConfiguration config, String path, Map<String, TeleportPlace> teleports) {
        for (TeleportPlace place : teleports.values()) {
            String placePath = path + "." + place.name();
            config.set(placePath + ".world", place.worldName());
            config.set(placePath + ".x", place.x());
            config.set(placePath + ".y", place.y());
            config.set(placePath + ".z", place.z());
            config.set(placePath + ".yaw", place.yaw());
            config.set(placePath + ".pitch", place.pitch());
        }
    }

    static Map<String, TeleportPlace> readTeleports(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, TeleportPlace> teleports = new HashMap<>();
        for (String name : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(name);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            if (worldName == null) {
                continue;
            }
            teleports.put(
                    Kingdom.normaliseId(name),
                    TeleportPlace.of(
                            name,
                            worldName,
                            entry.getDouble("x"),
                            entry.getDouble("y"),
                            entry.getDouble("z"),
                            (float) entry.getDouble("yaw"),
                            (float) entry.getDouble("pitch")));
        }
        return teleports;
    }
}
