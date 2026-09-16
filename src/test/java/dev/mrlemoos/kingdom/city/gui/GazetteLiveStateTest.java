package dev.mrlemoos.kingdom.city.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GazetteLiveStateTest {

    @Test
    void linesSummariseLiveFacts() {
        GazetteLiveState state = new GazetteLiveState("Budget Bill", "realm day 40", 2, 5, 1_000d);
        assertEquals(5, state.lines().size());
        assertTrue(state.lines().get(0).contains("Budget Bill"));
        assertTrue(state.lines().get(2).contains("2"));
        assertTrue(state.lines().get(4).contains("1000"));
    }

    @Test
    void blankOpenBillReadsAsNone() {
        GazetteLiveState state = new GazetteLiveState("", "none proclaimed", 0, 0, 0d);
        assertEquals("Open bill: none", state.lines().get(0));
    }

    @Test
    void treatyLineAppearsWhenTreatyBusinessIsLive() {
        GazetteLiveState state = new GazetteLiveState(
                "", "none proclaimed", 0, 0, 0d, "Treaty awaiting Crown: trade pact with Northmarch");

        assertTrue(state.lines().contains("Treaty awaiting Crown: trade pact with Northmarch"));
    }
}
