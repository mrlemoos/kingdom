package dev.leo.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.model.election.MpSeat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VillagerEconomicParticipantsTest {

    private static final UUID PRODUCTIVE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SEATED = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void seatedMpProxyJoinsWhenNotTerritoryProductive() {
        MpSeat seat = new MpSeat(1);
        seat.assignVillager("librarian", SEATED);

        List<VillagerEconomicParticipant> participants = VillagerEconomicParticipants.merge(
                List.of(new VillagerEconomicParticipant(PRODUCTIVE, "farmer", 1)),
                List.of(seat));

        assertEquals(2, participants.size());
        assertTrue(participants.stream().anyMatch(p -> p.villagerId().equals(SEATED)));
    }

    @Test
    void productiveVillagerKeepsTierWhenAlsoSeatedMp() {
        MpSeat seat = new MpSeat(1);
        seat.assignVillager("farmer", PRODUCTIVE);

        List<VillagerEconomicParticipant> participants = VillagerEconomicParticipants.merge(
                List.of(new VillagerEconomicParticipant(PRODUCTIVE, "farmer", 2)),
                List.of(seat));

        assertEquals(1, participants.size());
        assertEquals(2, participants.getFirst().tierIndex());
    }
}
