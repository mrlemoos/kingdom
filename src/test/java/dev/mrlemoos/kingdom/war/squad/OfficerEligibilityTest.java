package dev.mrlemoos.kingdom.war.squad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.muster.MusterConfig;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Domain stub for "officer is a military participant" (see Slice 5.3): a member is eligible to
 * command a squad if they are either on-duty on the Crown's standing roster, or have answered the
 * muster for the currently active war. No Bukkit type is involved — the predicate is a pure
 * function of existing war-domain services.
 */
class OfficerEligibilityTest {

    private static final UUID ON_DUTY_OFFICER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MUSTERED_OFFICER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNINVOLVED_PLAYER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private StandingRosterService rosterService;
    private MusterService musterService;
    private ActiveWar war;

    @BeforeEach
    void setUp() {
        KingdomService kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(ON_DUTY_OFFICER, "northmarch");
        kingdomService.joinKingdom(MUSTERED_OFFICER, "northmarch");
        kingdomService.joinKingdom(UNINVOLVED_PLAYER, "northmarch");

        rosterService = new StandingRosterService(
                kingdomService, new InMemoryStandingRosterStore(), StandingRosterConfig.defaults());
        rosterService.appoint("northmarch", NobleRank.KING, ON_DUTY_OFFICER);

        WarService warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());
        warService.setStandingRosterService(rosterService);
        BillPayload.War payload =
                new BillPayload.War("southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);
        warService.enactWarBill("northmarch", payload);
        war = warService.activeWarFor("northmarch").orElseThrow();

        musterService = new MusterService(warService, kingdomService, () -> 1_700_000_000_000L);
        musterService.setConfig(MusterConfig.on());
        musterService.setStandingRosterService(rosterService);
        musterService.openMuster(war.id());
    }

    @Test
    void anOnDutyStandingRosterMemberIsEligible() {
        Predicate<UUID> eligibility = OfficerEligibility.standingRosterOrMuster(rosterService, musterService, () -> war.id());

        assertTrue(eligibility.test(ON_DUTY_OFFICER));
    }

    @Test
    void aMemberWhoAnsweredTheMusterForTheActiveWarIsEligible() {
        musterService.answer(war.id(), MUSTERED_OFFICER);
        Predicate<UUID> eligibility = OfficerEligibility.standingRosterOrMuster(rosterService, musterService, () -> war.id());

        assertTrue(eligibility.test(MUSTERED_OFFICER));
    }

    @Test
    void aMemberWhoRefusedTheMusterIsIneligible() {
        musterService.refuse(war.id(), MUSTERED_OFFICER);
        Predicate<UUID> eligibility = OfficerEligibility.standingRosterOrMuster(rosterService, musterService, () -> war.id());

        assertFalse(eligibility.test(MUSTERED_OFFICER));
    }

    @Test
    void aMemberWhoIsNeitherOnDutyNorMusteredIsIneligible() {
        Predicate<UUID> eligibility = OfficerEligibility.standingRosterOrMuster(rosterService, musterService, () -> war.id());

        assertFalse(eligibility.test(UNINVOLVED_PLAYER));
    }

    @Test
    void nullActiveWarIdMeansMusterCannotMakeAnyoneEligible() {
        musterService.answer(war.id(), MUSTERED_OFFICER);
        Predicate<UUID> eligibility = OfficerEligibility.standingRosterOrMuster(rosterService, musterService, () -> null);

        assertFalse(eligibility.test(MUSTERED_OFFICER));
    }
}
