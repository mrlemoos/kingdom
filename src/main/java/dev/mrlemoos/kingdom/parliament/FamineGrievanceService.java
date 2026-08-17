package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.granary.FamineWatch;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * What the realm answers for when its villagers starve: every subject's political loyalty falls a
 * step, the grievance is entered in Hansard, and the realm is told — once for the famine, however
 * long it runs.
 *
 * <p><b>No motion is ever tabled here</b>, as none is for a winter censure (ADR 0006, ADR 0007). A
 * motion of no confidence needs two seated Members to choose it between them; an empty granary only
 * gives them the grievance to choose it over.
 */
public final class FamineGrievanceService {

    /** The business a famine is entered under in Hansard. */
    private static final String BUSINESS = "grievance";

    private static final String TITLE = "Famine";

    private final KingdomService kingdomService;
    private final LoyaltyService loyaltyService;
    private final GranaryConfig config;
    private final FamineWatch watch;
    private BiConsumer<String, String> announcer = (kingdomId, message) -> {};

    public FamineGrievanceService(
            KingdomService kingdomService,
            LoyaltyService loyaltyService,
            GranaryConfig config,
            FamineWatch watch) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
        this.config = Objects.requireNonNull(config, "config");
        this.watch = Objects.requireNonNull(watch, "watch");
    }

    /** Who tells the realm a famine has been entered against it. */
    public void setAnnouncer(BiConsumer<String, String> announcer) {
        this.announcer = announcer != null ? announcer : (kingdomId, message) -> {};
    }

    /**
     * Enters the grievance against {@code kingdomId} where its villagers are starving, returning
     * whether this is the day the realm answered for it. A realm whose villagers are fed answers for
     * nothing, and has its slate wiped so a famine come again is answered for afresh.
     */
    public boolean enterGrievance(String kingdomId, boolean starving, long realmDay, long mcDay) {
        if (kingdomId == null) {
            return false;
        }
        if (!starving) {
            watch.relieve(kingdomId);
            return false;
        }
        if (config.famineTierSteps() <= 0) {
            return false;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return false;
        }
        if (!watch.claim(kingdom.get().getId(), realmDay)) {
            return false;
        }

        for (PlayerMembership membership : kingdomService.getMembershipsView().values()) {
            if (!kingdom.get().getId().equals(membership.getKingdomId())) {
                continue;
            }
            for (int step = 0; step < config.famineTierSteps(); step++) {
                loyaltyService.recordActBreach(membership.getPlayerId());
            }
        }

        kingdom.get()
                .getParliamentState()
                .addHansardRecord(new HansardRecord(TITLE, BUSINESS, false, 0, 0, 0, 0, List.of(), mcDay));
        announcer.accept(
                kingdom.get().getId(),
                "The realm is starving for want of grain. The grievance is entered in Hansard; "
                        + "the House must judge it for itself.");
        return true;
    }
}
