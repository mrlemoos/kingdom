package dev.mrlemoos.kingdom.economy.villager.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.CoronaItem;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class CoronaMerchantPaymentTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private EconomyService economyService;
    private ItemStack[] contents;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economyService = new EconomyService();
        contents = new ItemStack[36];
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void collectsFromNuggetsOnlyWhenInventoryHasEnough() {
        contents[0] = CoronaItem.create(5);

        var payment = CoronaMerchantPayment.collectContents(contents, economyService, PLAYER, 3);

        assertTrue(payment.isPresent());
        assertEquals(3, payment.get().fromNuggets());
        assertEquals(0, payment.get().fromWallet());
        assertEquals(2, CoronaItem.countInContents(contents));
    }

    @Test
    void fallsBackToWalletWhenNuggetsAreShort() {
        contents[0] = CoronaItem.create(1);
        economyService.depositFromNuggets(PLAYER, 4);

        var payment = CoronaMerchantPayment.collectContents(contents, economyService, PLAYER, 3);

        assertTrue(payment.isPresent());
        assertEquals(1, payment.get().fromNuggets());
        assertEquals(2, payment.get().fromWallet());
        assertEquals(2.0, economyService.getWalletBalance(PLAYER), 1e-9);
    }

    @Test
    void rejectsWhenNuggetsAndWalletCannotCoverPrice() {
        contents[0] = CoronaItem.create(1);
        economyService.depositFromNuggets(PLAYER, 1);

        assertFalse(CoronaMerchantPayment.collectContents(contents, economyService, PLAYER, 3).isPresent());
    }

    @Test
    void spaceForResultsCountsEmptySlotsAndMatchingStacks() {
        ItemStack[] slots = new ItemStack[3];
        slots[0] = new ItemStack(Material.DIAMOND, 60);
        slots[1] = new ItemStack(Material.STONE, 64);

        // 4 room in the diamond stack + 64 in the empty slot, two diamonds a trade.
        assertEquals(34, CoronaMerchantPayment.spaceForResults(slots, new ItemStack(Material.DIAMOND, 2)));
    }

    @Test
    void spaceForResultsIsZeroWhenTheInventoryIsFull() {
        ItemStack[] slots = new ItemStack[2];
        slots[0] = new ItemStack(Material.STONE, 64);
        slots[1] = new ItemStack(Material.DIAMOND, 64);

        assertEquals(0, CoronaMerchantPayment.spaceForResults(slots, new ItemStack(Material.DIAMOND, 1)));
    }

    @Test
    void rejectsNonWholeCoronaPrices() {
        contents[0] = CoronaItem.create(5);

        assertFalse(CoronaMerchantPayment.collectContents(contents, economyService, PLAYER, 0).isPresent());
    }
}
