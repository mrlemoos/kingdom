package dev.mrlemoos.kingdom.feedback;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * The bar that hangs over a kingdom while its House divides: the bill, the ayes and the noes as they
 * are cast, and how much of the division window is left to cast them in.
 *
 * <p>It keeps no schedule of its own — the existing election sweep drives it — and it holds no state
 * beyond one bar per kingdom, rebuilt from the bill before the House on every sweep. Its audience is
 * re-seated each sweep too, so players joining and leaving need no listener of their own. Commons
 * divisions hang only over the Crown, the Premier, the Speaker and the MPs; a referendum still hangs over the
 * whole realm.
 */
public final class DivisionBossBarService {

    private final KingdomService kingdomService;
    private final Map<String, BossBar> bars = new ConcurrentHashMap<>();

    public DivisionBossBarService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    /**
     * Brings the bar into line with the business before the House: raised when a division opens,
     * retitled as votes are cast, and taken down the moment the division closes.
     */
    public void sync(String kingdomId, Optional<Bill> currentBill, long currentMcDay, int windowMcDays) {
        if (kingdomId == null || currentBill == null) {
            return;
        }
        if (currentBill.isEmpty() || currentBill.get().state() != BillState.DIVISION_OPEN) {
            clear(kingdomId);
            return;
        }
        Bill bill = currentBill.get();
        int aye = count(bill, VoteChoice.AYE);
        int nay = count(bill, VoteChoice.NAY);
        BossBar bar = bars.get(kingdomId);
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    c("&e" + DivisionBarText.label(bill.title(), aye, nay)),
                    BarColor.YELLOW,
                    BarStyle.SEGMENTED_10);
            bars.put(kingdomId, bar);
            RealmFeedback.divisionOpened(kingdomService, kingdomId);
        } else {
            bar.setTitle(c("&e" + DivisionBarText.label(bill.title(), aye, nay)));
        }
        bar.setProgress(DivisionBarText.progress(
                currentMcDay, bill.divisionClosesOnMcDay().orElse(-1L), windowMcDays));
        seat(bar, kingdomId, bill.type() != BillType.REFERENDUM);
        bar.setVisible(true);
    }

    /** Takes the kingdom's bar down, if one hangs. */
    public void clear(String kingdomId) {
        BossBar bar = bars.remove(kingdomId);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    /** Takes every bar down; called as the plugin shuts, so no bar is left hanging. */
    public void clearAll() {
        for (String kingdomId : Map.copyOf(bars).keySet()) {
            clear(kingdomId);
        }
    }

    /**
     * Re-seats the bar. A Commons division is shown only to the Crown, Premier, Speaker and MPs; a
     * referendum still hangs over the whole realm.
     */
    private void seat(BossBar bar, String kingdomId, boolean houseOnly) {
        bar.removeAll();
        for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
            if (!houseOnly || houseMember(member)) {
                bar.addPlayer(member);
            }
        }
    }

    private boolean houseMember(Player member) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(member.getUniqueId());
        return membership.isPresent() && HouseBarAudience.sees(membership.get().getRank());
    }

    private static int count(Bill bill, VoteChoice choice) {
        int counted = 0;
        for (VoteChoice cast : bill.votesView().values()) {
            if (cast == choice) {
                counted++;
            }
        }
        return counted;
    }
}
