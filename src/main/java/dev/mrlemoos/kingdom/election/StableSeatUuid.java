package dev.mrlemoos.kingdom.election;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StableSeatUuid {

    private StableSeatUuid() {}

    public static UUID forSeat(String kingdomId, int seatIndex) {
        return UUID.nameUUIDFromBytes(
                ("mp:" + kingdomId + ":" + seatIndex).getBytes(StandardCharsets.UTF_8));
    }
}
