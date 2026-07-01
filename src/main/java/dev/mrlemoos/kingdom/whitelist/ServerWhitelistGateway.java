package dev.mrlemoos.kingdom.whitelist;

import java.util.Set;
import java.util.UUID;

public interface ServerWhitelistGateway {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isWhitelisted(UUID playerId);

    void setWhitelisted(UUID playerId, boolean whitelisted);

    Set<UUID> whitelistedPlayerIds();
}
