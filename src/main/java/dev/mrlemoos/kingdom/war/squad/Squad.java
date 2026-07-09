package dev.mrlemoos.kingdom.war.squad;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A capped group of rank-and-file ({@link SquadMember}) assigned to one player officer on levy
 * (see the war glossary's Squad entry). Immutable snapshot — {@code SquadService} replaces the
 * stored instance via {@link #withState} on a command or officer-morale rout rather than mutating
 * in place.
 */
public record Squad(UUID id, String kingdomId, UUID officerId, Set<SquadMember> members, SquadState state) {

    public Squad {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(officerId, "officerId");
        Objects.requireNonNull(state, "state");
        if (kingdomId == null || kingdomId.isBlank()) {
            throw new IllegalArgumentException("kingdomId must not be blank");
        }
        members = Set.copyOf(Objects.requireNonNull(members, "members"));
    }

    public Squad withState(SquadState newState) {
        return new Squad(id, kingdomId, officerId, members, newState);
    }
}
