package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * An auditable, expiring grant of permission for a fealty subject to leave an active siege
 * without a morale breach — see the <b>Siege release</b> glossary entry in {@code CONTEXT.md}.
 */
class SiegeReleaseGrantTest {

    private static final UUID SUBJECT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRANTER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String WAR_ID = "war-1";

    @Test
    void grantIsValidBeforeItsExpiry() {
        SiegeReleaseGrant grant = new SiegeReleaseGrant(SUBJECT, WAR_ID, GRANTER, 1_000L, 2_000L, "field release");

        assertTrue(grant.isValidAt(1_500L));
    }

    @Test
    void grantIsInvalidAtOrAfterItsExpiry() {
        SiegeReleaseGrant grant = new SiegeReleaseGrant(SUBJECT, WAR_ID, GRANTER, 1_000L, 2_000L, "field release");

        assertFalse(grant.isValidAt(2_000L));
        assertFalse(grant.isValidAt(2_500L));
    }

    @Test
    void aNullNoteIsNormalisedToAnEmptyString() {
        SiegeReleaseGrant grant = new SiegeReleaseGrant(SUBJECT, WAR_ID, GRANTER, 1_000L, 2_000L, null);

        assertEquals("", grant.note());
    }

    @Test
    void expiryAtOrBeforeGrantTimeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SiegeReleaseGrant(SUBJECT, WAR_ID, GRANTER, 2_000L, 2_000L, null));
    }

    @Test
    void nullSubjectIsRejected() {
        assertThrows(
                NullPointerException.class, () -> new SiegeReleaseGrant(null, WAR_ID, GRANTER, 1_000L, 2_000L, null));
    }

    @Test
    void nullWarIdIsRejected() {
        assertThrows(
                NullPointerException.class, () -> new SiegeReleaseGrant(SUBJECT, null, GRANTER, 1_000L, 2_000L, null));
    }

    @Test
    void nullGranterIsRejected() {
        assertThrows(
                NullPointerException.class, () -> new SiegeReleaseGrant(SUBJECT, WAR_ID, null, 1_000L, 2_000L, null));
    }
}
