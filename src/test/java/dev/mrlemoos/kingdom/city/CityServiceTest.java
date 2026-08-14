package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CityServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRINCE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRISONER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID FOREIGN_PRINCE = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-000000000006");

    private static final CapitalLocation CAPITAL = new CapitalLocation("world", 10.5, 64.0, -20.5, 90.0f, 0.0f);

    private KingdomService kingdomService;
    private Set<UUID> prisoners;
    private CityService cityService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        prisoners = new HashSet<>();
        cityService = new CityService(kingdomService, prisoners::contains);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.joinKingdom(CITIZEN, "northmarch");
        kingdomService.joinKingdom(PRISONER, "northmarch");
        kingdomService.joinKingdom(OPERATOR, "northmarch");
        kingdomService.joinKingdom(FOREIGN_PRINCE, "southmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        kingdomService.assignTitle(FOREIGN_PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
    }

    private void setCapital() {
        cityService.setCapital("northmarch", NobleRank.KING, CAPITAL);
    }

    @Test
    void enforcementIsOffUntilCapitalIsSet() {
        assertFalse(cityService.enforcementActive("northmarch"));
        assertTrue(cityService.mayBuild("northmarch", CITIZEN));
        assertTrue(cityService.mayBuild("northmarch", FOREIGN_PRINCE));

        setCapital();

        assertTrue(cityService.enforcementActive("northmarch"));
        assertFalse(cityService.mayBuild("northmarch", CITIZEN));
    }

    @Test
    void enforcementIsOffForUnknownKingdom() {
        assertFalse(cityService.enforcementActive("nowhere"));
        assertTrue(cityService.mayBuild("nowhere", CITIZEN));
    }

    @Test
    void clearingTheCapitalTurnsEnforcementOff() {
        setCapital();

        assertInstanceOf(CityResult.Success.class, cityService.clearCapital("northmarch", NobleRank.KING));
        assertFalse(cityService.enforcementActive("northmarch"));
    }

    @Test
    void onlyRoyaltyMaySetTheCapital() {
        assertInstanceOf(CityResult.Failure.class, cityService.setCapital("northmarch", NobleRank.KNIGHT, CAPITAL));
        assertFalse(cityService.enforcementActive("northmarch"));
    }

    @Test
    void memberWithPermitMayBuild() {
        setCapital();

        assertInstanceOf(CityResult.Success.class, cityService.grantPermit("northmarch", CITIZEN));
        assertTrue(cityService.hasPermit("northmarch", CITIZEN));
        assertTrue(cityService.mayBuild("northmarch", CITIZEN));
    }

    @Test
    void grantIsIdempotent() {
        setCapital();

        cityService.grantPermit("northmarch", CITIZEN);
        assertInstanceOf(CityResult.Success.class, cityService.grantPermit("northmarch", CITIZEN));
        assertTrue(cityService.mayBuild("northmarch", CITIZEN));
    }

    @Test
    void foreignerIsDeniedAndCannotBeGranted() {
        setCapital();

        assertInstanceOf(CityResult.Failure.class, cityService.grantPermit("northmarch", FOREIGN_PRINCE));
        assertFalse(cityService.hasPermit("northmarch", FOREIGN_PRINCE));
        assertFalse(cityService.mayBuild("northmarch", FOREIGN_PRINCE));
    }

    @Test
    void prisonerCannotBeGrantedAPermit() {
        setCapital();
        prisoners.add(PRISONER);

        assertInstanceOf(CityResult.Failure.class, cityService.grantPermit("northmarch", PRISONER));
        assertFalse(cityService.mayBuild("northmarch", PRISONER));
    }

    @Test
    void monarchAndPrinceAreExemptInTheirOwnKingdom() {
        setCapital();

        assertTrue(cityService.mayBuild("northmarch", KING));
        assertTrue(cityService.mayBuild("northmarch", PRINCE));
    }

    @Test
    void princeOfAnotherKingdomIsDenied() {
        setCapital();

        assertFalse(cityService.mayBuild("northmarch", FOREIGN_PRINCE));
        assertFalse(cityService.mayBuild("northmarch", UUID.randomUUID()));
    }

    @Test
    void operatorIsNotExempt() {
        setCapital();

        assertFalse(cityService.mayBuild("northmarch", OPERATOR, NobleRank.KNIGHT, true, true));
    }

    @Test
    void revokeRemovesThePermit() {
        setCapital();
        cityService.grantPermit("northmarch", CITIZEN);

        assertInstanceOf(CityResult.Success.class, cityService.revokePermit("northmarch", CITIZEN));
        assertFalse(cityService.hasPermit("northmarch", CITIZEN));
        assertFalse(cityService.mayBuild("northmarch", CITIZEN));
        assertInstanceOf(CityResult.Failure.class, cityService.revokePermit("northmarch", CITIZEN));
    }

    @Test
    void aCapitalAloneStandsNoCrier() {
        setCapital();

        assertTrue(cityService.crierStand("northmarch").isEmpty());
        assertFalse(cityService.hasSeparateCrierStand("northmarch"));
    }

    @Test
    void crownMaySiteTheCrierAwayFromTheCapital() {
        setCapital();
        CapitalLocation stand = new CapitalLocation("world", 20.0, 65.0, -30.0, 180.0f, 0.0f);

        assertInstanceOf(
                CityResult.Success.class,
                cityService.setTownCrierStand("northmarch", NobleRank.KING, stand));
        assertTrue(cityService.hasSeparateCrierStand("northmarch"));
        assertEquals(stand, cityService.crierStand("northmarch").orElseThrow());
        assertEquals(CAPITAL, cityService.capital("northmarch").orElseThrow());
    }

    @Test
    void clearingTheCrierStandDismissesTheCrier() {
        setCapital();
        CapitalLocation stand = new CapitalLocation("world", 20.0, 65.0, -30.0, 180.0f, 0.0f);
        cityService.setTownCrierStand("northmarch", NobleRank.KING, stand);

        assertInstanceOf(
                CityResult.Success.class, cityService.clearTownCrierStand("northmarch", NobleRank.KING));
        assertFalse(cityService.hasSeparateCrierStand("northmarch"));
        assertTrue(cityService.crierStand("northmarch").isEmpty());
        assertInstanceOf(
                CityResult.Failure.class, cityService.clearTownCrierStand("northmarch", NobleRank.KING));
    }

    @Test
    void crierStandNeedsACapitalFirst() {
        CapitalLocation stand = new CapitalLocation("world", 20.0, 65.0, -30.0, 180.0f, 0.0f);

        assertInstanceOf(
                CityResult.Failure.class,
                cityService.setTownCrierStand("northmarch", NobleRank.KING, stand));
    }

    @Test
    void dissolvingTheCapitalClearsTheCrierStand() {
        setCapital();
        cityService.setTownCrierStand(
                "northmarch", NobleRank.KING, new CapitalLocation("world", 20.0, 65.0, -30.0, 0f, 0f));

        cityService.clearCapital("northmarch", NobleRank.KING);

        assertTrue(cityService.crierStand("northmarch").isEmpty());
        assertFalse(cityService.hasSeparateCrierStand("northmarch"));
    }
}
