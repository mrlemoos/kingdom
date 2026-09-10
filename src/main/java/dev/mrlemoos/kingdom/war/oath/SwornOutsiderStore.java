package dev.mrlemoos.kingdom.war.oath;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link SwornOutsider} registrations. */
public interface SwornOutsiderStore {

    void register(SwornOutsider outsider);

    Optional<SwornOutsider> find(UUID playerId);

    Collection<SwornOutsider> allView();

    void replaceAll(List<SwornOutsider> outsiders);

    void remove(UUID playerId);
}
