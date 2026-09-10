package dev.mrlemoos.kingdom.war.muster;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InMemoryMusterStore implements MusterStore {

    private final Map<String, Set<UUID>> eligibleByWar = new HashMap<>();
    private final Map<String, Map<UUID, MusterAnswer>> answersByWar = new HashMap<>();
    private final Map<UUID, MoraleTier> levyMoraleByPlayer = new HashMap<>();

    @Override public Set<UUID> eligibleFor(String warId) { return Set.copyOf(eligibleByWar.getOrDefault(warId, Set.of())); }
    @Override public Map<UUID, MusterAnswer> answersFor(String warId) { return Map.copyOf(answersByWar.getOrDefault(warId, Map.of())); }
    @Override public Map<UUID, MoraleTier> levyMoraleView() { return Map.copyOf(levyMoraleByPlayer); }
    @Override public Map<String, Set<UUID>> eligibleByWarView() { return copyEligible(eligibleByWar); }
    @Override public Map<String, Map<UUID, MusterAnswer>> answersByWarView() { return copyAnswers(answersByWar); }

    @Override
    public void replaceAll(Map<String, Set<UUID>> eligible, Map<String, Map<UUID, MusterAnswer>> answers, Map<UUID, MoraleTier> morale) {
        eligibleByWar.clear(); eligibleByWar.putAll(copyEligible(eligible));
        answersByWar.clear(); answersByWar.putAll(copyAnswers(answers));
        levyMoraleByPlayer.clear();
        if (morale != null) levyMoraleByPlayer.putAll(morale);
    }

    @Override public void putEligible(String warId, Set<UUID> eligible) { eligibleByWar.put(warId, new LinkedHashSet<>(eligible)); }
    @Override public void putAnswer(String warId, UUID playerId, MusterAnswer answer) { answersByWar.computeIfAbsent(warId, ignored -> new LinkedHashMap<>()).put(playerId, answer); }
    @Override public void putLevyMorale(UUID playerId, MoraleTier tier) { levyMoraleByPlayer.put(playerId, tier); }
    @Override public void clearWar(String warId) { eligibleByWar.remove(warId); answersByWar.remove(warId); }
    @Override public void clearLevyMorale(UUID playerId) { levyMoraleByPlayer.remove(playerId); }

    private static Map<String, Set<UUID>> copyEligible(Map<String, Set<UUID>> source) {
        Map<String, Set<UUID>> copy = new HashMap<>();
        if (source != null) for (Map.Entry<String, Set<UUID>> entry : source.entrySet()) if (entry.getKey() != null && entry.getValue() != null) copy.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        return Map.copyOf(copy);
    }

    private static Map<String, Map<UUID, MusterAnswer>> copyAnswers(Map<String, Map<UUID, MusterAnswer>> source) {
        Map<String, Map<UUID, MusterAnswer>> copy = new HashMap<>();
        if (source != null) for (Map.Entry<String, Map<UUID, MusterAnswer>> entry : source.entrySet()) if (entry.getKey() != null && entry.getValue() != null) copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        return Map.copyOf(copy);
    }
}
