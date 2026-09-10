package dev.mrlemoos.kingdom.hub;

import dev.mrlemoos.kingdom.city.gui.GazetteLiveState;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the Realm Hub needs to know about one subject at one moment: who they are, what is live
 * in their realm, and where the realm's offices stand. Read fresh each time the hub opens and never
 * persisted. Plain domain data with no platform dependency, so the hub's content is unit-testable.
 */
public final class RealmHubSnapshot {

    private final boolean member;
    private final String kingdomName;
    private final NobleRank rank;
    private final String titleLabel;
    private final boolean hasBuildPermit;
    private final boolean buildEnforcementActive;
    private final boolean warEnabled;
    private final boolean oathOfServiceOpened;
    private final String militaryMoraleTier;
    private final int standingRosterSize;
    private final int standingRosterCap;
    private final boolean rostered;
    private final boolean conscriptionEnabled;
    private final boolean electionOpen;
    private final String electionLabel;
    private final boolean pollingOpen;
    private final String referendumQuestion;
    private final boolean divisionAwaitingVote;
    private final boolean wanted;
    private final boolean resignationToReview;
    private final String musterStatus;
    private final String siegeStatus;
    private final String resignationSummary;
    private final String granaryRegion;
    private final String warCapitalRegion;
    private final Map<RealmHubTopic, List<SitePoint>> sites;
    private final SitePoint viewer;
    private final GazetteLiveState live;

    private RealmHubSnapshot(Builder builder) {
        this.member = builder.member;
        this.kingdomName = builder.kingdomName;
        this.rank = builder.rank;
        this.titleLabel = builder.titleLabel;
        this.hasBuildPermit = builder.hasBuildPermit;
        this.buildEnforcementActive = builder.buildEnforcementActive;
        this.warEnabled = builder.warEnabled;
        this.oathOfServiceOpened = builder.oathOfServiceOpened;
        this.militaryMoraleTier = builder.militaryMoraleTier;
        this.standingRosterSize = builder.standingRosterSize;
        this.standingRosterCap = builder.standingRosterCap;
        this.rostered = builder.rostered;
        this.conscriptionEnabled = builder.conscriptionEnabled;
        this.electionOpen = builder.electionOpen;
        this.electionLabel = builder.electionLabel;
        this.pollingOpen = builder.pollingOpen;
        this.referendumQuestion = builder.referendumQuestion;
        this.divisionAwaitingVote = builder.divisionAwaitingVote;
        this.wanted = builder.wanted;
        this.resignationToReview = builder.resignationToReview;
        this.musterStatus = builder.musterStatus;
        this.siegeStatus = builder.siegeStatus;
        this.resignationSummary = builder.resignationSummary;
        this.granaryRegion = builder.granaryRegion;
        this.warCapitalRegion = builder.warCapitalRegion;
        Map<RealmHubTopic, List<SitePoint>> copied = new EnumMap<>(RealmHubTopic.class);
        for (Map.Entry<RealmHubTopic, List<SitePoint>> entry : builder.sites.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.sites = copied;
        this.viewer = builder.viewer;
        this.live = builder.live;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean member() {
        return member;
    }

    public String kingdomName() {
        return kingdomName;
    }

    /** The subject's rank, or null for an untitled citizen. */
    public NobleRank rank() {
        return rank;
    }

    public String titleLabel() {
        return titleLabel;
    }

    public boolean hasBuildPermit() {
        return hasBuildPermit;
    }

    /** False when no capital is sited, in which case the realm's land is free to build. */
    public boolean buildEnforcementActive() {
        return buildEnforcementActive;
    }

    public boolean warEnabled() {
        return warEnabled;
    }

    public boolean oathOfServiceOpened() {
        return oathOfServiceOpened;
    }

    public String militaryMoraleTier() {
        return militaryMoraleTier;
    }

    public int standingRosterSize() {
        return standingRosterSize;
    }

    public int standingRosterCap() {
        return standingRosterCap;
    }

    public boolean rostered() {
        return rostered;
    }

    public boolean conscriptionEnabled() {
        return conscriptionEnabled;
    }

    public boolean electionOpen() {
        return electionOpen;
    }

    public String electionLabel() {
        return electionLabel;
    }

    public boolean pollingOpen() {
        return pollingOpen;
    }

    public String referendumQuestion() {
        return referendumQuestion;
    }

    public boolean divisionAwaitingVote() {
        return divisionAwaitingVote;
    }

    public boolean wanted() {
        return wanted;
    }

    public boolean resignationToReview() {
        return resignationToReview;
    }

    public String musterStatus() { return musterStatus; }

    public String siegeStatus() { return siegeStatus; }

    public String resignationSummary() {
        return resignationSummary;
    }

    /** The granary's WorldGuard region, or blank when none is sited. */
    public String granaryRegion() {
        return granaryRegion;
    }

    /** WorldGuard subregion used for capital-fall war aims, or blank when none is linked. */
    public String warCapitalRegion() {
        return warCapitalRegion;
    }

    /** Every point sited under {@code topic}; empty when nothing has been sited. */
    public List<SitePoint> sites(RealmHubTopic topic) {
        List<SitePoint> found = sites.get(topic);
        return found == null ? List.of() : found;
    }

    /** Where the reader stands, or null when unknown. */
    public SitePoint viewer() {
        return viewer;
    }

    public GazetteLiveState live() {
        return live;
    }

    /** Fluent builder: everything defaults to "nothing sited, nothing live, no realm". */
    public static final class Builder {

        private boolean member;
        private String kingdomName = "";
        private NobleRank rank;
        private String titleLabel = "Citizen";
        private boolean hasBuildPermit;
        private boolean buildEnforcementActive;
        private boolean warEnabled;
        private boolean oathOfServiceOpened;
        private String militaryMoraleTier = "";
        private int standingRosterSize;
        private int standingRosterCap;
        private boolean rostered;
        private boolean conscriptionEnabled;
        private boolean electionOpen;
        private String electionLabel = "";
        private boolean pollingOpen;
        private String referendumQuestion = "";
        private boolean divisionAwaitingVote;
        private boolean wanted;
        private boolean resignationToReview;
        private String musterStatus = "";
        private String siegeStatus = "";
        private String resignationSummary = "";
        private String granaryRegion = "";
        private String warCapitalRegion = "";
        private final Map<RealmHubTopic, List<SitePoint>> sites = new EnumMap<>(RealmHubTopic.class);
        private SitePoint viewer;
        private GazetteLiveState live = new GazetteLiveState("", "none proclaimed", 0, 0, 0d);

        private Builder() {}

        public Builder member(boolean member) {
            this.member = member;
            return this;
        }

        public Builder kingdomName(String kingdomName) {
            this.kingdomName = kingdomName == null ? "" : kingdomName;
            return this;
        }

        public Builder rank(NobleRank rank) {
            this.rank = rank;
            return this;
        }

        public Builder titleLabel(String titleLabel) {
            this.titleLabel = titleLabel == null || titleLabel.isBlank() ? "Citizen" : titleLabel;
            return this;
        }

        public Builder hasBuildPermit(boolean hasBuildPermit) {
            this.hasBuildPermit = hasBuildPermit;
            return this;
        }

        public Builder buildEnforcementActive(boolean buildEnforcementActive) {
            this.buildEnforcementActive = buildEnforcementActive;
            return this;
        }

        public Builder warEnabled(boolean warEnabled) {
            this.warEnabled = warEnabled;
            return this;
        }

        public Builder oathOfService(boolean opened, String moraleTier) {
            this.oathOfServiceOpened = opened;
            this.militaryMoraleTier = moraleTier == null ? "" : moraleTier;
            return this;
        }

        public Builder standingRoster(int size, int cap, boolean rostered) {
            this.standingRosterSize = Math.max(0, size);
            this.standingRosterCap = Math.max(0, cap);
            this.rostered = rostered;
            return this;
        }

        public Builder conscriptionEnabled(boolean conscriptionEnabled) {
            this.conscriptionEnabled = conscriptionEnabled;
            return this;
        }

        public Builder electionOpen(boolean electionOpen) {
            this.electionOpen = electionOpen;
            return this;
        }

        public Builder musterStatus(String musterStatus) {
            this.musterStatus = musterStatus == null ? "" : musterStatus;
            return this;
        }

        public Builder siegeStatus(String siegeStatus) {
            this.siegeStatus = siegeStatus == null ? "" : siegeStatus;
            return this;
        }

        public Builder electionLabel(String electionLabel) {
            this.electionLabel = electionLabel == null ? "" : electionLabel;
            return this;
        }

        public Builder pollingOpen(boolean pollingOpen) {
            this.pollingOpen = pollingOpen;
            return this;
        }

        public Builder referendumQuestion(String referendumQuestion) {
            this.referendumQuestion = referendumQuestion == null ? "" : referendumQuestion;
            return this;
        }

        public Builder divisionAwaitingVote(boolean divisionAwaitingVote) {
            this.divisionAwaitingVote = divisionAwaitingVote;
            return this;
        }

        public Builder wanted(boolean wanted) {
            this.wanted = wanted;
            return this;
        }

        public Builder resignationToReview(boolean resignationToReview) {
            this.resignationToReview = resignationToReview;
            return this;
        }

        public Builder resignationSummary(String resignationSummary) {
            this.resignationSummary = resignationSummary == null ? "" : resignationSummary;
            return this;
        }

        public Builder granaryRegion(String granaryRegion) {
            this.granaryRegion = granaryRegion == null ? "" : granaryRegion;
            return this;
        }

        public Builder warCapitalRegion(String warCapitalRegion) {
            this.warCapitalRegion = warCapitalRegion == null ? "" : warCapitalRegion;
            return this;
        }

        /** Adds one sited point under {@code topic}; call again for cells, mints and the like. */
        public Builder site(RealmHubTopic topic, SitePoint point) {
            if (point == null) {
                return this;
            }
            sites.computeIfAbsent(topic, key -> new ArrayList<>()).add(point);
            return this;
        }

        public Builder viewer(SitePoint viewer) {
            this.viewer = viewer;
            return this;
        }

        public Builder live(GazetteLiveState live) {
            this.live = live == null ? new GazetteLiveState("", "none proclaimed", 0, 0, 0d) : live;
            return this;
        }

        public RealmHubSnapshot build() {
            return new RealmHubSnapshot(this);
        }
    }
}
