package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import java.util.Optional;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Resolves the curfew window that applies to a kingdom: decree when present (including lifted),
 * otherwise the plugin fallback.
 */
public final class DecreeCurfewResolver {

    private DecreeCurfewResolver() {}

    public static CurfewEnforcementConfig resolve(
            Optional<CurfewEnforcementConfig> decreeCurfew, FileConfiguration pluginConfig) {
        if (decreeCurfew != null && decreeCurfew.isPresent()) {
            return decreeCurfew.get();
        }
        return CurfewEnforcementConfig.fromPluginConfig(pluginConfig);
    }
}
