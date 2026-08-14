package dev.mrlemoos.kingdom.hearth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A hearth is a lit campfire with a container set against it, face to face and never on the
 * diagonal. Nothing about it is the plugin's own: the world holds the fact and the sweep reads it.
 */
class HearthDetectionTest {

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static final class FakeWorld implements HearthBlockView {

        private final Set<String> litCampfires = new HashSet<>();
        private final Set<String> unlitCampfires = new HashSet<>();
        private final Set<String> containers = new HashSet<>();

        @Override
        public boolean isLitCampfire(int x, int y, int z) {
            return litCampfires.contains(key(x, y, z));
        }

        @Override
        public boolean isContainer(int x, int y, int z) {
            return containers.contains(key(x, y, z));
        }
    }

    @Test
    void litCampfireWithFaceAdjacentContainerIsAHearth() {
        FakeWorld world = new FakeWorld();
        world.litCampfires.add(key(10, 64, 10));
        world.containers.add(key(11, 64, 10));

        assertTrue(HearthDetection.isHearth(world, 10, 64, 10));
    }

    @Test
    void containerAboveOrBelowAlsoServes() {
        FakeWorld world = new FakeWorld();
        world.litCampfires.add(key(0, 64, 0));
        world.containers.add(key(0, 63, 0));

        assertTrue(HearthDetection.isHearth(world, 0, 64, 0));
    }

    @Test
    void unlitCampfireIsNoHearth() {
        FakeWorld world = new FakeWorld();
        world.unlitCampfires.add(key(10, 64, 10));
        world.containers.add(key(11, 64, 10));

        assertFalse(HearthDetection.isHearth(world, 10, 64, 10));
    }

    @Test
    void litCampfireWithoutAContainerIsNoHearth() {
        FakeWorld world = new FakeWorld();
        world.litCampfires.add(key(10, 64, 10));

        assertFalse(HearthDetection.isHearth(world, 10, 64, 10));
    }

    @Test
    void diagonalContainerDoesNotCount() {
        FakeWorld world = new FakeWorld();
        world.litCampfires.add(key(10, 64, 10));
        world.containers.add(key(11, 64, 11));

        assertFalse(HearthDetection.isHearth(world, 10, 64, 10));
    }
}
