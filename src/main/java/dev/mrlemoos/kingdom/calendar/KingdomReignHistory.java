package dev.mrlemoos.kingdom.calendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The roll of monarchs of one kingdom, oldest first, with at most one reign left open. */
public final class KingdomReignHistory {

    private final List<ReignRecord> reigns = new ArrayList<>();

    public List<ReignRecord> view() {
        return List.copyOf(reigns);
    }

    public void replaceAll(List<ReignRecord> loaded) {
        reigns.clear();
        if (loaded != null) {
            reigns.addAll(loaded);
        }
    }

    /** Opens a reign on the given day, closing whatever reign preceded it. A monarch already reigning is left alone. */
    public void openReign(String monarchId, String monarchName, String title, long accessionDay) {
        Optional<ReignRecord> open = RegnalDating.currentReign(reigns);
        if (open.isPresent() && open.get().monarchId().equals(monarchId)) {
            return;
        }
        closeOpenReign(accessionDay);
        reigns.add(new ReignRecord(
                monarchId, monarchName, title, RegnalDating.nextOrdinal(reigns, monarchName), accessionDay, ReignRecord.OPEN));
    }

    /** Closes the open reign, if any. Returns true when a reign ended. */
    public boolean closeOpenReign(long endDay) {
        for (int i = reigns.size() - 1; i >= 0; i--) {
            ReignRecord reign = reigns.get(i);
            if (reign.isOpen()) {
                reigns.set(i, new ReignRecord(
                        reign.monarchId(),
                        reign.monarchName(),
                        reign.title(),
                        reign.ordinal(),
                        reign.accessionDay(),
                        Math.max(endDay, reign.accessionDay())));
                return true;
            }
        }
        return false;
    }
}
