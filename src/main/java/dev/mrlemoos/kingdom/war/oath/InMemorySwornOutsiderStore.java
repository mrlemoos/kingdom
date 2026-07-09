package dev.mrlemoos.kingdom.war.oath;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemorySwornOutsiderStore implements SwornOutsiderStore {

    private final Map<UUID, SwornOutsider> outsidersByPlayer = new LinkedHashMap<>();

    @Override
    public void register(SwornOutsider outsider) {
        Objects.requireNonNull(outsider, "outsider");
        outsidersByPlayer.put(outsider.playerId(), outsider);
    }

    @Override
    public Optional<SwornOutsider> find(UUID playerId) {
        return Optional.ofNullable(outsidersByPlayer.get(playerId));
    }

    @Override
    public Collection<SwornOutsider> allView() {
        return List.copyOf(outsidersByPlayer.values());
    }

    @Override
    public void remove(UUID playerId) {
        outsidersByPlayer.remove(playerId);
    }
}
