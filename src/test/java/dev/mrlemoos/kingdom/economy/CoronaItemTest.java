package dev.mrlemoos.kingdom.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class CoronaItemTest {

    @Test
    void displayNameForAmountUsesSingularForOne() {
        assertEquals(ChatColor.GOLD + "Corona", CoronaItem.displayNameForAmount(1));
    }

    @Test
    void displayNameForAmountUsesPluralForMultiple() {
        assertEquals(ChatColor.GOLD + "Coronas", CoronaItem.displayNameForAmount(2));
        assertEquals(ChatColor.GOLD + "Coronas", CoronaItem.displayNameForAmount(64));
    }

    @Test
    void isCoronaDisplayNameRecognisesNamedVariants() {
        assertTrue(CoronaItem.isCoronaDisplayName(CoronaItem.DISPLAY_NAME_SINGULAR));
        assertTrue(CoronaItem.isCoronaDisplayName(CoronaItem.DISPLAY_NAME_PLURAL));
    }

    @Test
    void isCoronaDisplayNameRejectsOtherNames() {
        assertFalse(CoronaItem.isCoronaDisplayName("Corona"));
        assertFalse(CoronaItem.isCoronaDisplayName(null));
        assertFalse(CoronaItem.isCoronaDisplayName(ChatColor.YELLOW + "Gold"));
    }
}
