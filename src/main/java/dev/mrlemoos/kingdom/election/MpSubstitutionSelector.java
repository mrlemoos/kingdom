package dev.mrlemoos.kingdom.election;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Picks a substitute territory villager when a seated villager MP cannot be re-claimed: same
 * profession first, then any eligible candidate.
 */
public final class MpSubstitutionSelector {

    public record Candidate(String id, String profession) {
        public Candidate {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(profession, "profession");
        }
    }

    private MpSubstitutionSelector() {}

    public static Optional<Candidate> select(String preferredProfession, List<Candidate> eligible) {
        if (eligible == null || eligible.isEmpty()) {
            return Optional.empty();
        }
        String preferred = preferredProfession == null ? "" : preferredProfession;
        for (Candidate candidate : eligible) {
            if (preferred.equalsIgnoreCase(candidate.profession())) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(eligible.get(0));
    }
}
