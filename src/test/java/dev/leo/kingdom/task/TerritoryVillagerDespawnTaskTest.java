package dev.leo.kingdom.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TerritoryVillagerDespawnTaskTest {

    @Test
    void defaultIntervalMatchesElectionTaskCadence() {
        assertEquals(ElectionTask.DEFAULT_INTERVAL_TICKS, TerritoryVillagerDespawnTask.DEFAULT_INTERVAL_TICKS);
    }
}
