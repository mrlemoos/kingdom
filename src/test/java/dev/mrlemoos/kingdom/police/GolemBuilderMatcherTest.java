package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GolemBuilderMatcherTest {

    private static final UUID KNIGHT = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void aGolemRisingBelowTheHeadJustPlacedIsCreditedToItsPlacer() {
        GolemBuilderMatcher matcher = new GolemBuilderMatcher();
        matcher.headPlaced(KNIGHT, "world", 10, 66, 20, 1_000L);

        assertEquals(Optional.of(KNIGHT), matcher.builderOf("world", 10, 64, 20, 1_050L));
    }

    @Test
    void aMatchIsSpentOnce() {
        GolemBuilderMatcher matcher = new GolemBuilderMatcher();
        matcher.headPlaced(KNIGHT, "world", 10, 66, 20, 1_000L);
        matcher.builderOf("world", 10, 64, 20, 1_050L);

        assertTrue(matcher.builderOf("world", 10, 64, 20, 1_060L).isEmpty());
    }

    @Test
    void aStaleHeadIsNotCredited() {
        GolemBuilderMatcher matcher = new GolemBuilderMatcher();
        matcher.headPlaced(KNIGHT, "world", 10, 66, 20, 1_000L);

        assertTrue(matcher.builderOf("world", 10, 64, 20, 1_000L + GolemBuilderMatcher.WINDOW_MS + 1).isEmpty());
    }

    @Test
    void aHeadFarAwayOrInAnotherWorldIsNotCredited() {
        GolemBuilderMatcher matcher = new GolemBuilderMatcher();
        matcher.headPlaced(KNIGHT, "world", 10, 66, 20, 1_000L);
        matcher.headPlaced(OTHER, "world_nether", 10, 66, 20, 1_000L);

        assertTrue(matcher.builderOf("world", 30, 64, 20, 1_010L).isEmpty());
        assertTrue(matcher.builderOf("world_the_end", 10, 64, 20, 1_010L).isEmpty());
    }

    @Test
    void theNearestHeadWinsWhenTwoArePlacedTogether() {
        GolemBuilderMatcher matcher = new GolemBuilderMatcher();
        matcher.headPlaced(OTHER, "world", 12, 66, 20, 1_000L);
        matcher.headPlaced(KNIGHT, "world", 10, 66, 20, 1_000L);

        assertEquals(Optional.of(KNIGHT), matcher.builderOf("world", 10, 64, 20, 1_010L));
    }
}
