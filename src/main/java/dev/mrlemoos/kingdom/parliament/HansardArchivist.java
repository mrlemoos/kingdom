package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Binds the record of a Parliament at prorogation and shelves it in the registrar: one volume where
 * the session's business fits in one, and volume II, III and onwards where it does not.
 */
public final class HansardArchivist {

    private final KingdomService kingdomService;
    private dev.mrlemoos.kingdom.calendar.RealmCalendarService calendarService;

    public HansardArchivist(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public void setCalendarService(dev.mrlemoos.kingdom.calendar.RealmCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** Shelves the closing session's Hansard. Silent where the kingdom has set no registrar. */
    public void archive(String kingdomId, List<HansardRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return;
        }
        Optional<RegistrarSite> registrar = kingdom.get().getParliamentSites().registrar();
        if (registrar.isEmpty()) {
            return;
        }
        List<HansardVolume> volumes = calendarService == null
                ? HansardBook.render(kingdom.get().getDisplayName(), records)
                : HansardBook.render(
                        kingdom.get().getDisplayName(),
                        records,
                        mcDay -> calendarService.stampForMcDay(kingdomId, mcDay));
        for (HansardVolume volume : volumes) {
            try {
                RegistrarShelfWriter.placeBook(
                        registrar.get(), volume.title(), volume.pages(), existingShelfSites(kingdom.get()));
            } catch (IllegalStateException failed) {
                return;
            }
        }
    }

    private static List<RegistrarSite> existingShelfSites(Kingdom kingdom) {
        List<RegistrarSite> shelves = new ArrayList<>();
        for (AssentedAct act : kingdom.getParliamentState().assentedActsView()) {
            shelves.add(RegistrarSite.of(act.shelfWorld(), act.shelfBlockX(), act.shelfBlockY(), act.shelfBlockZ()));
        }
        return shelves;
    }
}
