package dev.mrlemoos.kingdom.model.parliament;

import java.util.Objects;
import java.util.UUID;

/**
 * A motion of no confidence that has been tabled but not yet confirmed by a <b>seconder</b>. The
 * motion holds the order paper meanwhile, but no division may open on it until a second seated
 * player MP—never the proposer, never the Premier—rises to second it.
 */
public final class PendingMotionSecond {

    private final String billId;
    private final UUID proposedBy;
    private final long offeredAtMs;

    public PendingMotionSecond(String billId, UUID proposedBy, long offeredAtMs) {
        this.billId = Objects.requireNonNull(billId, "billId");
        this.proposedBy = Objects.requireNonNull(proposedBy, "proposedBy");
        this.offeredAtMs = offeredAtMs;
    }

    public String billId() {
        return billId;
    }

    public UUID proposedBy() {
        return proposedBy;
    }

    public long offeredAtMs() {
        return offeredAtMs;
    }
}
