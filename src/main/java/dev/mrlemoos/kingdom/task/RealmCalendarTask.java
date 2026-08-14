package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.RealmDate;
import dev.mrlemoos.kingdom.calendar.RegnalDating;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.bukkit.Bukkit;

/**
 * Turns the page of the realm calendar: advances the clock, proclaims the New Year and each kingdom's accession
 * anniversary, ticks the loyalty and morale recovery clocks for the day, and persists the day so the record
 * survives a restart.
 */
public final class RealmCalendarTask implements Runnable {

    private final KingdomService kingdomService;
    private final RealmCalendarService calendarService;
    private final YamlKingdomStore store;
    private long lastProclaimedDay = -1L;
    private LoyaltyService loyaltyService;
    private MoraleService moraleService;

    public RealmCalendarTask(
            KingdomService kingdomService, RealmCalendarService calendarService, YamlKingdomStore store) {
        this.kingdomService = kingdomService;
        this.calendarService = calendarService;
        this.store = store;
    }

    /**
     * Optional hooks (nullable setters, mirroring the war domain) so the day roll also advances
     * loyalty and morale recovery. Without them the tracks still record offences; nothing recovers.
     */
    public void setRecoveryServices(LoyaltyService loyaltyService, MoraleService moraleService) {
        this.loyaltyService = loyaltyService;
        this.moraleService = moraleService;
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
        tickRecoveryClocks(realmDay);
        store.saveFrom(kingdomService);
    }

    /**
     * One recovery tick per realm day for every subject with a tier on record. Faithful subjects
     * hold no entry on the political track, so only those actually recovering are visited.
     */
    private void tickRecoveryClocks(long realmDay) {
        if (loyaltyService != null) {
            for (UUID playerId : new LinkedHashSet<>(loyaltyService.store().allTiersView().keySet())) {
                loyaltyService.tickRecovery(playerId, realmDay);
            }
        }
        if (moraleService != null) {
            for (UUID playerId : new LinkedHashSet<>(moraleService.store().allTiersView().keySet())) {
                moraleService.tickRecovery(playerId, realmDay);
            }
        }
    }
}
