package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.RealmDate;
import dev.mrlemoos.kingdom.calendar.RegnalDating;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import org.bukkit.Bukkit;

/**
 * Turns the page of the realm calendar: advances the clock, proclaims the New Year and each kingdom's accession
 * anniversary, and persists the day so the record survives a restart.
 */
public final class RealmCalendarTask implements Runnable {

    private final KingdomService kingdomService;
    private final RealmCalendarService calendarService;
    private final YamlKingdomStore store;
    private long lastProclaimedDay = -1L;

    public RealmCalendarTask(
            KingdomService kingdomService, RealmCalendarService calendarService, YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.calendarService = calendarService;
        this.store = store;
    }

    @Override
    public void run() {
        long realmDay = calendarService.currentRealmDay();
        if (realmDay == lastProclaimedDay) {
            return;
        }
        boolean firstSweep = lastProclaimedDay < 0L;
        lastProclaimedDay = realmDay;
        if (firstSweep) {
            return;
        }

        RealmDate date = dev.mrlemoos.kingdom.calendar.RealmCalendar.dateOf(realmDay);
        if (date.isNewYear()) {
            Bukkit.broadcastMessage(c(
                    "&6The realm turns the page: " + date.format() + ", Realm Year " + date.realmYear() + "."));
        }
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            calendarService.currentReign(kingdom.getId())
                    .filter(reign -> RegnalDating.isAccessionAnniversary(reign, realmDay))
                    .ifPresent(reign -> {
                        int year = RegnalDating.regnalYear(reign, realmDay);
                        Bukkit.broadcastMessage(c("&6" + kingdom.getDisplayName() + " enters the "
                                + year + dev.mrlemoos.kingdom.calendar.RealmCalendar.ordinalSuffix(year)
                                + " year of the reign of " + reign.styledName() + "."));
                    });
        }
        store.saveFrom(kingdomService);
    }
}
