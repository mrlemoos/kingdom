package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.model.parliament.ParliamentSites;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;

/**
 * Setting and clearing Parliament's points — the chambers, the Speaker's Chair, the bar, the MP seats
 * and the registrar — whichever road led there: the foundation stone or the operators' command. The
 * Lords carries the kingdom flag with it.
 */
public final class ParliamentSiting {

    private final ParliamentService parliamentService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final RoyalStandardPlacer royalStandardPlacer;

    /** @param royalStandardPlacer who raises the kingdom flag; null where no flag is flown */
    public ParliamentSiting(
            ParliamentService parliamentService,
            KingdomService kingdomService,
            YamlKingdomStore store,
            RoyalStandardPlacer royalStandardPlacer) {
        this.parliamentService = parliamentService;
        this.kingdomService = kingdomService;
        this.store = store;
        this.royalStandardPlacer = royalStandardPlacer;
    }

    public ParliamentResult setCommons(String kingdomId, ChamberSite site) {
        return saved(parliamentService.setCommons(kingdomId, site));
    }

    public ParliamentResult setSpeakerChair(String kingdomId, ChamberSite site) {
        return saved(parliamentService.setSpeakerChair(kingdomId, site));
    }

    public ParliamentResult setBar(String kingdomId, ChamberSite site) {
        return saved(parliamentService.setBar(kingdomId, site));
    }

    public ParliamentResult setRegistrar(String kingdomId, RegistrarSite site) {
        return saved(parliamentService.setRegistrar(kingdomId, site));
    }

    public ParliamentResult setMpSeat(String kingdomId, int seat, MpSeatLocation location) {
        return saved(parliamentService.setMpSeat(kingdomId, seat, location));
    }

    /**
     * Sets the House of Lords and flies the kingdom flag beside it: the design given, else the one
     * already stored, else Crown gold. A banner still standing at the old Lords is taken down.
     */
    public Lords setLords(String kingdomId, ChamberSite site, Optional<KingdomFlag> design) {
        Optional<Kingdom> before = kingdomService.getKingdom(kingdomId);
        Optional<ChamberSite> previous =
                before.isPresent() ? before.get().getParliamentSites().lords() : Optional.empty();
        ParliamentResult result = parliamentService.setLords(kingdomId, site);
        if (!(result instanceof ParliamentResult.Success)) {
            return new Lords(result, false);
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isPresent()) {
            kingdom.get().setFlag(KingdomFlagResolver.resolve(kingdom.get().getFlag(), design));
        }
        store.saveFrom(kingdomService);
        boolean flies = royalStandardPlacer != null && royalStandardPlacer.moveAndRaise(kingdomId, previous);
        return new Lords(result, flies);
    }

    /** What came of setting the Lords: the House's verdict and whether the kingdom flag now flies. */
    public record Lords(ParliamentResult result, boolean flagFlies) {}

    /** The seats already set, lowest first. */
    public List<Integer> setSeats(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new TreeSet<>(kingdom.get().getElectionState().seatLocationsView().keySet()));
    }

    /** The seat a seat's stone would set next; empty when all eight are set. */
    public OptionalInt nextEmptySeat(String kingdomId) {
        return MpSeatNumbering.nextEmpty(new TreeSet<>(setSeats(kingdomId)));
    }

    /** Sets the next empty seat at the location; refused when the House is full. */
    public ParliamentResult fillNextSeat(String kingdomId, MpSeatLocation location) {
        OptionalInt next = nextEmptySeat(kingdomId);
        if (next.isEmpty()) {
            return ParliamentResult.fail(fullHouse());
        }
        return setMpSeat(kingdomId, next.getAsInt(), location);
    }

    /** Why no seat stone is cut or laid while every seat is set. */
    public static String fullHouse() {
        return "All " + MpSeatNumbering.SEATS + " MP seats are set. Clear one from the Hub first.";
    }

    /** Forgets where a seat stands. */
    public ParliamentResult clearMpSeat(String kingdomId, int seat) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (!kingdom.get().getElectionState().clearSeatLocation(seat)) {
            return ParliamentResult.fail("MP seat " + seat + " is not set.");
        }
        return saved(ParliamentResult.ok("MP seat " + seat + " has been cleared."));
    }

    /** Where a single-point site of Parliament stands, if it is set. */
    public Optional<Where> where(String kingdomId, FoundationStone kind) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return Optional.empty();
        }
        ParliamentSites sites = kingdom.get().getParliamentSites();
        Optional<ChamberSite> chamber = switch (kind) {
            case COMMONS -> sites.commons();
            case LORDS -> sites.lords();
            case SPEAKER_CHAIR -> sites.speakerChair();
            case BAR -> sites.bar();
            default -> Optional.empty();
        };
        if (chamber.isPresent()) {
            ChamberSite site = chamber.get();
            return Optional.of(new Where(site.worldName(), site.x(), site.y(), site.z()));
        }
        if (kind == FoundationStone.REGISTRAR) {
            Optional<RegistrarSite> registrar = sites.registrar();
            if (registrar.isPresent()) {
                RegistrarSite site = registrar.get();
                return Optional.of(new Where(site.worldName(), site.blockX() + 0.5, site.blockY(), site.blockZ() + 0.5));
            }
        }
        return Optional.empty();
    }

    /** A point of Parliament in the world. */
    public record Where(String worldName, double x, double y, double z) {}

    /**
     * Clears a single-point site: the chambers, the chair, the bar or the registrar. The Lords takes the
     * kingdom flag down with it; the registrar's bookshelf and its books stay where they stand.
     */
    public ParliamentResult clear(String kingdomId, FoundationStone kind) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return ParliamentResult.fail("Unknown kingdom.");
        }
        if (where(kingdomId, kind).isEmpty()) {
            return ParliamentResult.fail(capitalise(kind.site()) + " is not yet sited.");
        }
        ParliamentSites sites = kingdom.get().getParliamentSites();
        switch (kind) {
            case COMMONS -> sites.setCommons(null);
            case LORDS -> {
                Optional<ChamberSite> lords = sites.lords();
                if (lords.isPresent() && royalStandardPlacer != null) {
                    royalStandardPlacer.clearAt(lords.get());
                }
                sites.setLords(null);
            }
            case SPEAKER_CHAIR -> sites.setSpeakerChair(null);
            case BAR -> sites.setBar(null);
            case REGISTRAR -> sites.setRegistrar(null);
            default -> {
                return ParliamentResult.fail(capitalise(kind.site()) + " is not a point of Parliament.");
            }
        }
        return saved(ParliamentResult.ok(capitalise(kind.site()) + " has been cleared."));
    }

    private ParliamentResult saved(ParliamentResult result) {
        if (result instanceof ParliamentResult.Success) {
            store.saveFrom(kingdomService);
        }
        return result;
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
