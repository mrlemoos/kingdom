package dev.mrlemoos.kingdom.war.siege;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemorySiegeReleaseStore implements SiegeReleaseStore {

    private final Map<String, SiegeReleaseGrant> grantsByKey = new LinkedHashMap<>();

    @Override
    public void save(SiegeReleaseGrant grant) {
        Objects.requireNonNull(grant, "grant");
        grantsByKey.put(key(grant.subjectId(), grant.warId()), grant);
    }

    @Override
    public Optional<SiegeReleaseGrant> find(UUID subjectId, String warId) {
        return Optional.ofNullable(grantsByKey.get(key(subjectId, warId)));
    }

    @Override
    public void revoke(UUID subjectId, String warId) {
        grantsByKey.remove(key(subjectId, warId));
    }

    @Override
    public Collection<SiegeReleaseGrant> allView() {
        return List.copyOf(grantsByKey.values());
    }

    private static String key(UUID subjectId, String warId) {
        return subjectId + "|" + warId;
    }
}
