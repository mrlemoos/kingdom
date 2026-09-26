package dev.mrlemoos.kingdom.poll;

import java.util.List;
import java.util.UUID;

/**
 * What a poll card offers one member at this moment: standing as a candidate, a ballot among the
 * candidates (or, for the Speaker, the casting vote between the tied), or the referendum's question.
 *
 * @param kind the poll the card belongs to; {@code null} when that poll has closed
 * @param candidates whom the member may vote for, in nomination order
 */
public record PollCardOffer(
        PollCardRules.PollKind kind,
        boolean mayStand,
        boolean standing,
        boolean mayVote,
        List<UUID> candidates,
        boolean voted) {

    public PollCardOffer {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    /** A card whose poll has closed. */
    public static PollCardOffer closed() {
        return new PollCardOffer(null, false, false, false, List.of(), false);
    }

    public boolean stale() {
        return kind == null;
    }

    /** Whether the card asks anything of this member. */
    public boolean hasBusiness() {
        return !stale() && (mayStand || standing || mayVote);
    }
}
