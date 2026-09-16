package dev.mrlemoos.kingdom.city.gui;

import dev.mrlemoos.kingdom.calendar.PollingDay;
import dev.mrlemoos.kingdom.calendar.RealmCalendar;
import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.treaty.TreatyService;
import dev.mrlemoos.kingdom.treaty.TreatyState;
import java.util.Locale;
import java.util.Optional;

/**
 * The one reader of the realm's live state: the open bill, the next polling day, the wanted, the
 * permits and the treasury. Read fresh each time a board is opened and never persisted. The Gazette
 * and the Realm Hub both ask this, so the two never disagree.
 */
public final class GazetteLiveStateReader {

    private final EconomyService economyService;
    private final MechanicalJusticeService justiceService;
    private final RealmCalendarService calendarService;
    private final PollingDay pollingDay;
    private TreatyService treatyService;
    private KingdomService kingdomService;

    public GazetteLiveStateReader(
            EconomyService economyService,
            MechanicalJusticeService justiceService,
            RealmCalendarService calendarService,
            PollingDay pollingDay) {
        this.economyService = economyService;
        this.justiceService = justiceService;
        this.calendarService = calendarService;
        this.pollingDay = pollingDay;
    }

    public GazetteLiveStateReader withTreatyService(TreatyService treatyService) {
        this.treatyService = treatyService;
        return this;
    }

    public GazetteLiveStateReader withKingdomService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
        return this;
    }

    public GazetteLiveState read(Kingdom kingdom) {
        String openBill = "";
        Optional<Bill> bill = kingdom.getParliamentState().currentBill();
        if (bill.isPresent()) {
            openBill = bill.get().title();
        }
        double treasury = economyService == null ? 0d : economyService.getTreasuryBalance(kingdom.getId());
        return new GazetteLiveState(
                openBill,
                nextElectionLabel(kingdom),
                wantedCount(kingdom),
                kingdom.getCityState().permitCount(),
                treasury,
                treatyLine(kingdom));
    }

    private String treatyLine(Kingdom kingdom) {
        if (treatyService == null) return "";
        for (TreatyState treaty : treatyService.treatiesView()) {
            String counterpart = counterpart(kingdom.getId(), treaty);
            if (counterpart == null) continue;
            String kind = treaty.kind() == dev.mrlemoos.kingdom.model.parliament.TreatyKind.TRADE_PACT
                    ? "trade pact" : "non-aggression treaty";
            String counterpartName = displayName(counterpart);
            if (treaty.active()) return "Treaty active: " + kind + " with " + counterpartName;
            return "Treaty awaiting Crown: " + kind + " with " + counterpartName;
        }
        return "";
    }

    private static String counterpart(String kingdomId, TreatyState treaty) {
        if (kingdomId.equals(treaty.firstKingdomId())) return treaty.secondKingdomId();
        if (kingdomId.equals(treaty.secondKingdomId())) return treaty.firstKingdomId();
        return null;
    }

    private String displayName(String kingdomId) {
        if (kingdomService == null) return kingdomId;
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        return kingdom.isPresent() ? kingdom.get().getDisplayName() : kingdomId;
    }

    private int wantedCount(Kingdom kingdom) {
        if (justiceService == null) {
            return 0;
        }
        int wanted = 0;
        for (Warrant warrant : justiceService.warrantsView()) {
            if (!kingdom.getId().equals(warrant.kingdomId())) {
                continue;
            }
            WarrantStatus status = warrant.status();
            if (status == WarrantStatus.ACTIVE || status == WarrantStatus.PENDING_CROWN) {
                wanted++;
            }
        }
        return wanted;
    }

    /** The day the realm next goes to the polls, or the phase of the election already running. */
    public String nextElectionLabel(Kingdom kingdom) {
        var election = kingdom.getElectionState().election();
        if (election.isActive()) {
            return "election in progress (" + election.phase().name().toLowerCase(Locale.UK) + ")";
        }
        if (calendarService == null || pollingDay == null) {
            return "none proclaimed";
        }
        long realmDay = calendarService.currentRealmDay();
        var today = RealmCalendar.dateOf(realmDay);
        long thisYear = pollingDay.dayInYear(today.realmYear());
        long next = realmDay <= thisYear ? thisYear : pollingDay.dayInYear(today.realmYear() + 1);
        var date = RealmCalendar.dateOf(next);
        return date.format() + ", Realm Year " + date.realmYear();
    }
}
