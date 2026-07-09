package dev.mrlemoos.kingdom.war.conscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pressed villager conscription: territory villagers pressed into wartime service are removed
 * from the villager economy while pressed, capped per kingdom, and returned on demobilisation.
 * Seated villager MPs are never eligible to be pressed.
 */
class ConscriptionServiceTest {

    private static final String KINGDOM_ID = "avalon";
    private static final UUID VILLAGER_ONE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VILLAGER_TWO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VILLAGER_THREE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SEATED_MP_VILLAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private KingdomService kingdomService;
    private InMemoryConscriptionStore store;
    private ConscriptionService conscriptionService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM_ID, "Avalon");
        store = new InMemoryConscriptionStore();
        conscriptionService = new ConscriptionService(kingdomService, store, new ConscriptionConfig(true, 2));
    }

    @Test
    void pressingAVillagerTracksItAsPressed() {
        WarResult result = conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        assertInstanceOf(WarResult.Success.class, result);
        assertTrue(conscriptionService.isPressed(VILLAGER_ONE));
    }

    @Test
    void anUnpressedVillagerIsNotReportedAsPressed() {
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
    }

    @Test
    void theCapIsEnforcedPerKingdom() {
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        conscriptionService.press(KINGDOM_ID, VILLAGER_TWO);

        WarResult result = conscriptionService.press(KINGDOM_ID, VILLAGER_THREE);

        assertInstanceOf(WarResult.Failure.class, result);
        assertFalse(conscriptionService.isPressed(VILLAGER_THREE));
        assertEquals(2, conscriptionService.pressedCount(KINGDOM_ID));
    }

    @Test
    void theCapIsPerKingdomNotGlobal() {
        kingdomService.createKingdom("northmarch", "Northmarch");
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        conscriptionService.press(KINGDOM_ID, VILLAGER_TWO);

        WarResult result = conscriptionService.press("northmarch", VILLAGER_THREE);

        assertInstanceOf(WarResult.Success.class, result);
        assertEquals(1, conscriptionService.pressedCount("northmarch"));
    }

    @Test
    void capRemainingReflectsHowManyMoreCanBePressed() {
        assertEquals(2, conscriptionService.capRemaining(KINGDOM_ID));

        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        assertEquals(1, conscriptionService.capRemaining(KINGDOM_ID));
    }

    @Test
    void aSeatedVillagerMpIsIneligibleToBePressed() {
        Kingdom kingdom = kingdomService.getKingdom(KINGDOM_ID).orElseThrow();
        kingdom.getElectionState().seat(1).orElseThrow().assignVillager("farmer", SEATED_MP_VILLAGER);

        WarResult result = conscriptionService.press(KINGDOM_ID, SEATED_MP_VILLAGER);

        assertInstanceOf(WarResult.Failure.class, result);
        assertFalse(conscriptionService.isPressed(SEATED_MP_VILLAGER));
        assertTrue(conscriptionService.isSeatedVillagerMp(KINGDOM_ID, SEATED_MP_VILLAGER));
    }

    @Test
    void aVillagerNotSeatedAsAnMpIsEligible() {
        Kingdom kingdom = kingdomService.getKingdom(KINGDOM_ID).orElseThrow();
        kingdom.getElectionState().seat(1).orElseThrow().assignVillager("farmer", SEATED_MP_VILLAGER);

        assertFalse(conscriptionService.isSeatedVillagerMp(KINGDOM_ID, VILLAGER_ONE));
    }

    @Test
    void pressingAnAlreadyPressedVillagerFails() {
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        WarResult result = conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        assertInstanceOf(WarResult.Failure.class, result);
        assertEquals(1, conscriptionService.pressedCount(KINGDOM_ID));
    }

    @Test
    void pressingForAnUnknownKingdomFails() {
        WarResult result = conscriptionService.press("nonexistent", VILLAGER_ONE);

        assertInstanceOf(WarResult.Failure.class, result);
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
    }

    @Test
    void aPressedVillagerIsExcludedFromGdpAndAnUnpressedVillagerIsNot() {
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        assertFalse(conscriptionService.isEconomicallyActive(VILLAGER_ONE));
        assertTrue(conscriptionService.shouldExcludeFromGdp(VILLAGER_ONE));
        assertTrue(conscriptionService.isEconomicallyActive(VILLAGER_TWO));
        assertFalse(conscriptionService.shouldExcludeFromGdp(VILLAGER_TWO));
    }

    @Test
    void releasingAPressedVillagerStopsExcludingItFromGdp() {
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);

        WarResult result = conscriptionService.release(VILLAGER_ONE);

        assertInstanceOf(WarResult.Success.class, result);
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
        assertTrue(conscriptionService.isEconomicallyActive(VILLAGER_ONE));
    }

    @Test
    void releasingAVillagerThatWasNeverPressedFailsGracefully() {
        WarResult result = conscriptionService.release(VILLAGER_ONE);

        assertInstanceOf(WarResult.Failure.class, result);
    }

    @Test
    void releaseAllDemobilisesEveryPressedVillagerInAKingdomAndFreesTheCap() {
        kingdomService.createKingdom("northmarch", "Northmarch");
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        conscriptionService.press(KINGDOM_ID, VILLAGER_TWO);
        conscriptionService.press("northmarch", VILLAGER_THREE);

        int released = conscriptionService.releaseAll(KINGDOM_ID);

        assertEquals(2, released);
        assertFalse(conscriptionService.isPressed(VILLAGER_ONE));
        assertFalse(conscriptionService.isPressed(VILLAGER_TWO));
        assertEquals(0, conscriptionService.pressedCount(KINGDOM_ID));
        // Other kingdoms are untouched by a targeted release-all.
        assertTrue(conscriptionService.isPressed(VILLAGER_THREE));
    }

    @Test
    void featureFlagDisabledMakesPressingANoOp() {
        ConscriptionService disabled =
                new ConscriptionService(kingdomService, store, new ConscriptionConfig(false, 2));

        WarResult result = disabled.press(KINGDOM_ID, VILLAGER_ONE);

        assertInstanceOf(WarResult.Failure.class, result);
        assertFalse(disabled.isPressed(VILLAGER_ONE));
    }

    @Test
    void pressedVillagersViewReflectsTheKingdomsCurrentLevy() {
        conscriptionService.press(KINGDOM_ID, VILLAGER_ONE);
        conscriptionService.press(KINGDOM_ID, VILLAGER_TWO);

        assertEquals(2, conscriptionService.pressedView(KINGDOM_ID).size());
        assertTrue(conscriptionService.pressedView(KINGDOM_ID).contains(VILLAGER_ONE));
        assertTrue(conscriptionService.pressedView(KINGDOM_ID).contains(VILLAGER_TWO));
    }
}
