package dev.mrlemoos.kingdom.police.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrialJuryBallotLayoutTest {

    @Test
    void mapsGuiltyAndNotGuiltySlots() {
        assertEquals(TrialJuryBallotAction.GUILTY, TrialJuryBallotLayout.actionForSlot(11));
        assertEquals(TrialJuryBallotAction.NOT_GUILTY, TrialJuryBallotLayout.actionForSlot(15));
        assertNull(TrialJuryBallotLayout.actionForSlot(13));
        assertNull(TrialJuryBallotLayout.actionForSlot(0));
    }

    @Test
    void infoLoreIsSecretAndShowsAccusedAndTime() {
        List<String> lore = TrialJuryBallotLayout.infoLore("Alice", 125_000L);
        assertEquals("Accused: Alice", lore.get(0));
        assertTrue(lore.get(1).contains("2m"));
        assertTrue(lore.stream().anyMatch(line -> line.toLowerCase().contains("secret")));
        assertTrue(lore.stream().noneMatch(line -> line.toLowerCase().contains("guilty vote")));
    }
}
