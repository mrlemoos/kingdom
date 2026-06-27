package dev.leo.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TreasuryLordPresenceTest {

    @Test
    void prefersStoredLordWhenStillPresent() {
        UUID stored = UUID.randomUUID();
        UUID duplicate = UUID.randomUUID();

        Optional<UUID> canonical =
                TreasuryLordPresence.selectCanonicalLord(Optional.of(stored), List.of(stored, duplicate));

        assertEquals(Optional.of(stored), canonical);
        assertEquals(List.of(duplicate), TreasuryLordPresence.duplicateLordIdsToRemove(List.of(stored, duplicate), stored));
    }

    @Test
    void adoptsFirstPresentLordWhenStoredIdMissing() {
        UUID present = UUID.randomUUID();

        Optional<UUID> canonical = TreasuryLordPresence.selectCanonicalLord(Optional.empty(), List.of(present));

        assertEquals(Optional.of(present), canonical);
    }

    @Test
    void adoptsFirstPresentLordWhenStoredIdStale() {
        UUID stale = UUID.randomUUID();
        UUID present = UUID.randomUUID();

        Optional<UUID> canonical =
                TreasuryLordPresence.selectCanonicalLord(Optional.of(stale), List.of(present));

        assertEquals(Optional.of(present), canonical);
        assertEquals(List.of(), TreasuryLordPresence.duplicateLordIdsToRemove(List.of(present), present));
    }

    @Test
    void removesAllPresentLordsWhenNoneShouldRemain() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(TreasuryLordPresence.selectCanonicalLord(Optional.empty(), List.of()).isEmpty());
        assertEquals(List.of(first, second), TreasuryLordPresence.lordIdsToRemove(List.of(first, second), Optional.empty()));
    }
}
