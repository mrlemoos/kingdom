package dev.mrlemoos.kingdom.church;

import java.util.ArrayList;
import java.util.List;

/**
 * Which rites one clicker may ask the cleric for, and no other. Plain facts in, rites out: the
 * rites window asks this, and each rite still goes through {@link ChurchService} for its own rules.
 */
public final class RitesEligibility {

    private final boolean member;
    private final boolean atChurch;
    private final boolean consecrated;
    private final boolean crown;
    private final boolean married;
    private final boolean partnerAtChurch;
    private final boolean heldExperience;
    private final boolean villagerAwaitingRites;
    private final boolean marriagesStanding;

    private RitesEligibility(Builder builder) {
        this.member = builder.member;
        this.atChurch = builder.atChurch;
        this.consecrated = builder.consecrated;
        this.crown = builder.crown;
        this.married = builder.married;
        this.partnerAtChurch = builder.partnerAtChurch;
        this.heldExperience = builder.heldExperience;
        this.villagerAwaitingRites = builder.villagerAwaitingRites;
        this.marriagesStanding = builder.marriagesStanding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Rite> rites() {
        List<Rite> rites = new ArrayList<>();
        if (!member) {
            return rites;
        }
        boolean atAltar = atChurch && consecrated;
        if (atChurch && !consecrated) {
            rites.add(Rite.CONSECRATE);
        }
        if (atAltar && !married && partnerAtChurch) {
            rites.add(Rite.MARRY);
        }
        if (atAltar && married) {
            rites.add(Rite.DIVORCE);
        }
        // The Crown's remedy is no rite at the altar: it needs no church at all.
        if (crown && marriagesStanding) {
            rites.add(Rite.ANNUL);
        }
        if (atAltar && heldExperience) {
            rites.add(Rite.FUNERAL);
        }
        if (atAltar && villagerAwaitingRites) {
            rites.add(Rite.VILLAGER_FUNERAL);
        }
        return List.copyOf(rites);
    }

    public static final class Builder {
        private boolean member;
        private boolean atChurch;
        private boolean consecrated;
        private boolean crown;
        private boolean married;
        private boolean partnerAtChurch;
        private boolean heldExperience;
        private boolean villagerAwaitingRites;
        private boolean marriagesStanding;

        private Builder() {}

        /** A subject of the realm the cleric serves. */
        public Builder member(boolean member) {
            this.member = member;
            return this;
        }

        public Builder atChurch(boolean atChurch) {
            this.atChurch = atChurch;
            return this;
        }

        public Builder consecrated(boolean consecrated) {
            this.consecrated = consecrated;
            return this;
        }

        /** The King or Queen of that realm. */
        public Builder crown(boolean crown) {
            this.crown = crown;
            return this;
        }

        public Builder married(boolean married) {
            this.married = married;
            return this;
        }

        /** Some unmarried fellow subject stands at the church to be asked. */
        public Builder partnerAtChurch(boolean partnerAtChurch) {
            this.partnerAtChurch = partnerAtChurch;
            return this;
        }

        public Builder heldExperience(boolean heldExperience) {
            this.heldExperience = heldExperience;
            return this;
        }

        public Builder villagerAwaitingRites(boolean villagerAwaitingRites) {
            this.villagerAwaitingRites = villagerAwaitingRites;
            return this;
        }

        /** The realm holds at least one marriage the Crown might annul. */
        public Builder marriagesStanding(boolean marriagesStanding) {
            this.marriagesStanding = marriagesStanding;
            return this;
        }

        public RitesEligibility build() {
            return new RitesEligibility(this);
        }
    }
}
