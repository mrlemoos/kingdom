package dev.mrlemoos.kingdom.foundation;

import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import dev.mrlemoos.kingdom.mint.RoyalMintPlacementPolicy;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import java.util.Optional;

/**
 * The kind of a foundation stone: which of the realm's sites laying it raises. The Realm Hub hands
 * the Crown one of these; placing it inside the kingdom's own territory is the act of siting.
 */
public enum FoundationStone {

    CAPITAL("The Capital", "the capital"),
    TOWN_CRIER("The Town Crier", "the Town Crier"),
    MINT("A Royal Mint", "a mint"),
    CHURCH("The Church", "the church"),
    COMMONS("The House of Commons", "the House of Commons"),
    LORDS("The House of Lords", "the House of Lords"),
    SPEAKER_CHAIR("The Speaker's Chair", "the Speaker's Chair"),
    BAR("The Bar", "the bar"),
    MP_SEAT("An MP Seat", "an MP seat"),
    REGISTRAR("The Registrar", "the registrar"),
    COURT("The Court", "the court"),
    CELL("A Cell", "a cell"),
    GRANARY("The Granary", "the granary");

    private final String title;
    private final String site;

    FoundationStone(String title, String site) {
        this.title = title;
        this.site = site;
    }

    /** The site's name as a heading: {@code The Church}. */
    public String title() {
        return title;
    }

    /** The site's name mid-sentence: {@code the church}. */
    public String site() {
        return site;
    }

    /** Who may lay this stone: whoever may site the place today. Mints are delegated to Lords. */
    public boolean mayLay(NobleRank rank) {
        return this == MINT ? RoyalMintPlacementPolicy.canPlace(rank) : RankAuthority.canSiteCapital(rank);
    }

    /** True for stones that are the site's own block and stay where laid: the registrar's and the court's. */
    public boolean staysWhereLaid() {
        return this == REGISTRAR || this == COURT;
    }

    /** Who may clear the site from the Hub: the Crown alone, for every place. */
    public boolean mayClear(NobleRank rank) {
        return RankAuthority.canSiteCapital(rank);
    }

    /** Those who may lay the stone, mid-sentence: {@code the King or Queen}. */
    public String layers() {
        return this == MINT ? "the King, Queen or a Lord" : "the King or Queen";
    }

    /** The stone the Hub hands out for a place; empty for places not yet laid by stone. */
    public static Optional<FoundationStone> forTopic(RealmHubTopic topic) {
        return switch (topic) {
            case PLACE_CHURCH -> Optional.of(CHURCH);
            case PLACE_CAPITAL -> Optional.of(CAPITAL);
            case PLACE_TOWN_CRIER -> Optional.of(TOWN_CRIER);
            case PLACE_MINTS -> Optional.of(MINT);
            case PLACE_COMMONS -> Optional.of(COMMONS);
            case PLACE_LORDS -> Optional.of(LORDS);
            case PLACE_SPEAKER_CHAIR -> Optional.of(SPEAKER_CHAIR);
            case PLACE_BAR -> Optional.of(BAR);
            case PLACE_MP_SEATS -> Optional.of(MP_SEAT);
            case PLACE_REGISTRAR -> Optional.of(REGISTRAR);
            case PLACE_COURT -> Optional.of(COURT);
            case PLACE_PRISON -> Optional.of(CELL);
            case PLACE_GRANARY -> Optional.of(GRANARY);
            default -> Optional.empty();
        };
    }

    /** The Hub place that hands out this stone; empty while it is not yet cut. */
    public Optional<RealmHubTopic> topic() {
        for (RealmHubTopic topic : RealmHubTopic.values()) {
            Optional<FoundationStone> kind = forTopic(topic);
            if (kind.isPresent() && kind.get() == this) {
                return Optional.of(topic);
            }
        }
        return Optional.empty();
    }
}
