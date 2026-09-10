package dev.mrlemoos.kingdom.war.muster;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistence port for active muster eligibility, answers, and levy morale. */
public interface MusterStore {

    Set<UUID> eligibleFor(String warId);

    Map<UUID, MusterAnswer> answersFor(String warId);

    Map<UUID, MoraleTier> levyMoraleView();

    Map<String, Set<UUID>> eligibleByWarView();

    Map<String, Map<UUID, MusterAnswer>> answersByWarView();

    void replaceAll(
            Map<String, Set<UUID>> eligibleByWar,
            Map<String, Map<UUID, MusterAnswer>> answersByWar,
            Map<UUID, MoraleTier> levyMoraleByPlayer);

    void putEligible(String warId, Set<UUID> eligible);

    void putAnswer(String warId, UUID playerId, MusterAnswer answer);

    void putLevyMorale(UUID playerId, MoraleTier tier);

    void clearWar(String warId);

    void clearLevyMorale(UUID playerId);
}
