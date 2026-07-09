package dev.mrlemoos.kingdom.war.desertion;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryTreasonReviewStore implements TreasonReviewStore {

    private final Map<UUID, TreasonReviewFlag> flagsByPlayer = new LinkedHashMap<>();

    @Override
    public void raise(TreasonReviewFlag flag) {
        Objects.requireNonNull(flag, "flag");
        flagsByPlayer.put(flag.playerId(), flag);
    }

    @Override
    public boolean isFlagged(UUID playerId) {
        return flagsByPlayer.containsKey(playerId);
    }

    @Override
    public Optional<TreasonReviewFlag> findFlag(UUID playerId) {
        return Optional.ofNullable(flagsByPlayer.get(playerId));
    }

    @Override
    public Collection<TreasonReviewFlag> allFlagsView() {
        return List.copyOf(flagsByPlayer.values());
    }

    @Override
    public void clear(UUID playerId) {
        flagsByPlayer.remove(playerId);
    }
}
