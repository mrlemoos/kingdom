package dev.mrlemoos.kingdom.war.siege;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link SiegeReleaseGrant} registrations, keyed by subject and war. */
public interface SiegeReleaseStore {

    void save(SiegeReleaseGrant grant);

    Optional<SiegeReleaseGrant> find(UUID subjectId, String warId);

    void revoke(UUID subjectId, String warId);

    /** Full audit view of every current grant, across all subjects and wars. */
    Collection<SiegeReleaseGrant> allView();
}
