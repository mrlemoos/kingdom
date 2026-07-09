package dev.mrlemoos.kingdom.war;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.roster.InMemoryStandingRosterStore;
import dev.mrlemoos.kingdom.war.roster.StandingRosterConfig;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the optional {@code StandingRosterService} hook on {@link WarService}: rostered members
 * of either belligerent are auto-mobilised to on-duty Steadfast with hardened service the moment a
 * war bill is enacted.
 */
class WarServiceStandingRosterHookTest {

    private static final UUID ATTACKER_ROSTERED = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID DEFENDER_ROSTERED = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ATTACKER_KNIGHT = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private KingdomService kingdomService;
    private WarService warService;
    private StandingRosterService rosterService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");
        kingdomService.joinKingdom(ATTACKER_ROSTERED, "northmarch");
        kingdomService.joinKingdom(ATTACKER_KNIGHT, "northmarch");
        kingdomService.joinKingdom(DEFENDER_ROSTERED, "southreach");

        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());

        rosterService = new StandingRosterService(
                kingdomService, new InMemoryStandingRosterStore(), StandingRosterConfig.defaults());
        rosterService.appoint("northmarch", NobleRank.KING, ATTACKER_ROSTERED);
        rosterService.appoint("southreach", NobleRank.KING, DEFENDER_ROSTERED);

        warService.setStandingRosterService(rosterService);
    }

    @Test
    void enactingWarBillMobilisesRosteredAttackerMembers() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);

        WarResult result = warService.enactWarBill("northmarch", payload);

        assertInstanceOf(WarResult.Success.class, result);
        assertTrue(rosterService.isOnDuty(ATTACKER_ROSTERED));
        assertTrue(rosterService.hasHardenedService(ATTACKER_ROSTERED));
    }

    @Test
    void enactingWarBillMobilisesRosteredDefenderMembers() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);

        warService.enactWarBill("northmarch", payload);

        assertTrue(rosterService.isOnDuty(DEFENDER_ROSTERED));
        assertTrue(rosterService.hasHardenedService(DEFENDER_ROSTERED));
    }

    @Test
    void enactingWarBillDoesNotMobiliseNonRosterKnight() {
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);

        warService.enactWarBill("northmarch", payload);

        assertFalse(rosterService.isOnDuty(ATTACKER_KNIGHT));
    }

    @Test
    void warServiceWithoutStandingRosterServiceStillEnactsWarBill() {
        WarService plainWarService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        plainWarService.setConfig(WarConfig.on());
        BillPayload.War payload = new BillPayload.War(
                "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3);

        WarResult result = plainWarService.enactWarBill("northmarch", payload);

        assertInstanceOf(WarResult.Success.class, result);
    }
}
