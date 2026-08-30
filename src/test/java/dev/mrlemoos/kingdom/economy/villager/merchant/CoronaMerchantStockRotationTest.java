package dev.mrlemoos.kingdom.economy.villager.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class CoronaMerchantStockRotationTest {

    private static final long DAY = TimeUnit.HOURS.toMillis(24);
    private static final UUID VILLAGER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private final CoronaMerchantStockRotation rotation = new CoronaMerchantStockRotation(3, DAY);

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void stocksAtMostMaxOffers() {
        assertEquals(3, rotation.stock(VILLAGER, offers(), 0L).size());
    }

    @Test
    void keepsTheSameShelfWithinAWindow() {
        assertEquals(rotation.stock(VILLAGER, offers(), 0L), rotation.stock(VILLAGER, offers(), DAY - 1));
    }

    @Test
    void rotatesTheShelfAcrossWindows() {
        assertNotEquals(rotation.stock(VILLAGER, offers(), 0L), rotation.stock(VILLAGER, offers(), DAY * 3));
    }

    @Test
    void differentVillagersStockDifferentShelves() {
        assertNotEquals(
                rotation.stock(VILLAGER, offers(), 0L),
                rotation.stock(UUID.fromString("00000000-0000-0000-0000-0000000000bb"), offers(), 0L));
    }

    @Test
    void shortListsAndDisabledRotationStockEverything() {
        List<CoronaMerchantOffer> offers = offers();
        assertEquals(offers, new CoronaMerchantStockRotation(0, DAY).stock(VILLAGER, offers, 0L));
        assertEquals(offers.subList(0, 2), rotation.stock(VILLAGER, offers.subList(0, 2), 0L));
        assertTrue(rotation.stock(VILLAGER, List.of(), 0L).isEmpty());
    }

    private static List<CoronaMerchantOffer> offers() {
        return List.of(
                new CoronaMerchantOffer(Material.BOOK, 5, 12),
                new CoronaMerchantOffer(Material.BOOKSHELF, 12, 8),
                new CoronaMerchantOffer(Material.NAME_TAG, 24, 4),
                new CoronaMerchantOffer(Material.PAPER, 2, 12),
                new CoronaMerchantOffer(Material.LANTERN, 9, 6),
                new CoronaMerchantOffer(Material.INK_SAC, 3, 12));
    }
}
