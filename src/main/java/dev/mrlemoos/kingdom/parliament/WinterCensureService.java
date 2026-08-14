package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Parliament's judgement of an ill-timed winter decision: a Premier who takes the realm to war, or
 * sends it to the country, in the dead of winter loses political standing for it and has the
 * grievance entered in Hansard for the realm to read.
 *
 * <p><b>No motion is ever tabled here.</b> A motion of no confidence needs two seated Members to
 * choose it between them, and a calendar check has no business overriding that (ADR 0006). This
 * service only lowers the Premier's own political loyalty and records why.
 */
public final class WinterCensureService {

    /** What the Premier is answering for. */
    public enum Act {

        WAR("Winter war", "taking the realm to war in the dead of winter"),
        DISSOLUTION("Winter dissolution", "sending the realm to the country in the dead of winter");

        private final String title;
        private final String grievance;

        Act(String title, String grievance) {
            this.title = title;
            this.grievance = grievance;
        }

        public String title() {
            return title;
        }

        public String grievance() {
            return grievance;
        }
    }

    /** The business a censure is entered under in Hansard. */
    private static final String BUSINESS = "censure";

    private final KingdomService kingdomService;
    private final LoyaltyService loyaltyService;
    private final WinterCensureConfig config;
    private final Supplier<Season> season;
    private final LongSupplier realmDay;
    private BiConsumer<String, String> announcer = (kingdomId, message) -> {};

    public WinterCensureService(
            KingdomService kingdomService,
            LoyaltyService loyaltyService,
            WinterCensureConfig config,
            Supplier<Season> season,
            LongSupplier realmDay) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
        this.config = Objects.requireNonNull(config, "config");
        this.season = Objects.requireNonNull(season, "season");
        this.realmDay = Objects.requireNonNull(realmDay, "realmDay");
    }

    /** Who tells the realm a censure has been entered. */
    public void setAnnouncer(BiConsumer<String, String> announcer) {
        this.announcer = announcer != null ? announcer : (kingdomId, message) -> {};
    }

    /**
     * Censures {@code kingdomId}'s Premier for {@code act} where the season is winter, returning the
     * Premier censured. Touches nobody in any other season, where the office is vacant, and where a
     * villager holds it — a villager Premier keeps no political loyalty to lose.
     */
    public Optional<UUID> censure(String kingdomId, Act act) {
        Objects.requireNonNull(act, "act");
        if (!config.enabled() || config.tierSteps() <= 0) {
            return Optional.empty();
        }
        if (season.get() != Season.WINTER) {
            return Optional.empty();
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        Optional<UUID> premier = premierOf(kingdom.get().getId());
        if (premier.isEmpty()) {
            return Optional.empty();
        }
        UUID censured = premier.get();
        for (int step = 0; step < config.tierSteps(); step++) {
            loyaltyService.recordActBreach(censured);
        }
        long day = realmDay.getAsLong();
        kingdom.get()
                .getParliamentState()
                .addHansardRecord(new HansardRecord(act.title(), BUSINESS, true, 0, 0, 0, 0, List.of(), day));
        announcer.accept(
                kingdom.get().getId(),
                "The Premier is censured for " + act.grievance()
                        + ". The grievance is entered in Hansard; the House must judge it for itself.");
        return Optional.of(censured);
    }

    private Optional<UUID> premierOf(String kingdomId) {
        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (kingdomId.equals(membership.getKingdomId()) && membership.getRank() == NobleRank.PREMIER) {
                return Optional.of(membership.getPlayerId());
            }
        }
        return Optional.empty();
    }
}
