package dev.mrlemoos.kingdom.granary;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Which of the starving the day takes. Drawn from a hat of those who have gone hungry long enough
 * and are not spared; an empty hat takes nobody. A port so the draw can be pinned in a test.
 */
@FunctionalInterface
public interface StarvationLot {

    Optional<UUID> draw(List<UUID> candidates);

    /** The ordinary lot: one name out of the hat, at random. */
    static StarvationLot random(Random random) {
        Random source = random != null ? random : new Random();
        return candidates -> candidates == null || candidates.isEmpty()
                ? Optional.empty()
                : Optional.of(candidates.get(source.nextInt(candidates.size())));
    }
}
