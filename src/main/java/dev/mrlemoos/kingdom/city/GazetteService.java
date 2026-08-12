package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.city.GazettePost.GazetteCurfewWindow;
import dev.mrlemoos.kingdom.model.city.GazettePostKind;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Crown authorship of Gazette posts: announcements (capped, quiet) and decrees (Hansard, broadcast,
 * optional curfew).
 */
public final class GazetteService {

    private final KingdomService kingdomService;

    public GazetteService(KingdomService kingdomService) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
    }

    public CityResult publishAnnouncement(
            String kingdomId, UUID authorId, String title, String body, long mcDay) {
        return publish(kingdomId, authorId, title, body, mcDay, GazettePostKind.ANNOUNCEMENT, Optional.empty());
    }

    /**
     * @param curfewChoice empty = decree does not change the kingdom's curfew; present = set that
     *     window (including {@link CurfewPresets#lifted()})
     */
    public CityResult publishDecree(
            String kingdomId,
            UUID authorId,
            String title,
            String body,
            long mcDay,
            Optional<CurfewEnforcementConfig> curfewChoice) {
        return publish(kingdomId, authorId, title, body, mcDay, GazettePostKind.DECREE, curfewChoice);
    }

    private CityResult publish(
            String kingdomId,
            UUID authorId,
            String title,
            String body,
            long mcDay,
            GazettePostKind kind,
            Optional<CurfewEnforcementConfig> curfewChoice) {
        if (kingdomId == null || kingdomId.isBlank()) {
            return CityResult.fail("Unknown kingdom.");
        }
        if (authorId == null) {
            return CityResult.fail("An author is required.");
        }
        String headline = title == null ? "" : title.trim();
        String text = body == null ? "" : body.trim();
        if (headline.isEmpty()) {
            return CityResult.fail("The book needs a title for the headline.");
        }
        if (text.isEmpty()) {
            return CityResult.fail("The book needs pages for the body.");
        }

        Optional<Kingdom> kingdomOpt = kingdomService.getKingdom(kingdomId);
        if (kingdomOpt.isEmpty()) {
            return CityResult.fail("Unknown kingdom.");
        }
        Kingdom kingdom = kingdomOpt.get();
        KingdomCityState city = kingdom.getCityState();
        if (!city.hasCapital()) {
            return CityResult.fail("There is no capital; the Gazette has nowhere to hang.");
        }

        Optional<PlayerMembership> membership = kingdomService.getMembership(authorId);
        if (membership.isEmpty() || !kingdomId.equals(membership.get().getKingdomId())) {
            return CityResult.fail("Only the Crown of this kingdom may publish to the Gazette.");
        }
        NobleRank rank = membership.get().getRank();
        if (rank == null || !CapitalSitingPolicy.isCrown(rank)) {
            return CityResult.fail("Only the King or Queen may publish to the Gazette.");
        }

        Optional<CurfewEnforcementConfig> curfew = curfewChoice == null ? Optional.empty() : curfewChoice;
        Optional<GazetteCurfewWindow> postCurfew = Optional.empty();
        if (kind == GazettePostKind.DECREE && curfew.isPresent()) {
            CurfewEnforcementConfig chosen = curfew.get();
            if (chosen.enabled()) {
                postCurfew = Optional.of(new GazetteCurfewWindow(
                        chosen.windowStartTick(), chosen.windowEndTick()));
            }
        }

        city.addGazettePost(new GazettePost(headline, text, authorId, mcDay, kind, postCurfew));

        if (kind == GazettePostKind.DECREE) {
            kingdom.getParliamentState().addHansardRecord(new HansardRecord(
                    headline,
                    "decree",
                    true,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    mcDay));
            if (curfew.isPresent()) {
                city.setDecreeCurfew(curfew.get());
            }
            RealmFeedback.royalAssent(kingdomService, kingdomId);
            RealmFeedback.kingdomMessage(
                    kingdomService,
                    kingdomId,
                    "&6DECREE &7— &f" + headline);
            return CityResult.ok("Decree published to the Gazette.");
        }
        return CityResult.ok("Announcement published to the Gazette.");
    }
}
