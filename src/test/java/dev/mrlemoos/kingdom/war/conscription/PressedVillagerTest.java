package dev.mrlemoos.kingdom.war.conscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A villager pressed into wartime service from a kingdom's territory population. */
class PressedVillagerTest {

    private static final UUID VILLAGER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void recordsKingdomIdVillagerIdAndPressedAtMs() {
        PressedVillager pressed = new PressedVillager("avalon", VILLAGER, 1_000L);

        assertEquals("avalon", pressed.kingdomId());
        assertEquals(VILLAGER, pressed.villagerId());
        assertEquals(1_000L, pressed.pressedAtMs());
    }

    @Test
    void rejectsANullKingdomIdOrVillagerId() {
        assertThrows(NullPointerException.class, () -> new PressedVillager(null, VILLAGER, 1_000L));
        assertThrows(NullPointerException.class, () -> new PressedVillager("avalon", null, 1_000L));
    }
}
