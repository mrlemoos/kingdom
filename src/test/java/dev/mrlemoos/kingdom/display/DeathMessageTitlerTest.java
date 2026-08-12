package dev.mrlemoos.kingdom.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class DeathMessageTitlerTest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static String plain(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component);
    }

    @Test
    void prefixesTheVictimNameInsideTheDeathMessage() {
        Component message = Component.text("Bob was slain by a zombie");

        Component titled = DeathMessageTitler.titled(message, "Bob", LEGACY.deserialize("§9§lLORD "));

        assertEquals("LORD Bob was slain by a zombie", plain(titled));
    }

    @Test
    void leavesTheMessageUntouchedWhenThereIsNoPrefix() {
        Component message = Component.text("Bob was slain by a zombie");

        Component titled = DeathMessageTitler.titled(message, "Bob", Component.empty());

        assertEquals(message, titled);
    }

    @Test
    void prefixesOnlyTheFirstOccurrenceSoAKillerNameIsUntouched() {
        Component message = Component.text("Bob was slain by Bob");

        Component titled = DeathMessageTitler.titled(message, "Bob", LEGACY.deserialize("§9§lLORD "));

        assertEquals("LORD Bob was slain by Bob", plain(titled));
    }

    @Test
    void leavesTheMessageUntouchedWhenTheNameIsAbsent() {
        Component message = Component.text("A mysterious death");

        Component titled = DeathMessageTitler.titled(message, "Bob", LEGACY.deserialize("§9§lLORD "));

        assertEquals("A mysterious death", plain(titled));
    }
}
