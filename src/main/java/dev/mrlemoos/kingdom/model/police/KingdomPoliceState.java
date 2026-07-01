package dev.mrlemoos.kingdom.model.police;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class KingdomPoliceState {

    private final Set<UUID> constables = new HashSet<>();
    private final Set<UUID> judges = new HashSet<>();
    private final Map<Integer, PrisonCellLocation> cells = new HashMap<>();
    private CourtLocation court;
    private UUID judgeEntityId;
    private final Set<UUID> patrolGolems = new HashSet<>();
    private final Set<UUID> guardGolems = new HashSet<>();

    public Set<UUID> constablesView() {
        return Set.copyOf(constables);
    }

    public Set<UUID> judgesView() {
        return Set.copyOf(judges);
    }

    public Map<Integer, PrisonCellLocation> cellsView() {
        return Map.copyOf(cells);
    }

    public Set<UUID> patrolGolemsView() {
        return Set.copyOf(patrolGolems);
    }

    public Set<UUID> guardGolemsView() {
        return Set.copyOf(guardGolems);
    }

    public boolean isConstable(UUID playerId) {
        return constables.contains(playerId);
    }

    public boolean isJudge(UUID playerId) {
        return judges.contains(playerId);
    }

    public void appointConstable(UUID playerId) {
        constables.add(playerId);
        judges.remove(playerId);
    }

    public void dismissConstable(UUID playerId) {
        constables.remove(playerId);
    }

    public void appointJudge(UUID playerId) {
        judges.add(playerId);
        constables.remove(playerId);
    }

    public void dismissJudge(UUID playerId) {
        judges.remove(playerId);
    }

    public Optional<PrisonCellLocation> cell(int slot) {
        return Optional.ofNullable(cells.get(slot));
    }

    public void setCell(int slot, PrisonCellLocation location) {
        cells.put(slot, location);
    }

    public void clearCell(int slot) {
        cells.remove(slot);
    }

    public OptionalInt lowestFreeCellSlot(int maxCells) {
        for (int slot = 1; slot <= maxCells; slot++) {
            if (!cells.containsKey(slot)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    public int configuredCellCount() {
        return cells.size();
    }

    public Optional<CourtLocation> court() {
        return Optional.ofNullable(court);
    }

    public boolean hasCourt() {
        return court != null;
    }

    public void setCourt(CourtLocation location) {
        court = location;
    }

    public void clearCourt() {
        court = null;
        judgeEntityId = null;
    }

    public Optional<UUID> judgeEntityId() {
        return Optional.ofNullable(judgeEntityId);
    }

    public void setJudgeEntityId(UUID entityId) {
        judgeEntityId = entityId;
    }

    public void clearJudgeEntityId() {
        judgeEntityId = null;
    }

    public boolean registerPatrolGolem(UUID entityId) {
        guardGolems.remove(entityId);
        return patrolGolems.add(entityId);
    }

    public boolean registerGuardGolem(UUID entityId) {
        patrolGolems.remove(entityId);
        return guardGolems.add(entityId);
    }

    public boolean deregisterGolem(UUID entityId) {
        boolean removed = patrolGolems.remove(entityId);
        removed |= guardGolems.remove(entityId);
        return removed;
    }

    public boolean isPatrolGolem(UUID entityId) {
        return patrolGolems.contains(entityId);
    }

    public boolean isGuardGolem(UUID entityId) {
        return guardGolems.contains(entityId);
    }

    public boolean isRegisteredGolem(UUID entityId) {
        return isPatrolGolem(entityId) || isGuardGolem(entityId);
    }

    public int patrolGolemCount() {
        return patrolGolems.size();
    }

    public int guardGolemCount() {
        return guardGolems.size();
    }

    public void replaceConstables(Set<UUID> loaded) {
        constables.clear();
        if (loaded != null) {
            constables.addAll(loaded);
        }
    }

    public void replaceJudges(Set<UUID> loaded) {
        judges.clear();
        if (loaded != null) {
            judges.addAll(loaded);
        }
    }

    public void replaceCells(Map<Integer, PrisonCellLocation> loaded) {
        cells.clear();
        if (loaded != null) {
            cells.putAll(loaded);
        }
    }

    public void replacePatrolGolems(Set<UUID> loaded) {
        patrolGolems.clear();
        if (loaded != null) {
            patrolGolems.addAll(loaded);
        }
    }

    public void replaceGuardGolems(Set<UUID> loaded) {
        guardGolems.clear();
        if (loaded != null) {
            guardGolems.addAll(loaded);
        }
    }
}
