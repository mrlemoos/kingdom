package dev.mrlemoos.kingdom.police;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves which kingdom's linked territory a player currently occupies.
 * Bukkit/WorldGuard implements this; domain callers treat absence as outside jurisdiction.
 */
@FunctionalInterface
public interface JurisdictionPort {

    /**
     * @return kingdom id whose linked WorldGuard region contains the player, if any
     */
    Optional<String> kingdomAt(UUID playerId);
}
