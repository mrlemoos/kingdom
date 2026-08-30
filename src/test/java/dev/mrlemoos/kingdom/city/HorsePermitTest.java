package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The horse permit: who a saddled horse answers to, and who else may lay a hand on it. */
class HorsePermitTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PRINCE = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID RIDER = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID FOREIGN_KING = UUID.fromString("00000000-0000-0000-0000-0000000000a5");
    private static final UUID HORSE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SECOND_HORSE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private KingdomService kingdomService;
    private CityService cityService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        cityService = new CityService(kingdomService);
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(PRINCE, "northmarch");
        kingdomService.joinKingdom(RIDER, "northmarch");
        kingdomService.joinKingdom(STRANGER, "northmarch");
        kingdomService.joinKingdom(FOREIGN_KING, "southmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(PRINCE, NobleRank.PRINCE, TitleStyle.MASCULINE);
        kingdomService.assignTitle(FOREIGN_KING, NobleRank.KING, TitleStyle.MASCULINE);
    }

    @Test
    void saddlingAHorseEntersItOnTheRegister() {
        assertInstanceOf(CityResult.Success.class, cityService.grantHorsePermit("northmarch", HORSE, RIDER));

        assertEquals(Optional.of(RIDER), cityService.horseOwner(HORSE));
        assertEquals(1, cityService.horsePermitCount("northmarch", RIDER));
    }

    @Test
    void aForeignerHoldsNoHorsePermitInThisRealm() {
        assertInstanceOf(
                CityResult.Failure.class, cityService.grantHorsePermit("northmarch", HORSE, FOREIGN_KING));
        assertTrue(cityService.horseOwner(HORSE).isEmpty());
    }

    @Test
    void aHorseAnswersToWhoeverSaddledItLast() {
        cityService.grantHorsePermit("northmarch", HORSE, RIDER);
        cityService.grantHorsePermit("northmarch", HORSE, STRANGER);

        assertEquals(Optional.of(STRANGER), cityService.horseOwner(HORSE));
        assertEquals(0, cityService.horsePermitCount("northmarch", RIDER));
    }

    @Test
    void revokingTakesEveryHorseFromAHolder() {
        cityService.grantHorsePermit("northmarch", HORSE, RIDER);
        cityService.grantHorsePermit("northmarch", SECOND_HORSE, RIDER);

        assertEquals(2, cityService.revokeHorsePermits(RIDER));
        assertTrue(cityService.horseOwner(HORSE).isEmpty());
        assertEquals(0, cityService.revokeHorsePermits(RIDER));
    }

    @Test
    void anUnclaimedHorseIsAnybodysToRide() {
        assertTrue(HorsePermitEnforcer.mayHandle(null, STRANGER, null, false));
    }

    @Test
    void theOwnerMayAlwaysHandleTheirOwnHorse() {
        assertTrue(HorsePermitEnforcer.mayHandle(RIDER, RIDER, null, true));
    }

    @Test
    void aStrangerMayNot() {
        assertFalse(HorsePermitEnforcer.mayHandle(RIDER, STRANGER, NobleRank.KNIGHT, true));
    }

    @Test
    void theCrownAndItsPrincesMayCommandeerASubjectsHorse() {
        assertTrue(HorsePermitEnforcer.mayHandle(RIDER, KING, NobleRank.KING, true));
        assertTrue(HorsePermitEnforcer.mayHandle(RIDER, PRINCE, NobleRank.PRINCE, true));
    }

    @Test
    void aForeignCrownMayNot() {
        assertFalse(HorsePermitEnforcer.mayHandle(RIDER, FOREIGN_KING, NobleRank.KING, false));
    }
}
