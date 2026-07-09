package dev.mrlemoos.kingdom.war.oath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Oath of service ceremony: binds a sworn outsider or an early member to a military obligation.
 * Sworn outsiders additionally register in the {@link SwornOutsiderStore} for a bounded purpose
 * and never gain a Commons seat from the oath alone, regardless of loyalty tier.
 */
class OathServiceTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String KINGDOM_ID = "avalon";
    private static final String PURPOSE = "wartime mercenary service against Castellan";

    private KingdomService kingdomService;
    private LoyaltyService loyaltyService;
    private MoraleService moraleService;
    private InMemorySwornOutsiderStore swornOutsiderStore;
    private OathService oathService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.enabled());
        moraleService = new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled());
        swornOutsiderStore = new InMemorySwornOutsiderStore();
        oathService = new OathService(
                kingdomService, loyaltyService, moraleService, swornOutsiderStore, OathConfig.on());
    }

    @Test
    void swornOutsiderOathOpensFaithfulPoliticalAndSteadfastMilitaryTracks() {
        OathResult result = oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);

        assertInstanceOf(OathResult.Success.class, result);
        OathResult.Success success = (OathResult.Success) result;
        assertEquals(LoyaltyTier.FAITHFUL, success.politicalTier());
        assertEquals(MoraleTier.STEADFAST, success.militaryTier());
        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(PLAYER));
        assertEquals(MoraleTier.STEADFAST, moraleService.tierOf(PLAYER).orElseThrow());
    }

    @Test
    void swornOutsiderOathRegistersTheOutsiderWithKingdomIdAndPurpose() {
        oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);

        SwornOutsider outsider = swornOutsiderStore.find(PLAYER).orElseThrow();
        assertEquals(KINGDOM_ID, outsider.kingdomId());
        assertEquals(PLAYER, outsider.playerId());
        assertEquals(PURPOSE, outsider.purpose());
        assertTrue(oathService.isSwornOutsider(PLAYER));
    }

    @Test
    void swornOutsiderOathNeverGrantsACommonsSeatRegardlessOfTier() {
        OathResult result = oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);

        OathResult.Success success = (OathResult.Success) result;
        assertFalse(success.commonsSeatGranted());
        assertFalse(oathService.isEligibleForCommonsSeat(PLAYER));
    }

    @Test
    void swornOutsiderOathNeverGrantsACommonsSeatEvenAfterAdvancingLoyaltyTierManuallyElsewhere() {
        oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);

        // Even if some future political change (outside this ceremony) raised the outsider's
        // standing, the oath itself never grants a Commons seat — sworn outsiders never gain
        // office or a Commons vote regardless of tier.
        assertFalse(oathService.isEligibleForCommonsSeat(PLAYER));
    }

    @Test
    void memberEarlyOathOpensMilitaryTrackWithoutTouchingPoliticalTierOrRegisteringAsOutsider() {
        kingdomService.createKingdom(KINGDOM_ID, "Avalon");
        kingdomService.joinKingdom(PLAYER, KINGDOM_ID);

        OathResult result = oathService.swearAsMember(PLAYER);

        assertInstanceOf(OathResult.Success.class, result);
        OathResult.Success success = (OathResult.Success) result;
        assertEquals(MoraleTier.STEADFAST, success.militaryTier());
        assertEquals(LoyaltyTier.FAITHFUL, success.politicalTier());
        assertFalse(success.commonsSeatGranted());
        assertFalse(swornOutsiderStore.find(PLAYER).isPresent());
        assertFalse(oathService.isSwornOutsider(PLAYER));
    }

    @Test
    void memberEarlyOathDoesNotResetAnAlreadyDegradedMilitaryTier() {
        kingdomService.createKingdom(KINGDOM_ID, "Avalon");
        kingdomService.joinKingdom(PLAYER, KINGDOM_ID);
        moraleService.openTrack(PLAYER);
        moraleService.recordSiegeHostileAction(PLAYER, true);

        OathResult result = oathService.swearAsMember(PLAYER);

        OathResult.Success success = (OathResult.Success) result;
        assertEquals(MoraleTier.SHAKEN, success.militaryTier());
    }

    @Test
    void alreadyAKingdomMemberCannotSwearTheOutsiderOath() {
        kingdomService.createKingdom(KINGDOM_ID, "Avalon");
        kingdomService.joinKingdom(PLAYER, KINGDOM_ID);

        OathResult result = oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);

        assertInstanceOf(OathResult.Failure.class, result);
        assertFalse(swornOutsiderStore.find(PLAYER).isPresent());
    }

    @Test
    void outsiderOathRequiresANonBlankPurpose() {
        assertThrows(IllegalArgumentException.class, () -> oathService.swearAsOutsider(KINGDOM_ID, PLAYER, ""));
        assertThrows(IllegalArgumentException.class, () -> oathService.swearAsOutsider(KINGDOM_ID, PLAYER, "   "));
    }

    @Test
    void featureFlagDisabledMakesBothOathCeremoniesANoOp() {
        OathService disabled = new OathService(
                kingdomService, loyaltyService, moraleService, swornOutsiderStore, OathConfig.off());

        OathResult outsiderResult = disabled.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);
        OathResult memberResult = disabled.swearAsMember(PLAYER);

        assertInstanceOf(OathResult.Disabled.class, outsiderResult);
        assertInstanceOf(OathResult.Disabled.class, memberResult);
        assertTrue(moraleService.tierOf(PLAYER).isEmpty());
        assertFalse(swornOutsiderStore.find(PLAYER).isPresent());
    }

    @Test
    void reSwearingAnAlreadySwornOutsiderDoesNotResetTheirDegradedMilitaryTier() {
        oathService.swearAsOutsider(KINGDOM_ID, PLAYER, PURPOSE);
        moraleService.recordSiegeHostileAction(PLAYER, true);

        OathResult result = oathService.swearAsOutsider(KINGDOM_ID, PLAYER, "a renewed purpose");

        OathResult.Success success = (OathResult.Success) result;
        assertEquals(MoraleTier.SHAKEN, success.militaryTier());
        assertEquals("a renewed purpose", swornOutsiderStore.find(PLAYER).orElseThrow().purpose());
    }
}
