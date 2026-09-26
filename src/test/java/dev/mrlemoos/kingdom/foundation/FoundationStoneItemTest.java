package dev.mrlemoos.kingdom.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.parliament.KingdomFlagItems;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class FoundationStoneItemTest {

    private FoundationStoneItem stones;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        stones = new FoundationStoneItem(MockBukkit.createMockPlugin());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aStoneRemembersItsKindAndItsRealm() {
        ItemStack stone = stones.create(FoundationStone.CHURCH, "northmarch");

        assertTrue(stone.getType().isBlock());
        assertEquals(Optional.of(FoundationStone.CHURCH), stones.kind(stone));
        assertEquals(Optional.of("northmarch"), stones.kingdomId(stone));
    }

    @Test
    void anOrdinaryBlockIsNoStone() {
        ItemStack bricks = new ItemStack(Material.CHISELED_STONE_BRICKS);

        assertEquals(Optional.empty(), stones.kind(bricks));
        assertEquals(Optional.empty(), stones.kingdomId(bricks));
        assertEquals(Optional.empty(), stones.kind(null));
    }

    @Test
    void theRegistrarStoneIsAChiseledBookshelf() {
        ItemStack stone = stones.create(FoundationStone.REGISTRAR, "northmarch");

        assertEquals(Material.CHISELED_BOOKSHELF, stone.getType());
        assertEquals(Optional.of(FoundationStone.REGISTRAR), stones.kind(stone));
    }

    @Test
    void theCourtStoneIsALecternAndTheGranaryStoneAHayBale() {
        assertEquals(Material.LECTERN, stones.create(FoundationStone.COURT, "northmarch").getType());
        assertEquals(Material.HAY_BLOCK, stones.create(FoundationStone.GRANARY, "northmarch").getType());
        assertEquals(Optional.of(FoundationStone.GRANARY), stones.kind(stones.create(FoundationStone.GRANARY, "northmarch")));
    }

    @Test
    void theLordsStoneIsABannerInTheCrownsDesign() {
        KingdomFlag design = new KingdomFlag(
                "BLUE_BANNER", List.of(new KingdomFlag.Layer("minecraft:border", "WHITE")));

        ItemStack stone = stones.createLords("northmarch", design);

        assertEquals(Material.BLUE_BANNER, stone.getType());
        assertEquals(Optional.of(FoundationStone.LORDS), stones.kind(stone));
        assertEquals(Optional.of("northmarch"), stones.kingdomId(stone));
        assertEquals(Optional.of(design), KingdomFlagItems.fromItem(stone));
    }

    @Test
    void theOtherParliamentStonesAreCutFromStone() {
        for (FoundationStone kind : List.of(
                FoundationStone.COMMONS, FoundationStone.SPEAKER_CHAIR, FoundationStone.BAR, FoundationStone.MP_SEAT)) {
            assertEquals(Material.CHISELED_STONE_BRICKS, stones.create(kind, "northmarch").getType(), kind.name());
        }
    }
}
