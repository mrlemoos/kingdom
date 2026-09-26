package dev.mrlemoos.kingdom.honours;

import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Swearing and unswearing the realm's sworn roles — constable, judge and priest — under one set of
 * rules, whether the Crown does it with the golden sword or an operator by command. The police and
 * church services keep their own invariants (membership, constable/judge exclusion, the priest apart
 * from the police); this adds the coronation gate and the bar on swearing a prisoner.
 */
public final class SwornRoleAppointments {

    /** What came of one swearing or unswearing. {@code sworn} is true when the role is now held. */
    public record Outcome(boolean success, boolean sworn, String message) {

        static Outcome refused(String message) {
            return new Outcome(false, false, message);
        }
    }

    private final PoliceService policeService;
    private final ChurchService churchService;

    public SwornRoleAppointments(PoliceService policeService, ChurchService churchService) {
        this.policeService = Objects.requireNonNull(policeService, "policeService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
    }

    public static String label(SwornRole role) {
        return switch (role) {
            case CONSTABLE -> "Constable";
            case JUDGE -> "Judge";
            case PRIEST -> "Priest";
        };
    }

    public boolean holds(String kingdomId, UUID playerId, SwornRole role) {
        return switch (role) {
            case CONSTABLE -> policeService.isConstable(kingdomId, playerId);
            case JUDGE -> policeService.isJudge(kingdomId, playerId);
            case PRIEST -> churchService.isPriest(kingdomId, playerId);
        };
    }

    public Set<SwornRole> heldBy(String kingdomId, UUID playerId) {
        Set<SwornRole> held = EnumSet.noneOf(SwornRole.class);
        for (SwornRole role : SwornRole.values()) {
            if (holds(kingdomId, playerId, role)) {
                held.add(role);
            }
        }
        return held;
    }

    /** Unswears the role when it is held, else swears it. */
    public Outcome toggle(String kingdomId, UUID actorId, NobleRank actorRank, UUID targetId, SwornRole role) {
        return holds(kingdomId, targetId, role)
                ? unswear(kingdomId, actorId, actorRank, targetId, role)
                : swear(kingdomId, actorId, actorRank, targetId, role);
    }

    /**
     * Swears {@code targetId} into {@code role}. A null {@code actorId} is an operator acting for the
     * Crown, and is not held to the coronation gate.
     */
    public Outcome swear(String kingdomId, UUID actorId, NobleRank actorRank, UUID targetId, SwornRole role) {
        Optional<String> uncrowned = gate(kingdomId, actorId, actorRank);
        if (uncrowned.isPresent()) {
            return Outcome.refused(uncrowned.get());
        }
        if (targetId != null && churchService.isUnderPrisonSentence(targetId)) {
            return Outcome.refused("That subject is serving a prison sentence and cannot be sworn.");
        }
        return switch (role) {
            case CONSTABLE -> of(policeService.appointConstable(kingdomId, actorRank, targetId), true);
            case JUDGE -> of(policeService.appointJudge(kingdomId, actorRank, targetId), true);
            case PRIEST -> of(churchService.swearPriest(kingdomId, actorRank, targetId), true);
        };
    }

    /** Releases {@code targetId} from {@code role}; a null {@code actorId} is an operator. */
    public Outcome unswear(String kingdomId, UUID actorId, NobleRank actorRank, UUID targetId, SwornRole role) {
        Optional<String> uncrowned = gate(kingdomId, actorId, actorRank);
        if (uncrowned.isPresent()) {
            return Outcome.refused(uncrowned.get());
        }
        return switch (role) {
            case CONSTABLE -> of(policeService.dismissConstable(kingdomId, actorRank, targetId), false);
            case JUDGE -> of(policeService.dismissJudge(kingdomId, actorRank, targetId), false);
            case PRIEST -> of(churchService.unswearPriest(kingdomId, actorRank, targetId), false);
        };
    }

    private Optional<String> gate(String kingdomId, UUID actorId, NobleRank actorRank) {
        if (actorId == null) {
            return Optional.empty();
        }
        return churchService.ceremonialRefusal(kingdomId, actorId, actorRank);
    }

    private static Outcome of(PoliceResult result, boolean swearing) {
        return result instanceof PoliceResult.Success
                ? new Outcome(true, swearing, result.message())
                : Outcome.refused(result.message());
    }

    private static Outcome of(ChurchResult result, boolean swearing) {
        return result instanceof ChurchResult.Success
                ? new Outcome(true, swearing, result.message())
                : Outcome.refused(result.message());
    }
}
