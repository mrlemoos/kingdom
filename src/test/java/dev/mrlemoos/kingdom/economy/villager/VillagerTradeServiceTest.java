package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VillagerTradeServiceTest {

    private static final EconomyConfig CONFIG = EconomyConfig.defaults();
    private static final UUID FARMER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUTCHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void runsEdgeWhenSellerProfessionExists() {
        List<VillagerTradeEdge> edges = List.of(VillagerTradeEdge.spendPercent("farmer", "butcher", 0.5));
        List<VillagerEconomicParticipant> participants = List.of(
                new VillagerEconomicParticipant(FARMER, "farmer", 0),
                new VillagerEconomicParticipant(BUTCHER, "butcher", 0));

        List<VillagerTradeSettlement> settlements = new VillagerTradeService().planTrades(
                edges, participants, CONFIG, 0.05, 1, new Random(1));

        assertEquals(1, settlements.size());
        assertEquals(FARMER, settlements.getFirst().buyerId());
        assertEquals(BUTCHER, settlements.getFirst().sellerId());
        assertEquals(0.2, settlements.getFirst().payment(), 1e-9);
        assertEquals(0.01, settlements.getFirst().commerceTax(), 1e-9);
    }

    @Test
    void runsMultiplePassesPerEdge() {
        List<VillagerTradeEdge> edges = List.of(VillagerTradeEdge.spendPercent("farmer", "butcher", 0.5));
        List<VillagerEconomicParticipant> participants = List.of(
                new VillagerEconomicParticipant(FARMER, "farmer", 0),
                new VillagerEconomicParticipant(BUTCHER, "butcher", 0));

        List<VillagerTradeSettlement> settlements = new VillagerTradeService().planTrades(
                edges, participants, CONFIG, 0.05, 3, new Random(1));

        assertEquals(3, settlements.size());
    }

    @Test
    void flatCoronaEdgeLetsCommonerTradeWithoutGdp() {
        UUID commoner = UUID.fromString("00000000-0000-0000-0000-000000000003");
        List<VillagerTradeEdge> edges = List.of(VillagerTradeEdge.flatCorona("none", "farmer", 0.2));
        List<VillagerEconomicParticipant> participants = List.of(
                new VillagerEconomicParticipant(commoner, "none", 0),
                new VillagerEconomicParticipant(FARMER, "farmer", 0));

        List<VillagerTradeSettlement> settlements = new VillagerTradeService().planTrades(
                edges, participants, CONFIG, 0.05, 1, new Random(1));

        assertEquals(1, settlements.size());
        assertEquals(0.2, settlements.getFirst().payment(), 1e-9);
    }

    @Test
    void skipsEdgeWhenSellerProfessionAbsent() {
        List<VillagerTradeEdge> edges = List.of(VillagerTradeEdge.spendPercent("farmer", "butcher", 0.5));
        List<VillagerEconomicParticipant> participants =
                List.of(new VillagerEconomicParticipant(FARMER, "farmer", 0));

        List<VillagerTradeSettlement> settlements = new VillagerTradeService().planTrades(
                edges, participants, CONFIG, 0.05, 1, new Random(1));

        assertTrue(settlements.isEmpty());
    }
}
