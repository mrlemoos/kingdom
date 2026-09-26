package dev.mrlemoos.kingdom.honours;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchConfig;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwornRoleAppointmentsTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID FOREIGNER = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private KingdomService kingdomService;
    private ChurchService churchService;
    private Set<UUID> prisoners;
    private SwornRoleAppointments appointments;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        PoliceService policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        prisoners = new HashSet<>();
        churchService = new ChurchService(
                kingdomService, policeService, prisoners::contains, () -> 100L, new ChurchConfig());
        appointments = new SwornRoleAppointments(policeService, churchService);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUBJECT, "northmarch");
        kingdomService.joinKingdom(FOREIGNER, "southmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
    }

    private SwornRoleAppointments.Outcome toggle(SwornRole role) {
        return appointments.toggle("northmarch", KING, NobleRank.KING, SUBJECT, role);
    }

    @Test
    void theSwordSwearsAConstableAndRefusesJudgeUntilTheConstableIsUnsworn() {
        SwornRoleAppointments.Outcome constable = toggle(SwornRole.CONSTABLE);
        assertTrue(constable.success(), constable.message());
        assertTrue(constable.sworn());
        assertTrue(appointments.holds("northmarch", SUBJECT, SwornRole.CONSTABLE));

        SwornRoleAppointments.Outcome judge = toggle(SwornRole.JUDGE);
        assertFalse(judge.success());
        assertEquals("A constable cannot also serve as judge.", judge.message());
        assertFalse(appointments.holds("northmarch", SUBJECT, SwornRole.JUDGE));

        SwornRoleAppointments.Outcome unsworn = toggle(SwornRole.CONSTABLE);
        assertTrue(unsworn.success(), unsworn.message());
        assertFalse(unsworn.sworn());
        assertFalse(appointments.holds("northmarch", SUBJECT, SwornRole.CONSTABLE));

        assertTrue(toggle(SwornRole.JUDGE).success());
        assertTrue(appointments.holds("northmarch", SUBJECT, SwornRole.JUDGE));
    }

    @Test
    void thePriesthoodIsRefusedWhileAPoliceRoleIsHeld() {
        toggle(SwornRole.JUDGE);

        SwornRoleAppointments.Outcome priest = toggle(SwornRole.PRIEST);
        assertFalse(priest.success());
        assertEquals("A constable or judge cannot also serve as priest.", priest.message());

        toggle(SwornRole.JUDGE);
        assertTrue(toggle(SwornRole.PRIEST).success());
        assertTrue(appointments.holds("northmarch", SUBJECT, SwornRole.PRIEST));
        assertFalse(toggle(SwornRole.CONSTABLE).success());
    }

    @Test
    void anUncrownedMonarchIsRefusedWhereTheGateApplies() {
        churchService.setChurch("northmarch", NobleRank.KING, new ChurchSite("world", 0, 64, 0, 0f, 0f));
        churchService.consecrate("northmarch", Celebrant.CLERIC);

        SwornRoleAppointments.Outcome refused = toggle(SwornRole.CONSTABLE);
        assertFalse(refused.success());
        assertTrue(refused.message().contains("not been crowned"), refused.message());
        assertFalse(appointments.holds("northmarch", SUBJECT, SwornRole.CONSTABLE));

        churchService.crown("northmarch", Celebrant.CLERIC, KING);
        assertTrue(toggle(SwornRole.CONSTABLE).success());
    }

    @Test
    void aRealmWithoutAConsecratedChurchIsNotGated() {
        assertTrue(toggle(SwornRole.CONSTABLE).success());
    }

    @Test
    void theOperatorsEscapeHatchIsNotGated() {
        churchService.setChurch("northmarch", NobleRank.KING, new ChurchSite("world", 0, 64, 0, 0f, 0f));
        churchService.consecrate("northmarch", Celebrant.CLERIC);

        assertTrue(appointments.swear("northmarch", null, NobleRank.KING, SUBJECT, SwornRole.CONSTABLE).success());
    }

    @Test
    void aPrisonerCannotBeSworn() {
        prisoners.add(SUBJECT);

        SwornRoleAppointments.Outcome refused = toggle(SwornRole.CONSTABLE);
        assertFalse(refused.success());
        assertTrue(refused.message().contains("prison"), refused.message());
    }

    @Test
    void onlySubjectsOfTheRealmAreSworn() {
        assertFalse(appointments.toggle("northmarch", KING, NobleRank.KING, FOREIGNER, SwornRole.CONSTABLE).success());
        assertFalse(appointments.toggle("northmarch", KING, NobleRank.KING, FOREIGNER, SwornRole.PRIEST).success());
    }

    @Test
    void onlyTheKingOrQueenSwears() {
        assertFalse(appointments.toggle("northmarch", SUBJECT, NobleRank.PRINCE, KING, SwornRole.JUDGE).success());
    }

    @Test
    void heldRolesAreReadForTheWindow() {
        toggle(SwornRole.CONSTABLE);
        assertEquals(Set.of(SwornRole.CONSTABLE), appointments.heldBy("northmarch", SUBJECT));
    }
}
