package dev.mrlemoos.kingdom.calendar;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * The realm's clock and calendar: one server-wide realm day, and a regnal era per kingdom.
 *
 * <p>The realm day is derived from the world clock, so it never drifts; the epoch is pinned once, on first run.
 */
public final class RealmCalendarService {

    private final KingdomService kingdomService;
    private final LongSupplier mcDayClock;
    private RealmClock clock;

    public RealmCalendarService(KingdomService kingdomService, LongSupplier mcDayClock) {
        this.kingdomService = kingdomService;
        this.mcDayClock = mcDayClock;
    }

    /** Pins the epoch on first run, or restores the stored one. */
    public void restore(long epochWorldDay, long lastSeenRealmDay) {
        this.clock = new RealmClock(epochWorldDay < 0L ? mcDayClock.getAsLong() : epochWorldDay, lastSeenRealmDay);
    }

    public long epochWorldDay() {
        return clock().epochWorldDay();
    }

    public long currentRealmDay() {
        return clock().advanceTo(mcDayClock.getAsLong());
    }

    public RealmDate today() {
        return RealmCalendar.dateOf(currentRealmDay());
    }

    /** e.g. {@code 12th of Harvest, Year 3 of King Leo II (Realm Year 48)}. */
    public String formatFor(String kingdomId) {
        return RegnalDating.format(reignsOf(kingdomId), currentRealmDay());
    }

    /** Dates a past event recorded against a raw in-game day, by the reign in force in that kingdom today. */
    public String stampForMcDay(String kingdomId, long mcDay) {
        long realmDay = Math.max(0L, mcDay - epochWorldDay());
        return RegnalDating.format(reignsOf(kingdomId), realmDay);
    }

    public Optional<ReignRecord> currentReign(String kingdomId) {
        return RegnalDating.currentReign(reignsOf(kingdomId));
    }

    /** Brings every kingdom's reign record in line with who actually wears the Crown. */
    public void reconcileAllReigns() {
        long realmDay = currentRealmDay();
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            reconcileReign(kingdom, realmDay);
        }
    }

    public void reconcileReign(String kingdomId) {
        kingdomService.getKingdom(kingdomId).ifPresent(kingdom -> reconcileReign(kingdom, currentRealmDay()));
    }

    private void reconcileReign(Kingdom kingdom, long realmDay) {
        KingdomReignHistory history = kingdom.getReignHistory();
        Optional<PlayerMembership> monarch = kingdomService.findMonarch(kingdom.getId());
        if (monarch.isEmpty()) {
            history.closeOpenReign(realmDay);
            return;
        }
        PlayerMembership seated = monarch.get();
        history.openReign(
                seated.getPlayerId().toString(),
                nameOf(seated.getPlayerId()),
                seated.getRank().displayTitle(seated.getTitleStyle()),
                realmDay);
    }

    private java.util.List<ReignRecord> reignsOf(String kingdomId) {
        return kingdomService.getKingdom(kingdomId)
                .map(kingdom -> kingdom.getReignHistory().view())
                .orElseGet(java.util.List::of);
    }

    private static String nameOf(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        String name = player.getName();
        return name != null ? name : playerId.toString().substring(0, 8);
    }

    private RealmClock clock() {
        if (clock == null) {
            restore(-1L, 0L);
        }
        return clock;
    }

    /** The rank a reign is dated by; the Crown alone. */
    public static boolean isCrown(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }
}
