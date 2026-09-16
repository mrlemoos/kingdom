package dev.mrlemoos.kingdom.parliament;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParliamentChatSessions {

    public enum SessionType {
        FISCAL,
        BUDGET_CUSTOM,
        STIPEND_PLAYER,
        STIPEND_AMOUNT,
        STIPEND_REASON,
        WAR_TARGET,
        TREATY_COUNTERPART,
        TREATY_KIND
    }

    public record Session(
            SessionType type,
            String kingdomId,
            UUID playerId,
            String optionalTitle,
            UUID stipendRecipientId,
            String stipendPlayerName,
            double stipendAmount,
            String treatyCounterpartId) {

        public Session(SessionType type, String kingdomId, UUID playerId) {
            this(type, kingdomId, playerId, null, null, null, 0, null);
        }

        public Session withTitle(String title) {
            return new Session(type, kingdomId, playerId, title, stipendRecipientId, stipendPlayerName, stipendAmount, treatyCounterpartId);
        }

        public Session withStipendRecipient(UUID recipientId, String playerName) {
            return new Session(type, kingdomId, playerId, optionalTitle, recipientId, playerName, stipendAmount, treatyCounterpartId);
        }

        public Session withStipendAmount(double amount) {
            return new Session(type, kingdomId, playerId, optionalTitle, stipendRecipientId, stipendPlayerName, amount, treatyCounterpartId);
        }

        public Session withTreatyCounterpart(String counterpartId) {
            return new Session(type, kingdomId, playerId, optionalTitle, stipendRecipientId, stipendPlayerName, stipendAmount, counterpartId);
        }

        public Session next(SessionType nextType) {
            return new Session(
                    nextType,
                    kingdomId,
                    playerId,
                    optionalTitle,
                    stipendRecipientId,
                    stipendPlayerName,
                    stipendAmount,
                    treatyCounterpartId);
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public void start(Session session) {
        sessions.put(session.playerId(), session);
    }

    public Optional<Session> get(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void advance(UUID playerId, Session session) {
        sessions.put(playerId, session);
    }

    public void cancel(UUID playerId) {
        sessions.remove(playerId);
    }

    public boolean has(UUID playerId) {
        return sessions.containsKey(playerId);
    }
}
