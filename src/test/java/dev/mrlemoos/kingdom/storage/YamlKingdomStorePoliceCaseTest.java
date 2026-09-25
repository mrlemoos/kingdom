package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PoliceCase;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SavedSpawn;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.police.ArrestRewardService;
import dev.mrlemoos.kingdom.police.MechanicalJusticeConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.police.PrisonSpawnPort;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Open trials and prison confinements survive a restart. */
class YamlKingdomStorePoliceCaseTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONVICT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ACCUSED = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID LATER = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private Map<UUID, SavedSpawn> spawnStore;
    private final java.util.Set<UUID> offline = new java.util.HashSet<>();

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        spawnStore = new HashMap<>();
        spawnStore.put(CONVICT, SavedSpawn.of("world", 100, 70, 200));

        kingdomService.createKingdom("northmarch", "Northmarch");
        for (UUID member : new UUID[] {KING, CONVICT, CONSTABLE, JUDGE, ACCUSED, LATER}) {
            kingdomService.joinKingdom(member, "northmarch");
        }
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(CONVICT, NobleRank.DUKE, TitleStyle.FEMININE);
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 2, new PrisonCellLocation("world", 4, 64, 0));
        policeService.setCourt(
                "northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);
    }

    @Test
    void confinementAndOpenTrialSurviveARestart() {
        PoliceTrialService before = newTrialService();
        arrest(before, CONVICT);
        before.sentence("northmarch", JUDGE, CONVICT, SentenceType.PRISON, 0, 15);
        before.labour(CONVICT, 1_000L, 5, 0.5);
        arrest(before, ACCUSED);
        long endsAtMs = before.prisonConfinement(CONVICT).orElseThrow().endsAtMs();
        String openCaseId = before.findOpenCase("northmarch", ACCUSED).orElseThrow().id();

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writePoliceCases(
                config, "kingdoms.northmarch.police.open-cases", before.openCasesView());
        YamlKingdomStore.writeConfinements(
                config, "kingdoms.northmarch.police.confinements", before.confinementsView());

        PoliceTrialService after = newTrialService();
        after.restore(
                YamlKingdomStore.readPoliceCases(
                        config.getConfigurationSection("kingdoms.northmarch.police.open-cases"),
                        "northmarch"),
                YamlKingdomStore.readConfinements(
                        config.getConfigurationSection("kingdoms.northmarch.police.confinements"),
                        "northmarch"));

        PoliceCase open = after.findOpenCase("northmarch", ACCUSED).orElseThrow();
        assertEquals(openCaseId, open.id());
        assertEquals(Optional.of(CONSTABLE), open.arrestingConstableId());

        assertTrue(after.isUnderPrisonSentence(CONVICT));
        assertTrue(after.isKingdomTeleportBlocked(CONVICT));
        assertFalse(after.isParliamentEligible(CONVICT));
        assertEquals(endsAtMs, after.prisonConfinement(CONVICT).orElseThrow().endsAtMs());
        assertEquals(5_000L, after.prisonConfinement(CONVICT).orElseThrow().labouredMs());
        assertEquals(15 * 60_000L, after.prisonConfinement(CONVICT).orElseThrow().sentenceMs());

        // The restored convict still holds cell 1, so the next prisoner goes to cell 2.
        after.sentence("northmarch", JUDGE, ACCUSED, SentenceType.PRISON, 0, 15);
        assertEquals(2, after.prisonConfinement(ACCUSED).orElseThrow().cellSlot());

        // A later arrest must not reuse a restored case id.
        arrest(after, LATER);
        assertNotEquals(openCaseId, after.findOpenCase("northmarch", LATER).orElseThrow().id());

        spawnStore.remove(CONVICT);
        assertInstanceOf(PoliceResult.Success.class, after.releaseFromPrison(CONVICT));
        assertEquals(100, spawnStore.get(CONVICT).x(), 1e-9);
        var membership = kingdomService.getMembership(CONVICT).orElseThrow();
        assertEquals(NobleRank.DUKE, membership.getRank());
        assertEquals(TitleStyle.FEMININE, membership.getTitleStyle());
    }

    @Test
    void villagerConfinementKeepsItsEconomyFreezeAndNoTeleportBar() {
        UUID villager = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        PoliceTrialService before = newTrialService();
        PoliceCase villagerCase = new PoliceCase(
                "northmarch-case-9", "northmarch", villager, CONSTABLE, "w-9", null, 1L);
        before.restore(java.util.List.of(villagerCase), java.util.List.of());
        before.applyVillagerPrison(villagerCase, 15, false);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeConfinements(config, "c", before.confinementsView());

        PoliceTrialService after = newTrialService();
        after.restore(
                java.util.List.of(),
                YamlKingdomStore.readConfinements(config.getConfigurationSection("c"), "northmarch"));

        assertTrue(after.isVillagerEconomyFrozen(villager));
        assertFalse(after.isKingdomTeleportBlocked(villager));
        assertFalse(after.labour(villager, 1_000L, 5, 0.5).credited());
    }

    @Test
    void aSentenceThatEndsWhileTheConvictIsOfflineWaitsForThemToReturn() {
        PoliceTrialService trialService = newTrialService();
        arrest(trialService, CONVICT);
        trialService.sentence("northmarch", JUDGE, CONVICT, SentenceType.PRISON, 0, 15);
        long afterSentence = trialService.prisonConfinement(CONVICT).orElseThrow().endsAtMs() + 1;
        spawnStore.remove(CONVICT);
        offline.add(CONVICT);

        assertEquals(0, trialService.releaseDueSentences(afterSentence));
        assertTrue(trialService.isUnderPrisonSentence(CONVICT));

        offline.remove(CONVICT);
        assertEquals(1, trialService.releaseDueSentences(afterSentence));
        assertEquals(100, spawnStore.get(CONVICT).x(), 1e-9);
    }

    private PoliceTrialService newTrialService() {
        EconomyService economy = new EconomyService(100.0);
        PrisonSpawnPort spawnPort = new PrisonSpawnPort() {
            @Override
            public Optional<SavedSpawn> capture(UUID playerId) {
                return Optional.ofNullable(spawnStore.get(playerId));
            }

            @Override
            public void restore(UUID playerId, Optional<SavedSpawn> prior) {
                prior.ifPresent(spawn -> spawnStore.put(playerId, spawn));
            }

            @Override
            public boolean canRestore(UUID playerId) {
                return !offline.contains(playerId);
            }
        };
        return new PoliceTrialService(
                kingdomService,
                policeService,
                justice,
                economy,
                new ArrestRewardService(kingdomService, justice, economy),
                (kingdomId, convictId, vacatedRank) -> {},
                spawnPort);
    }

    private void arrest(PoliceTrialService trialService, UUID suspect) {
        justice.openFromActBreach(
                new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN), suspect);
        Warrant pending = justice.findPendingForSuspect("northmarch", suspect).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
        trialService.arrest("northmarch", CONSTABLE, suspect);
    }
}
