package dev.mrlemoos.kingdom.treaty;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.TreatyKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** One realm's view of its treaties, and what its Crown may still table about each (no Bukkit). */
public final class TreatyRegister {

    public enum Status {
        ACTIVE,
        AWAITING_US,
        AWAITING_THEM,
        REPEAL_AWAITING_US,
        REPEAL_AWAITING_THEM
    }

    /** {@code daysLeft} counts to the proposal or repeal lapsing; zero for a settled active treaty. */
    public record Row(String counterpartId, TreatyKind kind, Status status, long daysLeft) {

        public boolean crownMayAnswer() {
            return status == Status.ACTIVE || status == Status.AWAITING_US || status == Status.REPEAL_AWAITING_US;
        }

        public boolean answerIsRepeal() {
            return status == Status.ACTIVE || status == Status.REPEAL_AWAITING_US;
        }
    }

    private TreatyRegister() {}

    public static List<Row> rows(String kingdomId, Collection<TreatyState> treaties, long mcDay) {
        String self = Kingdom.normaliseId(kingdomId);
        List<Row> rows = new ArrayList<>();
        for (TreatyState treaty : treaties) {
            String counterpart;
            if (self.equals(treaty.firstKingdomId())) {
                counterpart = treaty.secondKingdomId();
            } else if (self.equals(treaty.secondKingdomId())) {
                counterpart = treaty.firstKingdomId();
            } else {
                continue;
            }
            if (!treaty.active()) {
                // The day roll expires these; until then a lapsed proposal is not business.
                if (treaty.expiresOnMcDay() <= mcDay) continue;
                Status status = treaty.assentedBy().contains(self) ? Status.AWAITING_THEM : Status.AWAITING_US;
                rows.add(new Row(counterpart, treaty.kind(), status, treaty.expiresOnMcDay() - mcDay));
            } else if (treaty.repealExpiresOnMcDay() > mcDay && !treaty.repealAssentedBy().isEmpty()) {
                Status status = treaty.repealAssentedBy().contains(self)
                        ? Status.REPEAL_AWAITING_THEM : Status.REPEAL_AWAITING_US;
                rows.add(new Row(counterpart, treaty.kind(), status, treaty.repealExpiresOnMcDay() - mcDay));
            } else {
                rows.add(new Row(counterpart, treaty.kind(), Status.ACTIVE, 0));
            }
        }
        rows.sort(Comparator.comparing(Row::counterpartId).thenComparing(Row::kind));
        return rows;
    }
}
