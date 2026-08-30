package dev.mrlemoos.kingdom.church;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Standing offers of marriage and of divorce. A rite that binds two people needs both to say so:
 * one offers, the other answers by making the same offer back.
 *
 * <p>Memory-only, as open police cases are: an offer nobody answered before the server stopped is
 * an offer worth making again.
 */
public final class ChurchConsentBook {

    /** offerer → the subject they named. */
    private final Map<UUID, UUID> weddingOffers = new HashMap<>();

    private final Map<UUID, UUID> divorceOffers = new HashMap<>();

    /** @return true when the other party had already offered, so the rite may go ahead. */
    public boolean offerWedding(UUID from, UUID to) {
        return offer(weddingOffers, from, to);
    }

    public boolean offerDivorce(UUID from, UUID to) {
        return offer(divorceOffers, from, to);
    }

    public boolean hasWeddingOffer(UUID from, UUID to) {
        return to.equals(weddingOffers.get(from));
    }

    public void clear(UUID first, UUID second) {
        weddingOffers.remove(first);
        weddingOffers.remove(second);
        divorceOffers.remove(first);
        divorceOffers.remove(second);
    }

    public void forget(UUID playerId) {
        weddingOffers.remove(playerId);
        divorceOffers.remove(playerId);
        weddingOffers.values().removeIf(playerId::equals);
        divorceOffers.values().removeIf(playerId::equals);
    }

    private static boolean offer(Map<UUID, UUID> offers, UUID from, UUID to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.equals(to)) {
            return false;
        }
        if (from.equals(offers.get(to))) {
            offers.remove(to);
            offers.remove(from);
            return true;
        }
        offers.put(from, to);
        return false;
    }
}
