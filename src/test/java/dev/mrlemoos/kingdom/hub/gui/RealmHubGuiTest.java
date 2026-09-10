package dev.mrlemoos.kingdom.hub.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class RealmHubGuiTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void aRefusedEntryIsGreyedAndCarriesItsRefusal() {
        RealmHubEntry refused = RealmHubEntry.refused(
                RealmHubTopic.POWER_GAZETTE,
                "Publish to the Gazette",
                "A Duke or the Crown may publish.",
                List.of("Hold a signed book and right-click the Town Crier."));

        ItemStack item = RealmHubGui.item(refused);

        assertEquals(Material.GRAY_DYE, item.getType());
        List<String> lore = new ArrayList<>();
        for (String line : item.getItemMeta().getLore()) {
            lore.add(strip(line));
        }
        assertTrue(lore.contains("A Duke or the Crown may publish."), lore.toString());
    }

    @Test
    void aUsablEntryKeepsItsOwnItemAndInvitesAClick() {
        RealmHubEntry usable = RealmHubEntry.usable(
                RealmHubTopic.LOYALTY_LEDGER,
                "Loyalty Ledger",
                List.of("Your standing."),
                RealmHubAction.OPEN_LOYALTY_LEDGER);

        ItemStack item = RealmHubGui.item(usable);

        assertEquals(RealmHubGui.material(RealmHubTopic.LOYALTY_LEDGER), item.getType());
        assertTrue(item.getItemMeta().getLore().stream()
                .map(line -> strip(line))
                .anyMatch(line -> line.equals("Click to open")));
    }

    @Test
    void aFullScreenSpillsOntoASecondPageWithNavigation() {
        List<RealmHubEntry> entries = new ArrayList<>();
        for (int i = 0; i < RealmHubLayout.PAGE_SIZE + 3; i++) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.STANDING, "Entry " + i, List.of("A line"), RealmHubAction.NONE));
        }

        RealmHubGui first = RealmHubGui.create(entries, 0);
        Inventory firstInventory = first.getInventory();
        assertNotNull(firstInventory.getItem(RealmHubLayout.SLOT_NEXT));
        assertEquals("Entry 0", strip(firstInventory.getItem(0).getItemMeta().getDisplayName()));

        RealmHubGui second = RealmHubGui.create(entries, 1);
        assertEquals(1, second.page());
        assertNotNull(second.getInventory().getItem(RealmHubLayout.SLOT_PREVIOUS));
        assertEquals(
                "Entry " + RealmHubLayout.PAGE_SIZE,
                strip(second.getInventory().getItem(0).getItemMeta().getDisplayName()));
        assertEquals(RealmHubTopic.STANDING, second.entryForSlot(0).topic());
    }
}
