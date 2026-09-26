package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.foundation.NextFreeNumber;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;

/**
 * Siting and clearing the court with its villager judge, and the numbered cells, whichever road led
 * there — the foundation stone or the operators' command. The rules stay in {@link PoliceService};
 * this seats the judge, moves the court guards and saves.
 */
public final class PoliceSiting {

    private final PoliceService policeService;
    private final PoliceCourtService courtService;
    private final PoliceGolemService golemService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public PoliceSiting(
            PoliceService policeService,
            PoliceCourtService courtService,
            PoliceGolemService golemService,
            KingdomService kingdomService,
            YamlKingdomStore store) {
        this.policeService = policeService;
        this.courtService = courtService;
        this.golemService = golemService;
        this.kingdomService = kingdomService;
        this.store = store;
    }

    /** Sites the court and seats the judge there; a court that moves takes its judge and guards along. */
    public PoliceResult siteCourt(String kingdomId, NobleRank actorRank, boolean operator, CourtLocation court) {
        boolean moving = policeService.hasCourt(kingdomId);
        Optional<CourtLocation> previous = policeService.court(kingdomId);
        if (moving && PoliceAuthority.canConfigureSites(actorRank, operator)) {
            courtService.despawnJudge(kingdomId);
        }
        PoliceResult result = policeService.setCourt(kingdomId, actorRank, operator, court);
        if (!(result instanceof PoliceResult.Success)) {
            return result;
        }
        courtService.ensureJudge(kingdomId);
        if (moving && previous.isPresent()) {
            golemService.relocateCourtGuards(kingdomId, previous.get(), court);
        }
        save();
        return PoliceResult.ok(moving ? "Court moved. Magistrate reseated." : "Court set. Magistrate seated.");
    }

    /** Clears the court; the judge and the court guards go with it. */
    public PoliceResult clearCourt(String kingdomId, NobleRank actorRank, boolean operator) {
        if (!PoliceAuthority.canConfigureSites(actorRank, operator)) {
            return policeService.clearCourt(kingdomId, actorRank, operator);
        }
        Optional<CourtLocation> court = policeService.court(kingdomId);
        courtService.despawnJudge(kingdomId);
        court.ifPresent(location -> golemService.despawnCourtGuards(kingdomId, location));
        PoliceResult result = policeService.clearCourt(kingdomId, actorRank, operator);
        if (result instanceof PoliceResult.Success) {
            save();
        }
        return result;
    }

    /** The cells set, lowest first. */
    public List<Integer> cells(String kingdomId) {
        KingdomPoliceState police = policeService.policeState(kingdomId);
        return police == null ? List.of() : new ArrayList<>(new TreeSet<>(police.cellsView().keySet()));
    }

    /** The cell a cell's stone would set next: the lowest free number. There is no last cell. */
    public OptionalInt nextFreeCell(String kingdomId) {
        return NextFreeNumber.unbounded(new TreeSet<>(cells(kingdomId)));
    }

    public PoliceResult setCell(
            String kingdomId, NobleRank actorRank, boolean operator, int slot, PrisonCellLocation location) {
        return saved(policeService.setCell(kingdomId, actorRank, operator, slot, location));
    }

    /** Sets the next free cell at the location. */
    public PoliceResult fillNextCell(String kingdomId, NobleRank actorRank, PrisonCellLocation location) {
        OptionalInt next = nextFreeCell(kingdomId);
        if (next.isEmpty()) {
            return PoliceResult.fail("No cell number is free.");
        }
        return setCell(kingdomId, actorRank, false, next.getAsInt(), location);
    }

    public PoliceResult clearCell(String kingdomId, NobleRank actorRank, boolean operator, int slot) {
        return saved(policeService.clearCell(kingdomId, actorRank, operator, slot));
    }

    public Optional<CourtLocation> court(String kingdomId) {
        return policeService.court(kingdomId);
    }

    public Optional<PrisonCellLocation> cell(String kingdomId, int slot) {
        return policeService.cell(kingdomId, slot);
    }

    private PoliceResult saved(PoliceResult result) {
        if (result instanceof PoliceResult.Success) {
            save();
        }
        return result;
    }

    private void save() {
        if (store != null) {
            store.saveFrom(kingdomService);
        }
    }
}
