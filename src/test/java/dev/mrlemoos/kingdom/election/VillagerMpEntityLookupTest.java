package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMpEntityLookupTest {

    @Test
    void seatWithoutEntityIdIsVacant() {
        assertTrue(VillagerMpEntityLookup.isSeatVacant(VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID));
    }

    @Test
    void presentEntityIsNotVacant() {
        assertFalse(VillagerMpEntityLookup.isSeatVacant(VillagerMpEntityLookup.EntityPresence.PRESENT));
    }

    @Test
    void confirmedAbsentEntityIsVacant() {
        assertTrue(VillagerMpEntityLookup.isSeatVacant(VillagerMpEntityLookup.EntityPresence.ABSENT_CONFIRMED));
    }

    @Test
    void unknownPresenceIsNotVacantUntilChunkLoads() {
        assertFalse(VillagerMpEntityLookup.isSeatVacant(VillagerMpEntityLookup.EntityPresence.UNKNOWN));
    }

    @Test
    void shouldReplaceSeatedEntityOnlyWhenConfirmedAbsent() {
        assertFalse(VillagerMpEntityLookup.shouldReplaceSeatedEntity(VillagerMpEntityLookup.EntityPresence.PRESENT));
        assertFalse(VillagerMpEntityLookup.shouldReplaceSeatedEntity(VillagerMpEntityLookup.EntityPresence.UNKNOWN));
        assertTrue(VillagerMpEntityLookup.shouldReplaceSeatedEntity(VillagerMpEntityLookup.EntityPresence.ABSENT_CONFIRMED));
        assertTrue(VillagerMpEntityLookup.shouldReplaceSeatedEntity(VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID));
    }

    @Test
    void startupSyncShouldRetryWhileUnknown() {
        assertTrue(VillagerMpEntityLookup.needsStartupSyncRetry(VillagerMpEntityLookup.EntityPresence.UNKNOWN));
        assertFalse(VillagerMpEntityLookup.needsStartupSyncRetry(VillagerMpEntityLookup.EntityPresence.PRESENT));
    }

    @Test
    void assignedSeatWithoutEntityIdIsNotVacantForByElection() {
        assertFalse(VillagerMpEntityLookup.isSeatVacantForByElection(
                VillagerMpEntityLookup.EntityPresence.ABSENT_NO_ID, false));
    }

    @Test
    void confirmedAbsentStoredEntityIsVacantForByElection() {
        assertTrue(VillagerMpEntityLookup.isSeatVacantForByElection(
                VillagerMpEntityLookup.EntityPresence.ABSENT_CONFIRMED, true));
    }

    @Test
    void unknownPresenceIsNotVacantForByElection() {
        assertFalse(VillagerMpEntityLookup.isSeatVacantForByElection(
                VillagerMpEntityLookup.EntityPresence.UNKNOWN, true));
    }
}
