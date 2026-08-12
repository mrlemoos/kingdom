package dev.mrlemoos.kingdom.police;

import java.util.UUID;

/**
 * Strikes a convict from every build-permit register when a prison sentence begins. Release does
 * not restore the permit: the released player walks to city hall and applies again.
 */
@FunctionalInterface
public interface BuildPermitRevoker {

    /** @return the number of permits revoked */
    int revokeAllPermits(UUID convictId);

    static BuildPermitRevoker none() {
        return convictId -> 0;
    }
}
