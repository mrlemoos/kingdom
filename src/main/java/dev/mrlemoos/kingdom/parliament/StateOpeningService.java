package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Governs the State Opening: after a general election forms a government, Parliament stays
 * prorogued until the Crown (or, absent a monarch, the heir as regent) opens the session. If
 * neither attends within the commission delay — or the kingdom has no Lords chamber to gather in —
 * the session opens by royal commission so the realm's business is never permanently frozen.
 */
public final class StateOpeningService {

    public static final int DEFAULT_COMMISSION_DELAY_MC_DAYS = 3;

    private final KingdomService kingdomService;
    private final ParliamentService parliamentService;
    private final int commissionDelayMcDays;

    public StateOpeningService(KingdomService kingdomService, ParliamentService parliamentService) {
        this(kingdomService, parliamentService, DEFAULT_COMMISSION_DELAY_MC_DAYS);
    }

    public StateOpeningService(
            KingdomService kingdomService, ParliamentService parliamentService, int commissionDelayMcDays) {
        this.kingdomService = kingdomService;
        this.parliamentService = parliamentService;
        this.commissionDelayMcDays = Math.max(commissionDelayMcDays, 0);
    }

    /** Summons the Crown once a government has formed. */
    public StateOpeningSummons requestStateOpening(String kingdomId, long currentMcDay) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return StateOpeningSummons.NOT_NEEDED;
        }
        ParliamentState state = kingdom.get().getParliamentState();
        if (state.isSessionOpen()) {
            return StateOpeningSummons.NOT_NEEDED;
        }
        if (kingdom.get().getParliamentSites().lords().isEmpty()) {
            state.openSession();
            return StateOpeningSummons.COMMISSIONED;
        }
        state.awaitStateOpening(currentMcDay);
        return StateOpeningSummons.AWAITING_CROWN;
    }

    public boolean isAwaitingStateOpening(String kingdomId) {
        return kingdomService.getKingdom(kingdomId)
                .map(Kingdom::getParliamentState)
                .filter(state -> !state.isSessionOpen())
                .map(state -> state.stateOpeningPendingSinceMcDay().isPresent())
                .orElse(false);
    }

    /** The King or Queen, or the heir as regent when no monarch is seated. */
    public boolean canOpen(String kingdomId, UUID playerId) {
        return ResignationAuthority.monarchOrRegent(kingdomId, kingdomService)
                .filter(holder -> holder.equals(playerId))
                .isPresent();
    }

    public ParliamentResult open(String kingdomId, UUID playerId) {
        if (!isAwaitingStateOpening(kingdomId)) {
            return ParliamentResult.fail("Parliament is not waiting to be opened.");
        }
        if (!canOpen(kingdomId, playerId)) {
            return ParliamentResult.fail("Only the Crown may open Parliament.");
        }
        return parliamentService.openSession(kingdomId);
    }

    /**
     * Opens the session by royal commission once the Crown has failed to attend for the delay
     * period. Returns the announcement when it fires.
     */
    public Optional<String> commissionIfOverdue(String kingdomId, long currentMcDay) {
        if (!isAwaitingStateOpening(kingdomId)) {
            return Optional.empty();
        }
        OptionalLong pendingSince = kingdomService
                .getKingdom(kingdomId)
                .orElseThrow()
                .getParliamentState()
                .stateOpeningPendingSinceMcDay();
        if (pendingSince.isEmpty() || currentMcDay < pendingSince.getAsLong() + commissionDelayMcDays) {
            return Optional.empty();
        }
        ParliamentResult opened = parliamentService.openSession(kingdomId);
        if (opened instanceof ParliamentResult.Failure) {
            return Optional.empty();
        }
        return Optional.of("Parliament has been opened by royal commission in the Crown's absence.");
    }
}
