package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImmutableNpcPolicyTest {

    private static final UUID CRIER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CLERIC = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void ordinaryVillagerIsNotImmutable() {
        assertFalse(ImmutableNpcPolicy.isImmutable(false, false, false));
        assertTrue(ImmutableNpcPolicy.mayTakeFurtherOffice(false, false, false));
    }

    @Test
    void treasuryLordTownCrierAndClericAreImmutable() {
        assertTrue(ImmutableNpcPolicy.isImmutable(true, false, false));
        assertTrue(ImmutableNpcPolicy.isImmutable(false, true, false));
        assertTrue(ImmutableNpcPolicy.isImmutable(false, false, true));
        assertFalse(ImmutableNpcPolicy.mayTakeFurtherOffice(true, false, false));
        assertFalse(ImmutableNpcPolicy.mayTakeFurtherOffice(false, true, false));
        assertFalse(ImmutableNpcPolicy.mayTakeFurtherOffice(false, false, true));
    }

    @Test
    void storedCrierAndClericIdsMatch() {
        assertTrue(ImmutableNpcPolicy.matchesStoredIds(CRIER, Optional.of(CRIER), Optional.of(CLERIC)));
        assertTrue(ImmutableNpcPolicy.matchesStoredIds(CLERIC, Optional.of(CRIER), Optional.of(CLERIC)));
        assertFalse(ImmutableNpcPolicy.matchesStoredIds(OTHER, Optional.of(CRIER), Optional.of(CLERIC)));
        assertFalse(ImmutableNpcPolicy.matchesStoredIds(CRIER, Optional.empty(), Optional.empty()));
    }
}
