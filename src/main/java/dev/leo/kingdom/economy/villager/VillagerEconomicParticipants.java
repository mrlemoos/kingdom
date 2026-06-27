package dev.leo.kingdom.economy.villager;

import dev.leo.kingdom.model.election.MpSeat;
import dev.leo.kingdom.model.election.MpSeatKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillagerEconomicParticipants {

    private VillagerEconomicParticipants() {}

    public static List<VillagerEconomicParticipant> merge(
            List<VillagerEconomicParticipant> productive, List<MpSeat> seatedVillagerMpSeats) {
        Map<UUID, VillagerEconomicParticipant> byId = new LinkedHashMap<>();
        for (VillagerEconomicParticipant participant : productive) {
            byId.put(participant.villagerId(), participant);
        }
        for (MpSeat seat : seatedVillagerMpSeats) {
            if (seat.kind() != MpSeatKind.VILLAGER) {
                continue;
            }
            UUID entityId = seat.entityId().orElse(null);
            String profession = seat.profession().orElse(null);
            if (entityId == null || profession == null || profession.isBlank()) {
                continue;
            }
            byId.putIfAbsent(entityId, new VillagerEconomicParticipant(entityId, profession, 0));
        }
        return new ArrayList<>(byId.values());
    }
}
