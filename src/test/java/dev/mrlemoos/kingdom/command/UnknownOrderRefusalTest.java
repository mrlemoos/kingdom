package dev.mrlemoos.kingdom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** One wording, two doors: {@code KingdomCommand.execute} and Cloud's syntax handler. */
class UnknownOrderRefusalTest {

    @Test
    void anOrderIsRefusedByName() {
        assertEquals("The realm knows no such order: parliment", UnknownOrderRefusal.refusal("parliment"));
    }

    @Test
    void anUnnamedOrderIsStillRefused() {
        assertEquals("The realm knows no such order.", UnknownOrderRefusal.refusal(""));
        assertEquals("The realm knows no such order.", UnknownOrderRefusal.refusal("   "));
        assertEquals("The realm knows no such order.", UnknownOrderRefusal.refusal(null));
    }

    @Test
    void theOrderIsTheWordAfterTheRoot() {
        assertEquals("parliment", UnknownOrderRefusal.orderIn("kingdom parliment"));
        assertEquals("parliment", UnknownOrderRefusal.orderIn("/kdm parliment set commons"));
        assertEquals("parliment", UnknownOrderRefusal.orderIn("  kingdom   parliment  "));
    }

    @Test
    void aRootWithNothingAfterItNamesItself() {
        assertEquals("kingdom", UnknownOrderRefusal.orderIn("kingdom"));
        assertEquals("", UnknownOrderRefusal.orderIn(""));
        assertEquals("", UnknownOrderRefusal.orderIn(null));
    }

    @Test
    void theCloudDoorRefusesByNameThenNamesTheSyntaxThenPointsAtTheHub() {
        List<String> lines = UnknownOrderRefusal.lines("kingdom parliment", "kingdom parliament status");

        assertEquals(
                List.of(
                        "The realm knows no such order: parliment",
                        "The order runs: /kingdom parliament status",
                        UnknownOrderRefusal.HUB_POINTER),
                lines);
    }

    @Test
    void theSyntaxLineIsLeftOutWhenTheRealmKnowsNone() {
        List<String> lines = UnknownOrderRefusal.lines("kingdom parliment", "");

        assertEquals(List.of("The realm knows no such order: parliment", UnknownOrderRefusal.HUB_POINTER), lines);
    }

    @Test
    void thePointerSendsASubjectToTheHub() {
        assertTrue(UnknownOrderRefusal.HUB_POINTER.contains("/kingdom"));
        assertTrue(UnknownOrderRefusal.HUB_POINTER.contains("Realm Hub"));
    }
}
