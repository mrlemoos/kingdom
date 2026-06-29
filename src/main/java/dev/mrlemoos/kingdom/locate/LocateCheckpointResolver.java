package dev.mrlemoos.kingdom.locate;

import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class LocateCheckpointResolver {

    private LocateCheckpointResolver() {
    }

    public static Optional<TeleportPlace> resolve(
            UUID playerId, String checkpointName, KingdomService kingdomService, TeleportService teleportService) {
        if (isReservedKeyword(checkpointName)) {
            return Optional.empty();
        }
        return kingdomService
                .getMembership(playerId)
                .flatMap(membership -> teleportService.getPlace(membership.getKingdomId(), checkpointName));
    }

    public static boolean isReservedKeyword(String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        return "structure".equals(lower) || "biome".equals(lower);
    }
}
