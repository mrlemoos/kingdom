package dev.mrlemoos.kingdom.mint;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreasuryLordMessagesTest {

    @Test
    void formatsTerritoryBriefingWithWholeCoronaAmounts() {
        String message = TreasuryLordMessages.territoryBriefing(1250.0, 80.0);

        assertTrue(message.contains("[Lord of the Treasury]"));
        assertTrue(message.contains("Your Majesty"));
        assertTrue(message.contains("treasury holds"));
        assertTrue(message.contains("1250"));
        assertTrue(message.contains("Villager GDP is presently"));
        assertTrue(message.contains("80"));
        assertTrue(message.contains("Corona per day"));
    }

    @Test
    void formatsTerritoryBriefingWithFractionalAmounts() {
        String message = TreasuryLordMessages.territoryBriefing(99.5, 12.25);

        assertTrue(message.contains("99.50"));
        assertTrue(message.contains("12.25"));
    }

    @Test
    void prefixesSpeakerInGold() {
        String message = TreasuryLordMessages.territoryBriefing(0.0, 0.0);

        assertTrue(message.startsWith(c("&6[Lord of the Treasury]")));
    }
}
