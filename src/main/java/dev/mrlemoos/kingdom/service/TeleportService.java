package dev.mrlemoos.kingdom.service;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TeleportService {

    private final KingdomService kingdomService;

    public TeleportService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public TeleportResult createPlace(String kingdomId, TeleportPlace place) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return TeleportResult.fail("Unknown kingdom.");
        }
        if (kingdom.get().getTeleport(place.name()).isPresent()) {
            return TeleportResult.fail("A checkpoint with that name already exists in this kingdom.");
        }
        kingdom.get().putTeleport(place);
        return TeleportResult.ok("Created checkpoint '" + place.name() + "' in " + kingdom.get().getDisplayName() + ".");
    }

    public TeleportResult deletePlace(String kingdomId, String name) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return TeleportResult.fail("Unknown kingdom.");
        }
        if (kingdom.get().getTeleport(name).isEmpty()) {
            return TeleportResult.fail("Unknown checkpoint.");
        }
        kingdom.get().removeTeleport(name);
        return TeleportResult.ok("Deleted checkpoint '" + Kingdom.normaliseId(name) + "'.");
    }

    public Optional<TeleportPlace> getPlace(String kingdomId, String name) {
        return kingdomService.getKingdom(kingdomId).flatMap(kingdom -> kingdom.getTeleport(name));
    }

    public List<TeleportPlace> listPlaces(String kingdomId) {
        Optional<Kingdom> kingdom = kingdomService.getKingdom(kingdomId);
        if (kingdom.isEmpty()) {
            return List.of();
        }
        List<TeleportPlace> places = new ArrayList<>(kingdom.get().getTeleportsView().values());
        places.sort(Comparator.comparing(TeleportPlace::name));
        return places;
    }
}
