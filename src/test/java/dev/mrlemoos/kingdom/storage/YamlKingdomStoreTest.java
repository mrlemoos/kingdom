package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.parliament.Bill;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.model.parliament.PreparedPublicWork;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import dev.mrlemoos.kingdom.parliament.DivisionBloc;
import dev.mrlemoos.kingdom.parliament.DivisionBlocKind;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreTest {

    @Test
    void roundTripPreservesTeleportPlaces() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.putTeleport(TeleportPlace.of("mob_farm", "world", 120.5, 64.0, -30.5, 90f, 0f));
        kingdom.putTeleport(TeleportPlace.of("spawn", "world_nether", 0.0, 70.0, 0.0, 180f, -5f));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeTeleports(config, "kingdoms.northmarch.teleports", kingdom.getTeleportsView());

        Map<String, TeleportPlace> loaded = YamlKingdomStore.readTeleports(
                config.getConfigurationSection("kingdoms.northmarch.teleports"));

        assertEquals(2, loaded.size());
        TeleportPlace farm = loaded.get("mob_farm");
        assertEquals("world", farm.worldName());
        assertEquals(120.5, farm.x(), 1e-9);
        assertEquals(64.0, farm.y(), 1e-9);
        assertEquals(-30.5, farm.z(), 1e-9);
        assertEquals(90f, farm.yaw(), 1e-9);
        assertEquals(0f, farm.pitch(), 1e-9);

        TeleportPlace spawn = loaded.get("spawn");
        assertEquals("world_nether", spawn.worldName());
        assertEquals(180f, spawn.yaw(), 1e-9);
        assertEquals(-5f, spawn.pitch(), 1e-9);
    }

    @Test
    void readTeleportsReturnsEmptyMapWhenSectionMissing() {
        Map<String, TeleportPlace> loaded = YamlKingdomStore.readTeleports(null);
        assertEquals(Map.of(), loaded);
    }

    @Test
    void writeTeleportsHandlesEmptyMap() {
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeTeleports(config, "kingdoms.northmarch.teleports", new HashMap<>());
        assertEquals(Map.of(), YamlKingdomStore.readTeleports(
                config.getConfigurationSection("kingdoms.northmarch.teleports")));
    }

    @Test
    void roundTripPreservesParliamentState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentSites().setCommons(ChamberSite.of("world", 10, 64, 20));
        kingdom.getParliamentSites().setLords(ChamberSite.of("world", 30, 70, 40));
        kingdom.getParliamentSites().setRegistrar(RegistrarSite.of("world", 5, 64, 5));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertEquals(10, loaded.getParliamentSites().commons().orElseThrow().x(), 1e-9);
        assertEquals(30, loaded.getParliamentSites().lords().orElseThrow().x(), 1e-9);
        assertEquals(5, loaded.getParliamentSites().registrar().orElseThrow().blockX());
    }

    @Test
    void roundTripPreservesKingdomFlagWithPatterns() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.setFlag(new KingdomFlag(
                "RED_BANNER",
                java.util.List.of(
                        new KingdomFlag.Layer("minecraft:stripe_middle", "WHITE"),
                        new KingdomFlag.Layer("minecraft:border", "BLACK"))));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        KingdomFlag flag = loaded.getFlag().orElseThrow();
        assertEquals("RED_BANNER", flag.baseMaterial());
        assertEquals(2, flag.layers().size());
        assertEquals("minecraft:stripe_middle", flag.layers().get(0).patternId());
        assertEquals("WHITE", flag.layers().get(0).colour());
        assertEquals("minecraft:border", flag.layers().get(1).patternId());
        assertEquals("BLACK", flag.layers().get(1).colour());
    }

    @Test
    void hansardRecordsSurviveARestartMidSession() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentState().addHansardRecord(new HansardRecord(
                "Finance Act 2026",
                "fiscal",
                true,
                3,
                1,
                2,
                8,
                java.util.List.of(new DivisionBloc(DivisionBlocKind.PARTY, "Northern Union", "&9", 2, 0, 1)),
                142L));
        kingdom.getParliamentState().addHansardRecord(new HansardRecord(
                "Budget Act 2026", "budget", false, 1, 4, 0, 8, java.util.List.of(), 143L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        java.util.List<HansardRecord> records = loaded.getParliamentState().hansardView();
        assertEquals(2, records.size());
        HansardRecord first = records.get(0);
        assertEquals("Finance Act 2026", first.title());
        assertEquals("fiscal", first.business());
        assertTrue(first.carried());
        assertEquals(3, first.aye());
        assertEquals(1, first.nay());
        assertEquals(2, first.abstain());
        assertEquals(8, first.electorate());
        assertEquals(142L, first.decidedOnMcDay());
        assertEquals(1, first.blocs().size());
        assertEquals(DivisionBlocKind.PARTY, first.blocs().get(0).kind());
        assertEquals("Northern Union", first.blocs().get(0).label());
        assertEquals("&9", first.blocs().get(0).colour());
        assertEquals(2, first.blocs().get(0).aye());
        assertEquals(1, first.blocs().get(0).abstain());
        assertEquals("Budget Act 2026", records.get(1).title());
    }

    @Test
    void anOpenPublicWorkBillSurvivesARestart() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID premier = UUID.randomUUID();
        kingdom.getParliamentState().setPreparedPublicWork(
                new PreparedPublicWork(WealthBlockType.BEACON, "world", 10, 64, 20));
        kingdom.getParliamentState().setCurrentBill(new Bill(
                "northmarch-1",
                "northmarch",
                BillType.SPEND_PUBLIC_WORK,
                "Supply Act (Public work)",
                BillState.TABLED,
                premier,
                new BillPayload.SpendPublicWork(WealthBlockType.BEACON, "world", 10, 64, 20, 500.0),
                1_700_000_000_000L));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        PreparedPublicWork prepared = loaded.getParliamentState().preparedPublicWork().orElseThrow();
        assertEquals(WealthBlockType.BEACON, prepared.estateType());
        assertEquals(10, prepared.x());

        Bill restored = loaded.getParliamentState().currentBill().orElseThrow();
        assertEquals(BillType.SPEND_PUBLIC_WORK, restored.type());
        BillPayload.SpendPublicWork payload = (BillPayload.SpendPublicWork) restored.payload();
        assertEquals(WealthBlockType.BEACON, payload.estateType());
        assertEquals(500.0, payload.cost(), 1e-9);
        assertEquals(64, payload.y());
    }

    @Test
    void anOpenReferendumSurvivesARestartMidPollingWindow() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID crown = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Bill referendum = new Bill(
                "northmarch-1",
                "northmarch",
                BillType.REFERENDUM,
                "Should the realm keep the mint at Eastgate?",
                BillState.DIVISION_OPEN,
                crown,
                new BillPayload.Referendum(
                        "Should the realm keep the mint at Eastgate?", crown),
                1_700_000_000_000L);
        referendum.recordVote(member, dev.mrlemoos.kingdom.model.parliament.VoteChoice.AYE);
        referendum.setDivisionClosesOnMcDay(144L);
        kingdom.getParliamentState().setCurrentBill(referendum);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        Bill restored = loaded.getParliamentState().currentBill().orElseThrow();
        assertEquals(BillType.REFERENDUM, restored.type());
        assertEquals(144L, restored.divisionClosesOnMcDay().orElseThrow());
        assertEquals(1, restored.votesView().size());
        assertEquals(
                "Should the realm keep the mint at Eastgate?",
                ((BillPayload.Referendum) restored.payload()).question());
        assertEquals(
                crown,
                ((BillPayload.Referendum) restored.payload()).calledBy());
    }

    @Test
    void roundTripPreservesLastPremierQuestionsDay() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentState().recordPremierQuestions(142L);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertEquals(142L, loaded.getParliamentState().lastPremierQuestionsMcDay().orElseThrow());
    }

    @Test
    void roundTripLeavesQuestionsUncalledWhenNeverCalled() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        loaded.getParliamentState().recordPremierQuestions(5L);
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertTrue(loaded.getParliamentState().lastPremierQuestionsMcDay().isEmpty());
    }

    @Test
    void roundTripPreservesSeatReturns() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        MpSeat player = kingdom.getElectionState().seat(1).orElseThrow();
        player.assignPlayer(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        player.setReturnCount(12);
        MpSeat villager = kingdom.getElectionState().seat(2).orElseThrow();
        villager.assignVillager("farmer", null);
        villager.setReturnCount(6);
        MpSeat backfill = kingdom.getElectionState().seat(3).orElseThrow();
        backfill.assignVillager("none", null);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertEquals(12, loaded.getElectionState().seat(1).orElseThrow().returnCount().orElseThrow());
        assertEquals(6, loaded.getElectionState().seat(2).orElseThrow().returnCount().orElseThrow());
        assertTrue(loaded.getElectionState().seat(3).orElseThrow().returnCount().isEmpty());
    }

    @Test
    void roundTripPreservesNominationDeclarations() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        UUID alice = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
        UUID bob = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
        kingdom.getElectionState().election().openGeneral(9_000L);
        kingdom.getElectionState()
                .election()
                .nominate(alice, 1L, CandidateDeclaration.of("Cheaper bread", "Reform", "red"));
        kingdom.getElectionState().election().nominate(bob, 2L);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        CandidateDeclaration declared = loaded.getElectionState().election().declaration(alice);
        assertEquals("Cheaper bread", declared.manifesto());
        assertEquals("Reform", declared.partyName());
        assertEquals("&c", declared.partyColour());
        assertTrue(loaded.getElectionState().election().declaration(bob).isBlank());
    }

    @Test
    void roundTripPreservesSeatDeclarations() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        MpSeat player = kingdom.getElectionState().seat(1).orElseThrow();
        player.assignPlayer(UUID.fromString("00000000-0000-0000-0000-0000000000e3"));
        player.setDeclaration(CandidateDeclaration.of("Cheaper bread", "Reform", "red"));
        MpSeat villager = kingdom.getElectionState().seat(2).orElseThrow();
        villager.assignVillager("farmer", null);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        CandidateDeclaration declared =
                loaded.getElectionState().seat(1).orElseThrow().declaration().orElseThrow();
        assertEquals("Reform", declared.partyName());
        assertEquals("Cheaper bread", declared.manifesto());
        assertTrue(loaded.getElectionState().seat(2).orElseThrow().declaration().isEmpty());
    }
}
