package dev.mrlemoos.kingdom.hearth;

import java.util.Objects;

/**
 * A hearth is any lit campfire with a container set against it, face to face and never on the
 * diagonal. Nothing is placed, crafted or recorded: the world holds the fact and the sweep reads it.
 */
public final class HearthDetection {

    private static final int[][] FACES = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private HearthDetection() {}

    public static boolean isHearth(HearthBlockView world, int x, int y, int z) {
        Objects.requireNonNull(world, "world");
        if (!world.isLitCampfire(x, y, z)) {
            return false;
        }
        for (int[] face : FACES) {
            if (world.isContainer(x + face[0], y + face[1], z + face[2])) {
                return true;
            }
        }
        return false;
    }
}
