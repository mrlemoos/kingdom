package dev.mrlemoos.kingdom.war.squad;

import java.util.Objects;
import java.util.UUID;

/**
 * A single piece of rank-and-file assigned to a squad, drawn from one of the two pools described
 * in the war glossary: a {@link PressedVillager} conscripted from territory population (see
 * {@code ConscriptionService}), or a {@link CrownUnit} bought from treasury (see {@code
 * CrownSquadService}). Identified purely by entity/unit {@code UUID} — domain-only, no Bukkit
 * entity reference.
 */
public sealed interface SquadMember permits SquadMember.PressedVillager, SquadMember.CrownUnit {

    UUID id();

    record PressedVillager(UUID id) implements SquadMember {
        public PressedVillager {
            Objects.requireNonNull(id, "id");
        }
    }

    record CrownUnit(UUID id) implements SquadMember {
        public CrownUnit {
            Objects.requireNonNull(id, "id");
        }
    }
}
