package dev.mrlemoos.kingdom.election;

public final class VillagerMpEntityLookup {

    public enum EntityPresence {
        /** Seat has no stored entity id. */
        ABSENT_NO_ID,
        /** Chunk was loaded and the entity is not in the world. */
        ABSENT_CONFIRMED,
        /** Entity is loaded in memory. */
        PRESENT,
        /** Entity id exists but seat/origin chunks could not be loaded yet. */
        UNKNOWN
    }

    private VillagerMpEntityLookup() {}

    public static boolean isSeatVacant(EntityPresence presence) {
        return presence == EntityPresence.ABSENT_NO_ID || presence == EntityPresence.ABSENT_CONFIRMED;
    }

    /**
     * Whether a villager MP seat should trigger a by-election. A seat assigned after an election
     * may have no entity id until {@code VillagerMpEntityService} syncs; that is not a vacancy.
     */
    public static boolean isSeatVacantForByElection(EntityPresence presence, boolean hasStoredEntityId) {
        if (presence == EntityPresence.UNKNOWN) {
            return false;
        }
        if (presence == EntityPresence.ABSENT_NO_ID && !hasStoredEntityId) {
            return false;
        }
        return isSeatVacant(presence);
    }

    public static boolean shouldReplaceSeatedEntity(EntityPresence presence) {
        return presence == EntityPresence.ABSENT_NO_ID || presence == EntityPresence.ABSENT_CONFIRMED;
    }

    public static boolean needsStartupSyncRetry(EntityPresence presence) {
        return presence == EntityPresence.UNKNOWN;
    }
}
