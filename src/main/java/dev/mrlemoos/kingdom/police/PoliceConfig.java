package dev.mrlemoos.kingdom.police;

import java.util.List;

public record PoliceConfig(
        int maxCells,
        int maxPatrolGolems,
        int maxGuardGolems,
        int prisonSnapBackBlocks,
        List<Integer> sentenceMinutePresets) {

    public static PoliceConfig defaults() {
        return new PoliceConfig(4, 2, 2, 8, List.of(5, 15, 30, 60));
    }
}
