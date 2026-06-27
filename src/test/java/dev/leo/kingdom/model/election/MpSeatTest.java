package dev.leo.kingdom.model.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MpSeatTest {

    @Test
    void villagerSeatStoresOriginLocation() {
        MpSeat seat = new MpSeat(5);
        MpSeatLocation origin = new MpSeatLocation("world", 10.5, 64.0, -3.5, 90f, 0f);

        seat.assignVillager("farmer", UUID.randomUUID());
        seat.setOriginLocation(origin);

        assertEquals(origin, seat.originLocation().orElseThrow());
    }

    @Test
    void clearRemovesOriginLocation() {
        MpSeat seat = new MpSeat(5);
        seat.assignVillager("farmer", UUID.randomUUID());
        seat.setOriginLocation(new MpSeatLocation("world", 1, 2, 3, 0, 0));

        seat.clear();

        assertTrue(seat.originLocation().isEmpty());
        assertTrue(seat.entityId().isEmpty());
    }
}
