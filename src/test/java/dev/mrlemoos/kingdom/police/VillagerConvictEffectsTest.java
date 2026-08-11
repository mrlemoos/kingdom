package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PoliceCase;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerConvictEffectsTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID VILLAGER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SPEAKER_VILLAGER = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private PoliceTrialService trialService;
    private List<UUID> vacated;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        vacated = new ArrayList<>();
        trialService = new PoliceTrialService(
                kingdomService,
                policeService,
                justice,
                new EconomyService(100.0),
                new ArrestRewardService(kingdomService, justice, new EconomyService(100.0)),
                (kingdomId, convictId, vacatedRank) -> vacated.add(convictId),
                new PrisonSpawnPort() {
                    @Override
                    public Optional<dev.mrlemoos.kingdom.model.police.SavedSpawn> capture(UUID playerId) {
                        return Optional.empty();
                    }

                    @Override
                    public void restore(
                            UUID playerId,
                            Optional<dev.mrlemoos.kingdom.model.police.SavedSpawn> prior) {}
                });

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.joinKingdom(JUDGE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService
                .getKingdom("northmarch")
                .orElseThrow()
                .getParliamentState()
                .setSpeakerVillagerEntityId(SPEAKER_VILLAGER);
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt(
                "northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);
    }

    @Test
    void villagerSpeakerIsWarrantImmune() {
        assertTrue(VillagerWarrantPolicy.isImmune(
                SPEAKER_VILLAGER,
                kingdomService.getKingdom("northmarch").orElseThrow().getParliamentState().speakerVillagerEntityId()));
        assertFalse(VillagerWarrantPolicy.isImmune(
                VILLAGER,
                kingdomService.getKingdom("northmarch").orElseThrow().getParliamentState().speakerVillagerEntityId()));
    }

    @Test
    void justiceRejectsWarrantOnVillagerSpeaker() {
        justice.setSpeakerVillagerResolver(
                kingdomId -> kingdomService
                        .getKingdom(kingdomId)
                        .flatMap(k -> k.getParliamentState().speakerVillagerEntityId()));

        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        PoliceResult result = justice.openFromActBreach(breach, SPEAKER_VILLAGER);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(result.message().toLowerCase().contains("immun"));
    }

    @Test
    void ordinaryVillagerPrisonFreezesEconomyWithoutTeleportBan() {
        openApproveAndArrestVillager(VILLAGER);
        PoliceCase open = trialService.findOpenCase("northmarch", VILLAGER).orElseThrow();

        PoliceResult result = trialService.applyVillagerPrison(open, 15, false);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertTrue(trialService.isVillagerEconomyFrozen(VILLAGER));
        assertFalse(trialService.isKingdomTeleportBlocked(VILLAGER));
        assertTrue(vacated.isEmpty());
        assertEquals(
                SentenceType.PRISON,
                trialService.lastClosedSentence("northmarch", VILLAGER).orElseThrow());
    }

    @Test
    void officeVillagerPrisonVacatesSeatAndFreezesEconomy() {
        openApproveAndArrestVillager(VILLAGER);
        PoliceCase open = trialService.findOpenCase("northmarch", VILLAGER).orElseThrow();

        PoliceResult result = trialService.applyVillagerPrison(open, 15, true);

        assertInstanceOf(PoliceResult.Success.class, result);
        assertEquals(1, vacated.size());
        assertEquals(VILLAGER, vacated.get(0));
        assertTrue(trialService.isVillagerEconomyFrozen(VILLAGER));
    }

    @Test
    void wantedNametagNeverAppliesToVillagers() {
        assertFalse(WantedNametagPolicy.shouldShow(false, true, true));
    }

    private void openApproveAndArrestVillager(UUID villagerId) {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, villagerId);
        Warrant pending = justice.findPendingForSuspect("northmarch", villagerId).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
        trialService.arrest("northmarch", CONSTABLE, villagerId);
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
