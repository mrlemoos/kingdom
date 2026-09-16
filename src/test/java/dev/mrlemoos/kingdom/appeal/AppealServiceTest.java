package dev.mrlemoos.kingdom.appeal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.police.ActBreach;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.police.MechanicalJusticeConfig;
import dev.mrlemoos.kingdom.police.MechanicalJusticeService;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppealServiceTest {
    private static final UUID KING = UUID.randomUUID();
    private static final UUID JUDGE = UUID.randomUUID();
    private static final UUID CONSTABLE = UUID.randomUUID();
    private static final UUID PRISONER = UUID.randomUUID();
    private long now = 1_000_000L;
    private PoliceTrialService trials;
    private AppealService appeals;

    @BeforeEach void setUp() {
        KingdomService kingdoms = new KingdomService();
        PoliceService police = new PoliceService(kingdoms, PoliceConfig.defaults());
        MechanicalJusticeService justice = new MechanicalJusticeService(kingdoms, police, MechanicalJusticeConfig.enabled());
        trials = new PoliceTrialService(kingdoms, police, justice, new EconomyService(100));
        kingdoms.createKingdom("north", "North");
        for (UUID id : new UUID[] {KING, JUDGE, CONSTABLE, PRISONER}) kingdoms.joinKingdom(id, "north");
        kingdoms.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdoms.assignTitle(JUDGE, NobleRank.KNIGHT, TitleStyle.MASCULINE);
        kingdoms.assignTitle(CONSTABLE, NobleRank.KNIGHT, TitleStyle.MASCULINE);
        police.appointJudge("north", NobleRank.KING, JUDGE);
        police.appointConstable("north", NobleRank.KING, CONSTABLE);
        police.setCell("north", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        police.setCourt("north", NobleRank.KING, false, new CourtLocation("world", 0, 64, 0));
        justice.openFromActBreach(new ActBreach("north", "test", ConductKind.BUILD_BAN), PRISONER);
        Warrant warrant = justice.findPendingForSuspect("north", PRISONER).orElseThrow();
        justice.approveWarrant("north", KING, warrant.id());
        trials.arrest("north", CONSTABLE, PRISONER);
        assertInstanceOf(dev.mrlemoos.kingdom.police.PoliceResult.Success.class,
                trials.sentence("north", JUDGE, PRISONER, SentenceType.PRISON, 0, 15));
        appeals = new AppealService(kingdoms, trials, () -> now);
    }

    @Test void petitionThenUpholdLeavesSentence() {
        assertInstanceOf(AppealResult.Success.class, appeals.petition("north", PRISONER));
        assertInstanceOf(AppealResult.Success.class, appeals.uphold("north", NobleRank.KING));
        assertTrue(trials.isUnderPrisonSentence(PRISONER));
        assertTrue(appeals.pendingAppeal("north").isEmpty());
    }

    @Test void commuteHalvesRemainingAndRoundsUpToMinute() {
        appeals.petition("north", PRISONER);
        long original = trials.prisonConfinement(PRISONER).orElseThrow().endsAtMs();
        now = original - 15 * 60_000L;
        appeals.commute("north", NobleRank.KING);
        assertEquals(8 * 60_000L, trials.prisonConfinement(PRISONER).orElseThrow().endsAtMs() - now);
    }

    @Test void pardonReleasesImmediately() {
        appeals.petition("north", PRISONER);
        assertInstanceOf(AppealResult.Success.class, appeals.pardon("north", NobleRank.KING));
        assertFalse(trials.isUnderPrisonSentence(PRISONER));
    }
}
