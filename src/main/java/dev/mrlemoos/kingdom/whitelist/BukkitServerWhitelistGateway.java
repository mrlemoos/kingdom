package dev.mrlemoos.kingdom.whitelist;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class BukkitServerWhitelistGateway implements ServerWhitelistGateway {

    @Override
    public boolean isEnabled() {
        return Bukkit.hasWhitelist();
    }

    @Override
    public void setEnabled(boolean enabled) {
        Bukkit.setWhitelist(enabled);
    }

    @Override
    public boolean isWhitelisted(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return player.isWhitelisted();
    }

    @Override
    public void setWhitelisted(UUID playerId, boolean whitelisted) {
        Bukkit.getOfflinePlayer(playerId).setWhitelisted(whitelisted);
    }

    @Override
    public Set<UUID> whitelistedPlayerIds() {
        Set<UUID> ids = new LinkedHashSet<>();
        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
            ids.add(player.getUniqueId());
        }
        return ids;
    }
}
