package dev.mrlemoos.kingdom.appeal;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Runtime-only Crown petitions for active player prison sentences. */
public final class AppealService {
    private final KingdomService kingdomService;
    private final PoliceTrialService trialService;
    private final Supplier<Long> clockMs;
    private final Map<String, UUID> pendingByKingdom = new HashMap<>();

    public AppealService(KingdomService kingdomService, PoliceTrialService trialService) {
        this(kingdomService, trialService, System::currentTimeMillis);
    }

    AppealService(KingdomService kingdomService, PoliceTrialService trialService, Supplier<Long> clockMs) {
        this.kingdomService = Objects.requireNonNull(kingdomService);
        this.trialService = Objects.requireNonNull(trialService);
        this.clockMs = Objects.requireNonNull(clockMs);
    }

    public AppealResult petition(String kingdomId, UUID petitioner) {
        if (kingdomService.getMembership(petitioner).filter(m -> kingdomId.equals(m.getKingdomId())).isEmpty()) {
            return fail("You are not in that kingdom.");
        }
        if (!trialService.isUnderPrisonSentence(petitioner)) {
            return fail("Only an active prison sentence may be appealed.");
        }
        if (pendingByKingdom.containsKey(kingdomId)) {
            return fail("An appeal is already awaiting the Crown.");
        }
        pendingByKingdom.put(kingdomId, petitioner);
        return ok("Your appeal has been delivered to the Crown.");
    }

    public AppealResult uphold(String kingdomId, NobleRank crownRank) {
        return resolve(kingdomId, crownRank, Action.UPHOLD);
    }

    public AppealResult commute(String kingdomId, NobleRank crownRank) {
        return resolve(kingdomId, crownRank, Action.COMMUTE);
    }

    public AppealResult pardon(String kingdomId, NobleRank crownRank) {
        return resolve(kingdomId, crownRank, Action.PARDON);
    }

    public java.util.Optional<UUID> pendingAppeal(String kingdomId) {
        return java.util.Optional.ofNullable(pendingByKingdom.get(kingdomId));
    }

    private AppealResult resolve(String kingdomId, NobleRank crownRank, Action action) {
        if (!ResignationAuthority.canResolveResignation(kingdomId, kingdomService, crownRank)) {
            return fail("Only the Crown may resolve an appeal.");
        }
        UUID petitioner = pendingByKingdom.get(kingdomId);
        if (petitioner == null) {
            return fail("No appeal awaits the Crown.");
        }
        PoliceResult result = switch (action) {
            case UPHOLD -> PoliceResult.ok("Appeal upheld without commutation.");
            case COMMUTE -> trialService.commutePrisonSentence(petitioner, clockMs.get());
            case PARDON -> trialService.releaseFromPrison(petitioner);
        };
        if (result instanceof PoliceResult.Failure failure) {
            return fail(failure.message());
        }
        pendingByKingdom.remove(kingdomId);
        return ok(switch (action) {
            case UPHOLD -> "Appeal upheld.";
            case COMMUTE -> "Sentence commuted.";
            case PARDON -> "Pardon granted. Prisoner released.";
        });
    }

    private static AppealResult.Success ok(String message) {
        return new AppealResult.Success(message);
    }

    private static AppealResult.Failure fail(String message) {
        return new AppealResult.Failure(message);
    }

    private enum Action {
        UPHOLD,
        COMMUTE,
        PARDON
    }
}
