package dev.mrlemoos.kingdom.hub.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.strip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.hub.RealmHubAction;
import dev.mrlemoos.kingdom.hub.RealmHubEntry;
import dev.mrlemoos.kingdom.hub.RealmHubLayout;
import dev.mrlemoos.kingdom.hub.RealmHubSection;
import dev.mrlemoos.kingdom.hub.RealmHubTopic;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    void aSectionThatFitsHasBackAndNoPageArrows() {
        List<RealmHubEntry> entries = List.of(RealmHubEntry.usable(
                RealmHubTopic.GAZETTE, "The Gazette", List.of("A line"), RealmHubAction.OPEN_GAZETTE));

        RealmHubGui gui = RealmHubGui.section(RealmHubSection.CITY, entries, 0);
        Inventory inventory = gui.getInventory();

        assertEquals(Optional.of(RealmHubSection.CITY), gui.section());
        assertEquals("Back", strip(inventory.getItem(RealmHubLayout.SLOT_BACK).getItemMeta().getDisplayName()));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(RealmHubLayout.SLOT_NEXT).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(RealmHubLayout.SLOT_PREVIOUS).getType());
        assertEquals(RealmHubTopic.GAZETTE, gui.entryForSlot(0).topic());
    }

    @Test
    void anOverfullSectionSpillsOntoASecondPageWithArrows() {
        List<RealmHubEntry> entries = new ArrayList<>();
        for (int i = 0; i < RealmHubLayout.PAGE_SIZE + 3; i++) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.POWER_SITES, "Entry " + i, List.of("A line"), RealmHubAction.NONE));
        }

        RealmHubGui first = RealmHubGui.section(RealmHubSection.CITY, entries, 0);
        assertTrue(first.hasNext());
        assertEquals("Next page", strip(first.getInventory().getItem(RealmHubLayout.SLOT_NEXT).getItemMeta().getDisplayName()));
        assertEquals("Entry 0", strip(first.getInventory().getItem(0).getItemMeta().getDisplayName()));

        RealmHubGui second = RealmHubGui.section(RealmHubSection.CITY, entries, 1);
        assertEquals(1, second.page());
        assertNotNull(second.getInventory().getItem(RealmHubLayout.SLOT_PREVIOUS));
        assertEquals(
                "Entry " + RealmHubLayout.PAGE_SIZE,
                strip(second.getInventory().getItem(0).getItemMeta().getDisplayName()));
    }

    @Test
    void theFrontPageShowsStandingLiveBusinessAndADoorToEverySection() {
        List<RealmHubEntry> front = List.of(
                RealmHubEntry.usable(RealmHubTopic.STANDING, "Your Standing", List.of("Realm: Northmarch"), null),
                RealmHubEntry.usable(RealmHubTopic.LIVE_ELECTION, "An Election Is Under Way", List.of(), null));
        Map<RealmHubSection, List<RealmHubEntry>> sections = new EnumMap<>(RealmHubSection.class);

        RealmHubGui gui = RealmHubGui.frontPage(front, sections);

        assertTrue(gui.section().isEmpty());
        assertEquals(RealmHubTopic.STANDING, gui.entryForSlot(RealmHubLayout.SLOT_STANDING).topic());
        assertEquals(RealmHubTopic.LIVE_ELECTION, gui.entryForSlot(RealmHubLayout.liveSlot(0)).topic());
        for (RealmHubSection section : RealmHubSection.values()) {
            assertEquals(section, gui.doorForSlot(RealmHubLayout.doorSlot(section)));
        }
    }

    @Test
    void anEntryReadsStateAndHowThenWho() {
        RealmHubEntry usable = RealmHubEntry.usable(
                        RealmHubTopic.POWER_GAZETTE, "Publish", List.of("State", "How"), RealmHubAction.NONE)
                .withWho("A Duke or the Crown may publish.");

        List<String> lore = new ArrayList<>();
        for (String line : RealmHubGui.item(usable).getItemMeta().getLore()) {
            lore.add(strip(line));
        }
        assertEquals(List.of("State", "How", "A Duke or the Crown may publish."), lore);
    }
}
