package dev.mrlemoos.kingdom.church;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Standing requests for consent to a marriage or a divorce. One party asks at the church; the other
 * answers Accept or Refuse in a window within {@link #WINDOW_MS}, or the request lapses.
 *
 * <p>Memory-only, as open police cases are: a request nobody answered before the server stopped is
 * one worth making again.
 */
public final class ChurchConsentBook {

    /** How long the asked party has to answer. */
    public static final long WINDOW_MS = 60_000L;

    public enum Kind {
        MARRIAGE,
        DIVORCE
    }

    /** One request, keyed by the party whose answer it waits on. */
    public record Request(
            Kind kind, String kingdomId, UUID proposer, UUID answerer, long openedAtMs, long closesAtMs) {

        public long remainingMs(long nowMs) {
            return Math.max(0L, closesAtMs - nowMs);
        }

        boolean lapsed(long nowMs) {
            return nowMs >= closesAtMs;
        }

        boolean involves(UUID playerId) {
            return proposer.equals(playerId) || answerer.equals(playerId);
        }
    }

    /** answerer → the request waiting on them. */
    private final Map<UUID, Request> requests = new HashMap<>();

    /** @return the refusal, or empty when the request now waits on {@code to}'s answer */
    public Optional<String> propose(Kind kind, String kingdomId, UUID from, UUID to, long nowMs) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.equals(to)) {
            return Optional.of("Nobody may answer for themselves.");
        }
        expire(nowMs);
        for (Request request : requests.values()) {
            if (request.involves(from) || request.involves(to)) {
                return Optional.of("One of you already waits on an answer at the church.");
            }
        }
        requests.put(to, new Request(kind, kingdomId, from, to, nowMs, nowMs + WINDOW_MS));
        return Optional.empty();
    }

    public Optional<Request> pendingFor(UUID answerer, long nowMs) {
        Request request = requests.get(answerer);
        if (request == null || request.lapsed(nowMs)) {
            return Optional.empty();
        }
        return Optional.of(request);
    }

    /** Closes the request waiting on this answerer; empty when there is none or it has lapsed. */
    public Optional<Request> answer(UUID answerer, long nowMs) {
        Optional<Request> pending = pendingFor(answerer, nowMs);
        if (pending.isPresent()) {
            requests.remove(answerer);
        }
        return pending;
    }

    /** Every request still waiting on its answer. */
    public List<Request> open(long nowMs) {
        List<Request> open = new ArrayList<>();
        for (Request request : requests.values()) {
            if (!request.lapsed(nowMs)) {
                open.add(request);
            }
        }
        return open;
    }

    /** Removes and returns every request whose window has run out. */
    public List<Request> expire(long nowMs) {
        List<Request> lapsed = new ArrayList<>();
        Iterator<Request> iterator = requests.values().iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (request.lapsed(nowMs)) {
                lapsed.add(request);
                iterator.remove();
            }
        }
        return lapsed;
    }

    public void forget(UUID playerId) {
        requests.values().removeIf(request -> request.involves(playerId));
    }
}
