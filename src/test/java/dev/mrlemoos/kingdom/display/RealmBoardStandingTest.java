package dev.mrlemoos.kingdom.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RealmBoardStandingTest {

    @Test
    @DisplayName("a guest on foreign dirt has no standing line")
    void aGuestOnForeignDirtHasNoStandingLine() {
        assertTrue(RealmBoardStanding.line(false, Optional.empty(), LoyaltyTier.FAITHFUL).isEmpty());
    }

    @Test
    @DisplayName("a civilian on home soil shows political loyalty")
    void aCivilianOnHomeSoilShowsPoliticalLoyalty() {
        assertEquals(
                Optional.of("Loyalty: Faithful"),
                RealmBoardStanding.line(true, Optional.empty(), LoyaltyTier.FAITHFUL));
        assertEquals(
                Optional.of("Loyalty: Doubtful"),
                RealmBoardStanding.line(true, Optional.empty(), LoyaltyTier.DOUBTFUL));
    }

    @Test
    @DisplayName("an open military track shows morale instead")
    void anOpenMilitaryTrackShowsMoraleInstead() {
        assertEquals(
                Optional.of("Morale: Steadfast"),
                RealmBoardStanding.line(true, Optional.of(MoraleTier.STEADFAST), LoyaltyTier.FAITHFUL));
        assertEquals(
                Optional.of("Morale: Shaken"),
                RealmBoardStanding.line(true, Optional.of(MoraleTier.SHAKEN), LoyaltyTier.DOUBTFUL));
    }

    @Test
    @DisplayName("home soil is the fealty kingdom's dirt")
    void homeSoilIsTheFealtyKingdomsDirt() {
        assertTrue(RealmBoardStanding.isHomeSoil("northmarch", "northmarch", null));
        assertTrue(RealmBoardStanding.isHomeSoil("avalon", null, "avalon"));
        assertFalse(RealmBoardStanding.isHomeSoil("avalon", "northmarch", null));
        assertFalse(RealmBoardStanding.isHomeSoil("avalon", null, "northmarch"));
        assertFalse(RealmBoardStanding.isHomeSoil("avalon", null, null));
    }
}
