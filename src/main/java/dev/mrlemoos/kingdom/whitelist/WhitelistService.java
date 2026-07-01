package dev.mrlemoos.kingdom.whitelist;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Set;
import java.util.UUID;

public final class WhitelistService {

    private final ServerWhitelistGateway gateway;

    public WhitelistService(ServerWhitelistGateway gateway) {
        this.gateway = gateway;
    }

    public boolean isEnabled() {
        return gateway.isEnabled();
    }

    public Set<UUID> whitelistedPlayerIds() {
        return gateway.whitelistedPlayerIds();
    }

    public WhitelistResult setEnabled(NobleRank actorRank, boolean operator, boolean enabled) {
        if (!WhitelistAuthority.canManage(actorRank, operator)) {
            return WhitelistResult.fail("Only the King, Queen, or an operator may change the server whitelist.");
        }
        gateway.setEnabled(enabled);
        return WhitelistResult.ok(enabled
                ? "Server whitelist enabled. Only listed players may join the server."
                : "Server whitelist disabled.");
    }

    public WhitelistResult allowPlayer(NobleRank actorRank, boolean operator, UUID playerId) {
        if (!WhitelistAuthority.canManage(actorRank, operator)) {
            return WhitelistResult.fail("Only the King, Queen, or an operator may manage the server whitelist.");
        }
        if (gateway.isWhitelisted(playerId)) {
            return WhitelistResult.fail("That player is already on the server whitelist.");
        }
        gateway.setWhitelisted(playerId, true);
        return WhitelistResult.ok("Added player to the server whitelist.");
    }

    public WhitelistResult disallowPlayer(NobleRank actorRank, boolean operator, UUID playerId) {
        if (!WhitelistAuthority.canManage(actorRank, operator)) {
            return WhitelistResult.fail("Only the King, Queen, or an operator may manage the server whitelist.");
        }
        if (!gateway.isWhitelisted(playerId)) {
            return WhitelistResult.fail("That player is not on the server whitelist.");
        }
        gateway.setWhitelisted(playerId, false);
        return WhitelistResult.ok("Removed player from the server whitelist.");
    }
}
