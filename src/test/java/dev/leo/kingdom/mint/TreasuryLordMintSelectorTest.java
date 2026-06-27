package dev.leo.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.model.MintLocation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreasuryLordMintSelectorTest {

    @Test
    void selectsNearestMintInSameWorld() {
        List<MintLocation> mints = List.of(
                new MintLocation("world", 0, 64, 0),
                new MintLocation("world", 100, 64, 100),
                new MintLocation("other", 1, 64, 1));

        Optional<MintLocation> nearest = TreasuryLordMintSelector.nearestInWorld(mints, "world", 5.0, 64.0, 5.0);

        assertTrue(nearest.isPresent());
        assertEquals(0, nearest.get().x());
        assertEquals(0, nearest.get().z());
    }

    @Test
    void ignoresMintsInOtherWorlds() {
        List<MintLocation> mints = List.of(new MintLocation("nether", 0, 64, 0));

        Optional<MintLocation> nearest = TreasuryLordMintSelector.nearestInWorld(mints, "world", 0.0, 64.0, 0.0);

        assertTrue(nearest.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoMints() {
        Optional<MintLocation> nearest =
                TreasuryLordMintSelector.nearestInWorld(List.of(), "world", 0.0, 64.0, 0.0);

        assertTrue(nearest.isEmpty());
    }
}
